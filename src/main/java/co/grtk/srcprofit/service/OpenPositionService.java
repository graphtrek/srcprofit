package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.PositionDto;
import co.grtk.srcprofit.entity.AssetClass;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OpenPositionEntity;
import co.grtk.srcprofit.entity.OptionStatus;
import co.grtk.srcprofit.entity.OptionType;
import co.grtk.srcprofit.mapper.PositionMapper;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OpenPositionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static co.grtk.srcprofit.mapper.PositionMapper.calculateAndSetAnnualizedRoi;
import static org.apache.commons.csv.CSVParser.parse;

/**
 * Service for parsing and persisting IBKR Flex Report Open Positions CSV data.
 *
 * Implements the CSV parsing and upsert workflow for open positions snapshots:
 * 1. Parse CSV using Apache Commons CSV
 * 2. Upsert corresponding InstrumentEntity (ground truth sync)
 * 3. For each row, check if position exists by conid
 * 4. If exists: update all fields (upsert behavior)
 * 5. If new: create and insert new position
 * 6. Return count of successful records
 *
 * Instrument Synchronization:
 * - Before position upsert, ensures corresponding InstrumentEntity exists
 * - Maps: conid→conid, symbol→ticker, description→name
 * - IBKR Flex Report is ground truth for instrument identification
 * - Creates instrument if missing, updates name/ticker if exists
 * - Fallback: uses symbol as name if description is empty
 * - Preserves existing instrument data (price, Alpaca metadata, etc.)
 *
 * Error Handling:
 * - Simple pattern (like NetAssetValueService): Exceptions propagate, transaction rolls back
 * - No detailed error tracking (unlike OptionService which uses CsvImportResult)
 * - Rationale: Open positions are snapshots, not transactional data
 *
 * Transaction Management:
 * - @Transactional ensures all-or-nothing semantics
 * - Both instrument and position are saved/rolled back atomically
 * - Any parsing error causes entire import to fail and rollback
 * - No partial imports or partial updates
 *
 * @see OpenPositionEntity for entity structure
 * @see OpenPositionRepository for persistence methods
 * @see InstrumentRepository for instrument persistence
 * @see FlexReportsService for orchestration
 */
@Service
public class OpenPositionService {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionService.class);

    private final OpenPositionRepository openPositionRepository;
    private final InstrumentRepository instrumentRepository;

    public OpenPositionService(
            OpenPositionRepository openPositionRepository,
            InstrumentRepository instrumentRepository) {
        this.openPositionRepository = openPositionRepository;
        this.instrumentRepository = instrumentRepository;
    }

    /**
     * Parses IBKR Open Positions CSV and persists to database using upsert logic.
     *
     * CSV Format:
     * - First row: column headers
     * - Comma-separated values
     * - UTF-8 encoding
     *
     * Required Columns:
     * - ClientAccountID: Account identifier
     * - Conid: Contract ID (natural key for upsert)
     * - AssetClass: OPT, STK, CASH, etc.
     * - Symbol: Ticker symbol
     * - Quantity: Position size
     * - Report Date: Snapshot date (format: YYYY-MM-DD)
     * - Currency: Currency code (USD, EUR, etc.)
     *
     * Optional Columns:
     * - Mark Price, Position Value, Cost Basis Price, Cost Basis Money
     * - FIFO PNL Unrealized, Side
     * - For options: Strike, Expiry, Put/Call, Underlying Symbol, Underlying Conid
     *
     * Position Upsert Logic:
     * - Check if position with same conid already exists via findByConid()
     * - If yes: update all fields on existing entity
     * - If no: create new entity and insert
     * - Natural key: conid (unique constraint enforced at database level)
     *
     * Instrument Upsert (NEW):
     * - Before position upsert, ensures InstrumentEntity exists via findByConid()
     * - If new: creates instrument with conid, ticker, and name from CSV
     * - If exists: updates name/ticker from CSV (IBKR is ground truth)
     * - Falls back to symbol if description is empty
     * - Preserves existing price/metadata fields
     * - Same transaction boundary: instrument + position = atomic
     *
     * @param csv the CSV data as a string (complete file content)
     * @return count in format "saved/deleted" (e.g., "50/10" = 50 saved, 10 deleted)
     * @throws IOException if CSV parsing fails
     * @throws RuntimeException if any field parsing fails (will cause transaction rollback)
     */
    @Transactional
    public String saveCSV(String csv) throws IOException {
        Set<Long> processedConids = new HashSet<>();
        Set<String> csvAccounts = new HashSet<>();
        int savedCount = 0;

        try (CSVParser csvRecords = parse(csv,
                CSVFormat.Builder.create()
                        .setHeader()                   // First row is headers
                        .setSkipHeaderRecord(true)     // Don't parse header row again
                        .setIgnoreHeaderCase(true)     // Case-insensitive header matching
                        .setTrim(true)                 // Trim whitespace from values
                        .get())) {

            for (CSVRecord csvRecord : csvRecords) {
                try {
                    // Parse required fields (will throw exception if missing/invalid)
                    String account = csvRecord.get("ClientAccountID");
                    Long conid = parseLong(csvRecord.get("Conid"));
                    String assetClass = csvRecord.get("AssetClass");

                    // ISSUE-046: Track for deletion logic (account-scoped)
                    csvAccounts.add(account);
                    processedConids.add(conid);
                    String symbol = csvRecord.get("Symbol");
                    LocalDate reportDate = LocalDate.parse(csvRecord.get("ReportDate"));
                    Integer quantity = parseInt(csvRecord.get("Quantity"));
                    String currency = csvRecord.get("CurrencyPrimary");
                    String description = getStringOrNull(csvRecord, "Description");

                    // INSTRUMENT UPSERT: Ensure instrument exists for this position
                    // IBKR Flex Report is ground truth for instrument identification
                    // For stocks: use conid (the stock's own contract ID)
                    // For options: use underlyingConid (the underlying stock's contract ID)
                    Long instrumentConid = "OPT".equals(assetClass)
                            ? parseLongOrNull(csvRecord, "UnderlyingConid")
                            : conid;
                    String underlyingSymbol = "OPT".equals(assetClass)
                            ? getStringOrNull(csvRecord, "UnderlyingSymbol")
                            : symbol;

                    // Only sync instruments for STK and OPT (stocks and options)
                    // Skip other asset classes (CASH, BOND, FOP, etc.)
                    if (instrumentConid != null && underlyingSymbol != null) {
                        // Check by conid first (primary key), then by ticker (unique constraint)
                        InstrumentEntity instrument = instrumentRepository.findByConid(instrumentConid);
                        if (instrument == null) {
                            // conid not found - check if ticker already exists
                            // (can happen if same symbol has different conid in CSV)
                            instrument = instrumentRepository.findByTicker(underlyingSymbol);
                            if (instrument == null) {
                                // Neither conid nor ticker exists - create new
                                log.debug("Creating new instrument for conid={}, symbol={}", instrumentConid, underlyingSymbol);
                                instrument = new InstrumentEntity();
                                instrument.setConid(instrumentConid);
                                instrument.setTicker(underlyingSymbol);
                            } else {
                                // Ticker exists but conid differs - update ticker's conid
                                log.debug("Updating conid for existing ticker: symbol={}, old_conid={}, new_conid={}",
                                        underlyingSymbol, instrument.getConid(), instrumentConid);
                                instrument.setConid(instrumentConid);
                            }
                        } else {
                            log.debug("Updating existing instrument for conid={}", instrumentConid);
                        }

                        // Update instrument name from CSV description (IBKR ground truth)
                        // Fallback to symbol if description is empty
                        String instrumentName = (description != null && !description.isEmpty())
                                ? description
                                : underlyingSymbol;
                        instrument.setName(instrumentName);

                        // Save instrument (upsert)
                        // Note: Don't touch other fields (price, Alpaca metadata, etc.)
                        instrumentRepository.save(instrument);
                        log.debug("Saved instrument: conid={}, ticker={}, name={}",
                                instrument.getConid(), instrument.getTicker(), instrument.getName());
                    } else if ("OPT".equals(assetClass)) {
                        // Options without underlying info - log warning but don't fail
                        log.warn("Option position missing underlying info: conid={}, symbol={}", conid, symbol);
                    }

                    // UPSERT LOGIC: Check if position already exists
                    OpenPositionEntity entity = openPositionRepository.findByConid(conid);
                    if (entity == null) {
                        entity = new OpenPositionEntity();
                        entity.setConid(conid);
                    }

                    // Always update all fields (upsert behavior)
                    entity.setAccount(account);
                    entity.setAssetClass(assetClass);
                    entity.setSymbol(symbol);
                    entity.setReportDate(reportDate);
                    entity.setQuantity(quantity);
                    entity.setCurrency(currency);

                    // Core Identification fields
                    entity.setAccountAlias(getStringOrNull(csvRecord, "AccountAlias"));

                    // Position Classification fields
                    entity.setSubCategory(getStringOrNull(csvRecord, "SubCategory"));
                    entity.setSide(getStringOrNull(csvRecord, "Side"));
                    entity.setLevelOfDetail(getStringOrNull(csvRecord, "LevelOfDetail"));

                    // Basic Position Info fields
                    entity.setDescription(getStringOrNull(csvRecord, "Description"));
                    entity.setMultiplier(parseDoubleOrNull(csvRecord, "Multiplier"));

                    // Pricing and Value fields
                    entity.setCostBasisPrice(parseDoubleOrNull(csvRecord, "CostBasisPrice"));
                    entity.setCostBasisMoney(parseDoubleOrNull(csvRecord, "CostBasisMoney"));
                    entity.setMarkPrice(parseDoubleOrNull(csvRecord, "MarkPrice"));
                    entity.setPositionValue(parseDoubleOrNull(csvRecord, "PositionValue"));
                    entity.setPositionValueInBase(parseDoubleOrNull(csvRecord, "PositionValueInBase"));
                    entity.setFxRateToBase(parseDoubleOrNull(csvRecord, "FXRateToBase"));
                    entity.setOpenPrice(parseDoubleOrNull(csvRecord, "OpenPrice"));

                    // Profit and Loss fields
                    entity.setFifoPnlUnrealized(parseDoubleOrNull(csvRecord, "FifoPnlUnrealized"));
                    entity.setUnrealizedCapitalGainsPnl(parseDoubleOrNull(csvRecord, "UnrealizedCapitalGainsPnL"));
                    entity.setUnrealizedFxPnl(parseDoubleOrNull(csvRecord, "UnrealizedFxPnL"));
                    entity.setPercentOfNAV(parseDoubleOrNull(csvRecord, "PercentOfNAV"));

                    // Securities Identification fields
                    entity.setSecurityId(getStringOrNull(csvRecord, "SecurityID"));
                    entity.setSecurityIdType(getStringOrNull(csvRecord, "SecurityIDType"));
                    entity.setCusip(getStringOrNull(csvRecord, "CUSIP"));
                    entity.setIsin(getStringOrNull(csvRecord, "ISIN"));
                    entity.setFigi(getStringOrNull(csvRecord, "FIGI"));
                    entity.setSedol(getStringOrNull(csvRecord, "SEDOL"));

                    // Options-specific fields (nullable for non-OPT assets)
                    entity.setStrike(parseDoubleOrNull(csvRecord, "Strike"));
                    entity.setExpirationDate(parseDateOrNull(csvRecord, "Expiry"));
                    entity.setPutCall(getStringOrNull(csvRecord, "Put/Call"));
                    entity.setUnderlyingConid(parseLongOrNull(csvRecord, "UnderlyingConid"));
                    entity.setUnderlyingSymbol(getStringOrNull(csvRecord, "UnderlyingSymbol"));
                    entity.setUnderlyingSecurityId(getStringOrNull(csvRecord, "UnderlyingSecurityID"));
                    entity.setUnderlyingListingExchange(getStringOrNull(csvRecord, "UnderlyingListingExchange"));

                    // Corporate Actions & Adjustments fields
                    entity.setPrincipalAdjustFactor(parseDoubleOrNull(csvRecord, "PrincipalAdjustFactor"));
                    entity.setAccruedInterest(parseDoubleOrNull(csvRecord, "AccruedInterest"));
                    entity.setCode(getStringOrNull(csvRecord, "Code"));
                    entity.setHoldingPeriodDateTime(parseLocalDateTimeOrNull(csvRecord, "HoldingPeriodDateTime"));
                    entity.setOriginatingOrderId(getStringOrNull(csvRecord, "OriginatingOrderID"));
                    entity.setOriginatingTransactionId(getStringOrNull(csvRecord, "OriginatingTransactionID"));

                    // Bond & Structured Products fields
                    entity.setIssuer(getStringOrNull(csvRecord, "Issuer"));
                    entity.setIssuerCountryCode(getStringOrNull(csvRecord, "IssuerCountryCode"));

                    // Commodities fields
                    entity.setCommodityType(getStringOrNull(csvRecord, "CommodityType"));
                    entity.setFineness(parseDoubleOrNull(csvRecord, "Fineness"));
                    entity.setWeight(getStringOrNull(csvRecord, "Weight"));
                    entity.setDeliveryType(getStringOrNull(csvRecord, "DeliveryType"));
                    entity.setSerialNumber(getStringOrNull(csvRecord, "SerialNumber"));

                    // Metadata fields
                    entity.setModel(getStringOrNull(csvRecord, "Model"));
                    entity.setOpenDateTime(parseLocalDateTimeOrNull(csvRecord, "OpenDateTime"));

                    // Save or update
                    openPositionRepository.save(entity);
                    savedCount++;

                } catch (Exception e) {
                    log.error("Error processing CSV record #{}: {}", csvRecord.getRecordNumber(), e.getMessage(), e);
                    throw new RuntimeException("Failed to parse record " + csvRecord.getRecordNumber(), e);
                }
            }

            // ISSUE-046: DELETE POSITIONS NOT IN CSV (account-scoped)
            int deletedCount = 0;
            if (!csvAccounts.isEmpty()) {
                // Query positions from CSV accounts
                List<OpenPositionEntity> accountPositions = new ArrayList<>();
                for (String account : csvAccounts) {
                    accountPositions.addAll(openPositionRepository.findByAccount(account));
                }

                // Filter to positions not processed (closed in IBKR)
                List<OpenPositionEntity> toDelete = accountPositions.stream()
                        .filter(entity -> !processedConids.contains(entity.getConid()))
                        .toList();

                if (!toDelete.isEmpty()) {
                    openPositionRepository.deleteAll(toDelete);
                    deletedCount = toDelete.size();
                    log.info("Deleted {} closed positions not in CSV from accounts: {}",
                            deletedCount, csvAccounts);
                }
            }

            log.info("OpenPositionService.saveCSV() completed: {} saved, {} deleted",
                    savedCount, deletedCount);

            // Return in format "saved/deleted"
            return savedCount + "/" + deletedCount;

        } catch (IOException e) {
            log.error("CSV parsing error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse CSV", e);
        }
    }

    /**
     * Parse Long from CSV field value.
     * Throws NumberFormatException if value is not a valid long.
     *
     * @param value the string value to parse
     * @return the parsed long value
     * @throws NumberFormatException if value is not a valid long
     */
    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            throw new NumberFormatException("Cannot parse null or empty string as Long");
        }
        return Long.parseLong(value.trim());
    }

    /**
     * Parse Integer from CSV field value.
     * Throws NumberFormatException if value is not a valid integer.
     *
     * @param value the string value to parse
     * @return the parsed integer value
     * @throws NumberFormatException if value is not a valid integer
     */
    private Integer parseInt(String value) {
        if (value == null || value.isEmpty()) {
            throw new NumberFormatException("Cannot parse null or empty string as Integer");
        }
        return Integer.parseInt(value.trim());
    }

    /**
     * Parse Double from CSV field value, returning null if field is missing or empty.
     * This is null-safe: missing columns or empty cells return null without error.
     *
     * @param record the CSV record
     * @param fieldName the field name to parse
     * @return the parsed double value, or null if field is missing/empty
     * @throws NumberFormatException if field exists but contains invalid number
     */
    private Double parseDoubleOrNull(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            if (value == null || value.isEmpty()) {
                return null;
            }
            return Double.parseDouble(value.trim());
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV - return null
            return null;
        }
    }

    /**
     * Parse LocalDate from CSV field value, returning null if field is missing or empty.
     * This is null-safe: missing columns or empty cells return null without error.
     *
     * @param record the CSV record
     * @param fieldName the field name to parse
     * @return the parsed date value, or null if field is missing/empty
     * @throws java.time.format.DateTimeParseException if field exists but contains invalid date
     */
    private LocalDate parseDateOrNull(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDate.parse(value.trim());
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV - return null
            return null;
        }
    }

    /**
     * Parse Long from CSV field value, returning null if field is missing or empty.
     * This is null-safe: missing columns or empty cells return null without error.
     *
     * @param record the CSV record
     * @param fieldName the field name to parse
     * @return the parsed long value, or null if field is missing/empty
     * @throws NumberFormatException if field exists but contains invalid number
     */
    private Long parseLongOrNull(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            if (value == null || value.isEmpty()) {
                return null;
            }
            return Long.parseLong(value.trim());
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV - return null
            return null;
        }
    }

    /**
     * Get String from CSV field value, returning null if field is missing or empty.
     * This is null-safe: missing columns or empty cells return null without error.
     *
     * @param record the CSV record
     * @param fieldName the field name to retrieve
     * @return the string value trimmed, or null if field is missing/empty
     */
    private String getStringOrNull(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            if (value == null || value.isEmpty()) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV - return null
            return null;
        }
    }

    /**
     * Parse LocalDateTime from CSV field value, returning null if field is missing or empty.
     * Supports multiple datetime formats commonly used in IBKR reports:
     * - ISO format: YYYY-MM-DD HH:mm:ss
     * - ISO with T: YYYY-MM-DDTHH:mm:ss
     *
     * @param record the CSV record
     * @param fieldName the field name to parse
     * @return the parsed LocalDateTime value, or null if field is missing/empty
     */
    private LocalDateTime parseLocalDateTimeOrNull(CSVRecord record, String fieldName) {
        try {
            String value = record.get(fieldName);
            if (value == null || value.isEmpty()) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            // Try ISO date-time format with space separator (IBKR format)
            try {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                // Try ISO format with T separator
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (IllegalArgumentException e) {
            // Column doesn't exist in CSV - return null
            return null;
        } catch (Exception e) {
            // Invalid date format - log and return null
            log.warn("Failed to parse LocalDateTime from field '{}': {}", fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Get all open option positions with calculated financial metrics.
     *
     * Returns all OPT (option) positions from the latest IBKR Flex Report snapshot.
     * Each position includes calculated metrics: daysBetween, daysLeft, breakEven,
     * annualizedRoiPercent, and probability of profit.
     *
     * Optional date filtering: If startDate is provided, only positions with
     * reportDate >= startDate are returned. If null, all positions are returned.
     *
     * @param startDate optional filter for earliest report date (null = all positions)
     * @return List of open option positions as DTOs with calculations applied
     */
    public List<PositionDto> getAllOpenOptionDtos(LocalDate startDate) {
        List<OpenPositionEntity> openOptions = (startDate != null)
                ? openPositionRepository.findAllOptionsByDate(startDate)
                : openPositionRepository.findAllOptions();
        return convertToPositionDtos(openOptions);
    }

    /**
     * Get open option positions for a specific underlying ticker.
     *
     * Returns all OPT positions where the underlying instrument matches the ticker.
     * Each position includes calculated metrics.
     *
     * Example: getOpenOptionsByTickerDto("SPY") returns all SPY options
     *
     * @param ticker the underlying instrument ticker (e.g., "SPY", "AAPL")
     * @return List of open options for the specified ticker
     */
    public List<PositionDto> getOpenOptionsByTickerDto(String ticker) {
        List<OpenPositionEntity> openOptions = openPositionRepository.findOptionsByUnderlyingTicker(ticker);
        return convertToPositionDtos(openOptions);
    }

    /**
     * Convert list of OpenPositionEntity to list of PositionDto.
     * Applies field mapping and calculates financial metrics for each position.
     *
     * @param entities list of open position entities from database
     * @return list of position DTOs with calculated metrics
     */
    private List<PositionDto> convertToPositionDtos(List<OpenPositionEntity> entities) {
        return entities.stream()
                .map(this::convertToPositionDto)
                .toList();
    }

    /**
     * Convert single OpenPositionEntity to PositionDto.
     *
     * Field Mappings:
     * - underlyingSymbol → ticker (underlying stock, not option symbol)
     * - quantity → quantity
     * - reportDate → tradeDate (snapshot date for calculations)
     * - expirationDate → expirationDate
     * - strike → positionValue (already in dollars)
     * - costBasisPrice → tradePrice (acquisition cost per unit)
     * - markPrice → marketPrice + marketValue
     * - fifoPnlUnrealized → unRealizedProfitOrLoss
     * - putCall ("P"/"C") → type (OptionType enum)
     *
     * Calculated Fields (via PositionMapper.calculateAndSetAnnualizedRoi):
     * - daysBetween
     * - daysLeft
     * - breakEven
     * - annualizedRoiPercent
     * - probability
     *
     * @param entity the open position entity from database
     * @return position DTO with field mapping and calculations applied
     */
    private PositionDto convertToPositionDto(OpenPositionEntity entity) {
        PositionDto dto = new PositionDto();

        // Basic identification
        dto.setTicker(entity.getUnderlyingSymbol());  // Use underlying, not option symbol!
        dto.setCode(entity.getSymbol());               // Option symbol code for Alpaca API
        dto.setQuantity(entity.getQuantity());

        // Dates
        dto.setTradeDate(entity.getReportDate());           // Report date as "trade date" for calculations
        dto.setExpirationDate(entity.getExpirationDate());

        // Pricing (strike already in dollars, not cents)
        dto.setPositionValue(entity.getStrike());           // Strike price = position value
        dto.setTradePrice(entity.getCostBasisPrice());      // Cost basis = trade price
        dto.setMarketPrice(entity.getMarkPrice());          // Mark price = market price
        dto.setMarketValue(entity.getMarkPrice());          // Also set marketValue for probability calc

        // Option type mapping: "P" or "C" string → OptionType enum
        if ("P".equals(entity.getPutCall())) {
            dto.setType(OptionType.PUT);
        } else if ("C".equals(entity.getPutCall())) {
            dto.setType(OptionType.CALL);
        }

        // P&L fields from IBKR
        dto.setUnRealizedProfitOrLoss(entity.getFifoPnlUnrealized());

        // Status (open positions are always OPEN)
        dto.setStatus(OptionStatus.OPEN);
        dto.setAssetClass(AssetClass.OPT);

        // CALCULATE derived fields: daysBetween, daysLeft, breakEven, ROI, probability
        // This matches OptionService.getPositionDtos() pattern
        calculateAndSetAnnualizedRoi(dto);

        return dto;
    }
}
