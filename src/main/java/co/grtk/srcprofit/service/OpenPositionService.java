package co.grtk.srcprofit.service;

import co.grtk.srcprofit.entity.OpenPositionEntity;
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

import static org.apache.commons.csv.CSVParser.parse;

/**
 * Service for parsing and persisting IBKR Flex Report Open Positions CSV data.
 *
 * Implements the CSV parsing and upsert workflow for open positions snapshots:
 * 1. Parse CSV using Apache Commons CSV
 * 2. For each row, check if position exists by conid
 * 3. If exists: update all fields (upsert behavior)
 * 4. If new: create and insert new position
 * 5. Return count of successful records
 *
 * Error Handling:
 * - Simple pattern (like NetAssetValueService): Exceptions propagate, transaction rolls back
 * - No detailed error tracking (unlike OptionService which uses CsvImportResult)
 * - Rationale: Open positions are snapshots, not transactional data
 *
 * Transaction Management:
 * - @Transactional ensures all-or-nothing semantics
 * - Any parsing error causes entire import to fail and rollback
 * - No partial imports or partial updates
 *
 * @see OpenPositionEntity for entity structure
 * @see OpenPositionRepository for persistence methods
 * @see FlexReportsService for orchestration
 */
@Service
public class OpenPositionService {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionService.class);

    private final OpenPositionRepository openPositionRepository;

    public OpenPositionService(OpenPositionRepository openPositionRepository) {
        this.openPositionRepository = openPositionRepository;
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
     * Upsert Logic:
     * - Check if position with same conid already exists via findByConid()
     * - If yes: update all fields on existing entity
     * - If no: create new entity and insert
     * - Natural key: conid (unique constraint enforced at database level)
     *
     * @param csv the CSV data as a string (complete file content)
     * @return count of records successfully processed (inserted or updated)
     * @throws IOException if CSV parsing fails
     * @throws RuntimeException if any field parsing fails (will cause transaction rollback)
     */
    @Transactional
    public int saveCSV(String csv) throws IOException {
        int rowCount = 0;

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
                    String symbol = csvRecord.get("Symbol");
                    LocalDate reportDate = LocalDate.parse(csvRecord.get("ReportDate"));
                    Integer quantity = parseInt(csvRecord.get("Quantity"));
                    String currency = csvRecord.get("CurrencyPrimary");

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
                    rowCount++;

                } catch (Exception e) {
                    log.error("Error processing CSV record #{}: {}", csvRecord.getRecordNumber(), e.getMessage(), e);
                    throw new RuntimeException("Failed to parse record " + csvRecord.getRecordNumber(), e);
                }
            }

            log.info("OpenPositionService.saveCSV() completed: {} records processed", rowCount);
            return rowCount;

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
}
