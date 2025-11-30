package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaOptionSnapshotDto;
import co.grtk.srcprofit.dto.AlpacaOptionSnapshotsResponseDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OpenPositionEntity;
import co.grtk.srcprofit.entity.OptionSnapshotEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OpenPositionRepository;
import co.grtk.srcprofit.repository.OptionSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static co.grtk.srcprofit.mapper.PositionCalculationHelper.calculateDaysLeft;
import static co.grtk.srcprofit.mapper.PositionCalculationHelper.calculateProbability;

/**
 * Service for managing option snapshots downloaded from Alpaca Data API.
 *
 * Orchestrates batch refresh of option snapshots based on OpenPositionEntity records
 * from IBKR Flex Reports (source of truth). For each underlying symbol with open positions:
 * - Fetches snapshots for exact positions held
 * - Fetches snapshots for ±2 nearby strike prices (trading context)
 * - Validates all positions have valid underlyingInstrument relationships (fail-fast)
 * - Batches by underlying to minimize API calls
 *
 * Both PUT and CALL options refreshed via separate API calls per underlying.
 *
 * OCC Symbol Format: AAPL230120C00150000
 * - Root: AAPL (1-6 characters)
 * - Expiration: 230120 (YYMMDD)
 * - Type: C (call) or P (put)
 * - Strike: 00150000 (strike price * 1000, padded to 8 digits)
 */
@Service
public class OptionSnapshotService {
    private static final Logger log = LoggerFactory.getLogger(OptionSnapshotService.class);

    private static final double MIN_STRIKE_MULTIPLIER = 0.90;  // -10%
    private static final double MAX_STRIKE_MULTIPLIER = 1.10;  // +10%
    private static final long EXPIRATION_DAYS = 90;            // 3 months

    private final AlpacaService alpacaService;
    private final OptionSnapshotRepository optionSnapshotRepository;
    private final InstrumentRepository instrumentRepository;
    private final OpenPositionRepository openPositionRepository;

    public OptionSnapshotService(AlpacaService alpacaService,
                                OptionSnapshotRepository optionSnapshotRepository,
                                InstrumentRepository instrumentRepository,
                                OpenPositionRepository openPositionRepository) {
        this.alpacaService = alpacaService;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.instrumentRepository = instrumentRepository;
        this.openPositionRepository = openPositionRepository;
    }

    /**
     * Refresh option snapshots based on open positions from OpenPositionEntity.
     *
     * For each underlying symbol with open option positions:
     * 1. Validates all positions have valid underlyingInstrument relationships
     * 2. Calculates strike range from actual positions ± 2 strikes
     * 3. Calculates expiration range from actual positions
     * 4. Fetches CALL snapshots from Alpaca API
     * 5. Fetches PUT snapshots from Alpaca API
     * 6. Filters results to only save snapshots for held + nearby positions
     * 7. Saves/updates snapshots in database
     *
     * Per-underlying errors do NOT abort the batch. Continues with next underlying.
     *
     * @return Total number of snapshots saved/updated
     */
    @Transactional
    public int refreshOptionSnapshots() {
        log.debug("OptionSnapshotService: Starting refreshOptionSnapshots()");

        // Query all option positions with underlying instruments (JOIN FETCH prevents N+1)
        List<OpenPositionEntity> openOptions = openPositionRepository.findAllOptionsWithUnderlying();

        log.info("Found {} open option positions", openOptions.size());

        // Group positions by underlying symbol
        Map<String, List<OpenPositionEntity>> positionsByUnderlying = groupPositionsByUnderlying(openOptions);

        log.info("Processing {} underlying symbols", positionsByUnderlying.size());

        int totalSaved = 0;
        for (Map.Entry<String, List<OpenPositionEntity>> entry : positionsByUnderlying.entrySet()) {
            try {
                String underlyingSymbol = entry.getKey();
                List<OpenPositionEntity> positions = entry.getValue();

                int saved = refreshSnapshotsForUnderlying(underlyingSymbol, positions);
                totalSaved += saved;
                log.info("Refreshed {} snapshots for {}", saved, underlyingSymbol);
            } catch (Exception e) {
                log.warn("Failed to refresh snapshots for {}: {}",
                        entry.getKey(), e.getMessage());
                // Continue with next underlying - don't abort batch
            }
        }

        log.info("OptionSnapshotService: Completed refreshOptionSnapshots() - {} total snapshots saved",
                totalSaved);
        return totalSaved;
    }

    /**
     * Refresh option snapshots for a specific underlying symbol with given positions.
     *
     * Validates all positions have valid underlyingInstrument relationships (fail-fast).
     * Calculates strike and expiration ranges from actual positions.
     * Fetches snapshots for held + nearby positions via two API calls (CALL and PUT).
     *
     * @param underlyingSymbol The underlying symbol (e.g., "SPY")
     * @param positions The open option positions for this underlying
     * @return Number of snapshots saved/updated for this underlying
     * @throws IllegalStateException if any position lacks underlyingInstrument
     */
    private int refreshSnapshotsForUnderlying(String underlyingSymbol,
                                              List<OpenPositionEntity> positions) {
        // FAIL FAST: Validate all positions have underlyingInstrument
        for (OpenPositionEntity position : positions) {
            if (position.getUnderlyingInstrument() == null) {
                throw new IllegalStateException(
                    "Position missing underlyingInstrument: symbol=" +
                    position.getSymbol() + ", conid=" + position.getConid() +
                    ". This indicates a data quality issue.");
            }
        }

        // Get underlying instrument from first position (all have same underlying)
        InstrumentEntity underlyingInstrument = positions.get(0).getUnderlyingInstrument();

        if (underlyingInstrument.getPrice() == null ||
            underlyingInstrument.getPrice() <= 0) {
            log.warn("Skipping {} - invalid underlying price {}",
                    underlyingSymbol, underlyingInstrument.getPrice());
            return 0;
        }

        // Calculate strike range from ACTUAL positions ± 2 strikes
        StrikeRange strikeRange = calculateStrikeRange(positions);

        // Calculate expiration range from ACTUAL positions
        ExpirationRange expirationRange = calculateExpirationRange(positions);

        // Build set of OCC symbols to save (held + nearby strikes)
        Set<String> symbolsToSave = buildSymbolsToSave(positions);

        log.debug("Refreshing for {} - Strike: ${}-${}, Expiration: {}-{}, {} symbols",
                underlyingSymbol,
                strikeRange.min, strikeRange.max,
                expirationRange.min, expirationRange.max,
                symbolsToSave.size());

        // Rate limiting
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Snapshot fetch delay interrupted for {}", underlyingSymbol);
        }

        int saved = 0;
        // Fetch CALL options
        saved += fetchAndSaveSnapshots(
            underlyingInstrument, "call",
            strikeRange.min, strikeRange.max,
            expirationRange.max, symbolsToSave);

        // Fetch PUT options
        saved += fetchAndSaveSnapshots(
            underlyingInstrument, "put",
            strikeRange.min, strikeRange.max,
            expirationRange.max, symbolsToSave);

        return saved;
    }

    /**
     * Fetch option snapshots for a specific type (call/put) and save only those in symbolsToSave.
     *
     * @param instrument The underlying instrument
     * @param type "call" or "put"
     * @param lowerStrike Minimum strike price
     * @param upperStrike Maximum strike price
     * @param maxExpiration Maximum expiration date
     * @param symbolsToSave Set of OCC symbols to save (held + nearby positions)
     * @return Number of snapshots saved
     */
    private int fetchAndSaveSnapshots(InstrumentEntity instrument, String type,
                                      BigDecimal lowerStrike, BigDecimal upperStrike,
                                      LocalDate maxExpiration, Set<String> symbolsToSave) {
        try {
            AlpacaOptionSnapshotsResponseDto response = alpacaService.getOptionSnapshots(
                    instrument.getTicker(),
                    type,
                    lowerStrike.toString(),
                    upperStrike.toString()
            );

            if (response == null || response.getSnapshots() == null || response.getSnapshots().isEmpty()) {
                log.debug("No {} snapshots found for {} in specified range", type, instrument.getTicker());
                return 0;
            }

            int count = 0;
            for (Map.Entry<String, AlpacaOptionSnapshotDto> entry : response.snapshots.entrySet()) {
                try {
                    String symbol = entry.getKey();
                    AlpacaOptionSnapshotDto snapshotDto = entry.getValue();

                    // FILTER: Only save if symbol is in our set of held/nearby positions
                    if (!symbolsToSave.contains(symbol)) {
                        log.trace("Skipping {} - not in held or nearby positions", symbol);
                        continue;
                    }

                    // Parse expiration from OCC symbol and filter
                    LocalDate expiration = parseExpirationFromSymbol(symbol);
                    if (expiration.isAfter(maxExpiration)) {
                        log.debug("Skipping {} - expiration {} beyond range",
                                 symbol, expiration);
                        continue;
                    }

                    saveOrUpdateSnapshot(symbol, snapshotDto, instrument);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to save snapshot {}: {}", entry.getKey(), e.getMessage());
                    // Continue with next snapshot
                }
            }

            return count;
        } catch (Exception e) {
            log.error("Error fetching {} snapshots from Alpaca for {}: {}",
                    type, instrument.getTicker(), e.getMessage());
            throw e;
        }
    }

    /**
     * Save or update a single option snapshot.
     *
     * Uses upsert pattern: checks if snapshot already exists by symbol.
     * If exists, updates fields. If not, creates new snapshot.
     *
     * @param symbol The OCC symbol (from API response map key)
     * @param snapshotDto The snapshot data from Alpaca API
     * @param instrument The underlying instrument
     */
    private void saveOrUpdateSnapshot(String symbol, AlpacaOptionSnapshotDto snapshotDto, InstrumentEntity instrument) {

        // Check if snapshot already exists
        Optional<OptionSnapshotEntity> existing = optionSnapshotRepository.findBySymbol(symbol);
        OptionSnapshotEntity snapshot = existing.orElse(new OptionSnapshotEntity());

        // Parse OCC symbol to extract strike, expiration, type
        OccSymbolParts parts = parseOccSymbol(symbol);

        // Set contract identification (using direct field access for now)
        snapshot.symbol = symbol;
        snapshot.instrument = instrument;
        snapshot.optionType = parts.type;
        snapshot.strikePrice = parts.strikePrice;
        snapshot.expirationDate = parts.expirationDate;

        // Map latest trade data
        if (snapshotDto.latestTrade != null) {
            AlpacaOptionSnapshotDto.LatestTradeDto trade = snapshotDto.latestTrade;
            if (trade.timestamp != null && !trade.timestamp.isEmpty()) {
                snapshot.lastTradeTime = parseTimestamp(trade.timestamp);
            }
            snapshot.lastTradeExchange = trade.exchange;
            if (trade.price != null && !trade.price.isEmpty()) {
                snapshot.lastTradePrice = parseBigDecimal(trade.price);
            }
            snapshot.lastTradeSize = trade.size;
        }

        // Map latest quote data
        if (snapshotDto.latestQuote != null) {
            AlpacaOptionSnapshotDto.LatestQuoteDto quote = snapshotDto.latestQuote;
            if (quote.timestamp != null && !quote.timestamp.isEmpty()) {
                snapshot.lastQuoteTime = parseTimestamp(quote.timestamp);
            }
            snapshot.askExchange = quote.askExchange;
            if (quote.askPrice != null && !quote.askPrice.isEmpty()) {
                snapshot.askPrice = parseBigDecimal(quote.askPrice);
            }
            snapshot.askSize = quote.askSize;
            snapshot.bidExchange = quote.bidExchange;
            if (quote.bidPrice != null && !quote.bidPrice.isEmpty()) {
                snapshot.bidPrice = parseBigDecimal(quote.bidPrice);
            }
            snapshot.bidSize = quote.bidSize;
        }

        // Map Greeks
        if (snapshotDto.greeks != null) {
            AlpacaOptionSnapshotDto.GreeksDto greeks = snapshotDto.greeks;
            snapshot.delta = parseBigDecimal(greeks.delta);
            snapshot.gamma = parseBigDecimal(greeks.gamma);
            snapshot.theta = parseBigDecimal(greeks.theta);
            snapshot.vega = parseBigDecimal(greeks.vega);
            snapshot.rho = parseBigDecimal(greeks.rho);
            snapshot.impliedVolatility = parseBigDecimal(greeks.impliedVolatility);
        }

        // Calculate derived metrics (ROI, POP, daysLeft)
        calculateDerivedMetrics(snapshot, instrument);

        // Log calculated metrics
        log.debug("Calculated metrics for {}: daysLeft={}, roiOnCollateral={}, roiOnPremium={}, pop={}",
                snapshot.symbol, snapshot.daysLeft, snapshot.roiOnCollateral, snapshot.roiOnPremium, snapshot.pop);

        // Save to database (snapshotUpdatedAt will be set by Hibernate)
        optionSnapshotRepository.save(snapshot);

        boolean isNew = !existing.isPresent();
        log.debug("{}d snapshot: {} ({})", isNew ? "Saved new" : "Updated", symbol,
                parts.strikePrice + " " + parts.type + " " + parts.expirationDate);
    }

    /**
     * Delete expired option snapshots.
     *
     * Finds all snapshots with expiration date before today and deletes them.
     *
     * @return Number of snapshots deleted
     */
    @Transactional
    public int deleteExpiredSnapshots() {
        LocalDate today = LocalDate.now();
        int deletedCount = optionSnapshotRepository.deleteByExpirationDateBefore(today);

        log.info("Deleted {} expired option snapshots (expiration < {})", deletedCount, today);
        return deletedCount;
    }

    /**
     * Get all snapshots for a specific instrument ticker.
     *
     * @param ticker The instrument ticker (e.g., "AAPL")
     * @return List of snapshots for the ticker
     */
    public List<OptionSnapshotEntity> getSnapshotsForInstrument(String ticker) {
        InstrumentEntity instrument = instrumentRepository.findByTicker(ticker);
        if (instrument == null) {
            log.warn("Instrument not found: {}", ticker);
            return List.of();
        }

        List<OptionSnapshotEntity> snapshots = optionSnapshotRepository.findByInstrument(instrument);
        log.debug("Found {} snapshots for {}", snapshots.size(), ticker);
        return snapshots;
    }

    /**
     * Calculate derived metrics for an option snapshot.
     *
     * Calculates:
     * - daysLeft: Days until expiration (from today to expirationDate)
     * - roiOnCollateral: Annualized ROI on capital at risk (strike price)
     * - roiOnPremium: Annualized ROI on premium (midPrice)
     * - pop: Probability of Profit (two methods available)
     *
     * POP Calculation Strategy:
     * 1. Primary: Delta approximation if delta available → POP = (1 - |delta|) * 100
     * 2. Fallback: Probability method if delta unavailable but has price/daysLeft data
     *    Uses normal distribution: calculateProbability(strikePrice, instrumentPrice, daysLeft)
     * 3. Null: If delta unavailable AND insufficient data for probability method
     *
     * Null handling:
     * - If instrument price is null: logs warning, sets ROI/POP to null
     * - If midPrice is null (no bid/ask): sets ROI fields to null
     * - If delta is null: attempts probability method fallback, sets POP to null if fallback fails
     * - daysLeft is always calculated (expirationDate is always set)
     *
     * @param snapshot The snapshot to calculate metrics for
     * @param instrument The underlying instrument (provides current price)
     */
    private void calculateDerivedMetrics(OptionSnapshotEntity snapshot, InstrumentEntity instrument) {
        // Calculate daysLeft (always possible - expirationDate is always set)
        snapshot.daysLeft = calculateDaysLeft(snapshot.expirationDate);

        // Get midPrice for both ROI and POP calculations
        BigDecimal midPrice = snapshot.getMidPrice();

        // Check if we can calculate ROI metrics (requires instrument price)
        boolean canCalculateRoi = instrument != null && instrument.getPrice() != null && instrument.getPrice() > 0;
        if (!canCalculateRoi) {
            log.debug("Cannot calculate ROI for {} - instrument price not available", snapshot.symbol);
            snapshot.roiOnCollateral = null;
            snapshot.roiOnPremium = null;
        } else {
            // Calculate ROI metrics (require midPrice, positive daysLeft, valid strike)
            if (midPrice == null || midPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("Cannot calculate ROI for {} - midPrice not available (no bid/ask data)",
                        snapshot.symbol);
                snapshot.roiOnCollateral = null;
                snapshot.roiOnPremium = null;
            } else if (snapshot.daysLeft <= 0) {
                // Expired or expires today - ROI not meaningful
                log.debug("Cannot calculate ROI for {} - daysLeft is {} (expired or expires today)",
                        snapshot.symbol, snapshot.daysLeft);
                snapshot.roiOnCollateral = null;
                snapshot.roiOnPremium = null;
            } else if (snapshot.strikePrice == null || snapshot.strikePrice.compareTo(BigDecimal.ZERO) <= 0) {
                // Invalid strike price
                log.warn("Cannot calculate ROI for {} - invalid strike price", snapshot.symbol);
                snapshot.roiOnCollateral = null;
                snapshot.roiOnPremium = null;
            } else {
                // ROI on Collateral: (midPrice / strikePrice) * (365 / daysLeft) * 100
                BigDecimal roiOnCollateralDecimal = midPrice
                        .divide(snapshot.strikePrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(365.0 / snapshot.daysLeft))
                        .multiply(BigDecimal.valueOf(100.0));
                snapshot.roiOnCollateral = roiOnCollateralDecimal.setScale(0, RoundingMode.HALF_UP).intValue();

                // ROI on Premium: (365 / daysLeft) * 100
                // Simplified because premium/premium = 1
                double roiOnPremiumDecimal = (365.0 / snapshot.daysLeft) * 100.0;
                snapshot.roiOnPremium = (int) Math.round(roiOnPremiumDecimal);
            }
        }

        // Calculate POP using available data
        if (snapshot.delta != null) {
            // Primary method: Use delta approximation (fast, TastyTrade methodology)
            // POP = (1 - |delta|) * 100
            // Delta ranges from -1 to 1, so |delta| is 0 to 1
            // POP represents probability of profit for premium sellers
            double deltaAbs = Math.abs(snapshot.delta.doubleValue());
            double popDecimal = (1.0 - deltaAbs) * 100.0;
            snapshot.pop = (int) Math.round(popDecimal);

            // Clamp to valid range [0, 100]
            if (snapshot.pop < 0) snapshot.pop = 0;
            if (snapshot.pop > 100) snapshot.pop = 100;
        } else if (canCalculateRoi && snapshot.daysLeft > 0 && midPrice != null && midPrice.compareTo(BigDecimal.ZERO) > 0) {
            // Fallback method: Use probability calculation when delta not available
            // Requires: instrument price, midPrice (bid/ask), and daysLeft
            try {
                double strikeAsDouble = snapshot.strikePrice.doubleValue();
                double midPriceAsDouble = midPrice.doubleValue();
                double instrumentPrice = instrument.getPrice();

                // calculateProbability uses: strikePrice (position value), instrumentPrice (market value), daysLeft
                snapshot.pop = calculateProbability(strikeAsDouble, instrumentPrice, snapshot.daysLeft);
                log.debug("Calculated POP for {} using probability method: {}% (delta unavailable)",
                        snapshot.symbol, snapshot.pop);
            } catch (Exception e) {
                log.debug("Failed to calculate POP using probability method for {}: {}",
                        snapshot.symbol, e.getMessage());
                snapshot.pop = null;
            }
        } else {
            log.debug("Cannot calculate POP for {} - delta not available and insufficient data for probability method",
                    snapshot.symbol);
            snapshot.pop = null;
        }
    }

    // ============ Helper Methods ============

    /**
     * Parse OCC option symbol to extract strike, expiration, and type.
     *
     * Format: AAPL230120C00150000
     * - Root: AAPL (1-6 characters)
     * - Expiration: 230120 (YYMMDD)
     * - Type: C (call) or P (put)
     * - Strike: 00150000 (strike * 1000)
     *
     * @param symbol OCC symbol
     * @return Parsed symbol parts (strike, expiration, type)
     */
    private OccSymbolParts parseOccSymbol(String symbol) {
        if (symbol == null || symbol.length() < 16) {
            throw new IllegalArgumentException("Invalid OCC symbol format: " + symbol);
        }

        // Find the expiration date (YYMMDD follows the root symbol)
        // Root can be 1-6 characters, followed by 6 digits for YYMMDD
        int expirationStart = symbol.length() - 15; // 15 chars from end (YYMMDD + C/P + Strike)
        String expirationStr = symbol.substring(expirationStart, expirationStart + 6);

        // Parse YYMMDD to LocalDate
        try {
            int year = 2000 + Integer.parseInt(expirationStr.substring(0, 2));
            int month = Integer.parseInt(expirationStr.substring(2, 4));
            int day = Integer.parseInt(expirationStr.substring(4, 6));
            LocalDate expirationDate = LocalDate.of(year, month, day);

            // Get option type (C or P)
            char typeChar = symbol.charAt(expirationStart + 6);
            String type = (typeChar == 'C') ? "call" : "put";

            // Parse strike price (last 8 digits divided by 1000)
            String strikeStr = symbol.substring(expirationStart + 7);
            long strikeInt = Long.parseLong(strikeStr);
            BigDecimal strike = BigDecimal.valueOf(strikeInt).divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

            return new OccSymbolParts(strike, expirationDate, type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse OCC symbol: " + symbol, e);
        }
    }

    /**
     * Parse expiration date from OCC symbol.
     *
     * @param symbol OCC symbol
     * @return Expiration date
     */
    private LocalDate parseExpirationFromSymbol(String symbol) {
        return parseOccSymbol(symbol).expirationDate;
    }

    /**
     * Parse timestamp string to OffsetDateTime.
     *
     * Expects ISO 8601 format (e.g., "2023-01-15T20:00:00Z")
     *
     * @param timestamp ISO 8601 timestamp string
     * @return OffsetDateTime
     */
    private OffsetDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timestamp);
            return null;
        }
    }

    /**
     * Parse string to BigDecimal safely.
     *
     * @param value String value to parse
     * @return BigDecimal, or null if value is null or empty
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal: {}", value);
            return null;
        }
    }

    /**
     * Parse double to BigDecimal safely.
     * Converts Double from Alpaca API to BigDecimal with scale 6.
     *
     * @param value Double value to parse (e.g., from Greeks API response)
     * @return BigDecimal, or null if value is null
     */
    private BigDecimal parseBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value).setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from Double: {}", value);
            return null;
        }
    }

    // ============ New Helper Methods for OpenPositionEntity-based Refresh ============

    /**
     * Group open positions by underlying symbol.
     *
     * @param positions List of open option positions
     * @return Map of underlying symbol → list of positions for that underlying
     */
    private Map<String, List<OpenPositionEntity>> groupPositionsByUnderlying(
            List<OpenPositionEntity> positions) {
        return positions.stream()
                .collect(Collectors.groupingBy(OpenPositionEntity::getUnderlyingSymbol));
    }

    /**
     * Calculate strike price range from actual positions ± 2 strikes.
     *
     * Determines strike increment based on prices, then calculates bounds.
     *
     * @param positions Positions for the same underlying
     * @return StrikeRange with min/max strike prices
     */
    private StrikeRange calculateStrikeRange(List<OpenPositionEntity> positions) {
        double minStrike = positions.stream()
                .mapToDouble(OpenPositionEntity::getStrike)
                .min()
                .orElse(0.0);

        double maxStrike = positions.stream()
                .mapToDouble(OpenPositionEntity::getStrike)
                .max()
                .orElse(0.0);

        // Use average strike for increment calculation
        double avgStrike = (minStrike + maxStrike) / 2.0;
        double strikeIncrement = calculateStrikeIncrement(avgStrike);

        // Expand by ±2 strikes
        BigDecimal expandedMin = BigDecimal.valueOf(minStrike - (2 * strikeIncrement))
                .setScale(2, RoundingMode.DOWN);
        BigDecimal expandedMax = BigDecimal.valueOf(maxStrike + (2 * strikeIncrement))
                .setScale(2, RoundingMode.UP);

        return new StrikeRange(expandedMin, expandedMax);
    }

    /**
     * Calculate strike increment based on price level (OCC standard).
     *
     * @param strikePrice The strike price
     * @return Strike increment ($0.50, $1.00, $5.00, or $10.00)
     */
    private double calculateStrikeIncrement(double strikePrice) {
        if (strikePrice < 3.0) {
            return 0.50;
        } else if (strikePrice < 100.0) {
            return 1.00;
        } else if (strikePrice < 200.0) {
            return 5.00;
        } else {
            return 10.00;
        }
    }

    /**
     * Calculate expiration date range from actual positions.
     *
     * @param positions Positions for the same underlying
     * @return ExpirationRange with min/max expiration dates
     */
    private ExpirationRange calculateExpirationRange(List<OpenPositionEntity> positions) {
        LocalDate minExp = positions.stream()
                .map(OpenPositionEntity::getExpirationDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        LocalDate maxExp = positions.stream()
                .map(OpenPositionEntity::getExpirationDate)
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        return new ExpirationRange(minExp, maxExp);
    }

    /**
     * Build set of OCC symbols for held positions and ±2 nearby strikes.
     *
     * @param positions Positions for the same underlying
     * @return Set of OCC symbols to save (held + nearby)
     */
    private Set<String> buildSymbolsToSave(List<OpenPositionEntity> positions) {
        Set<String> symbols = new HashSet<>();

        for (OpenPositionEntity position : positions) {
            // Add exact position
            symbols.add(constructOccSymbol(position));

            // Add ±2 nearby strikes
            double strikeIncrement = calculateStrikeIncrement(position.getStrike());
            for (int i = -2; i <= 2; i++) {
                if (i == 0) continue; // Already added
                double nearbyStrike = position.getStrike() + (i * strikeIncrement);
                symbols.add(constructOccSymbolForStrike(
                    position.getUnderlyingSymbol(),
                    position.getExpirationDate(),
                    position.getPutCall(),
                    nearbyStrike
                ));
            }
        }

        return symbols;
    }

    /**
     * Construct OCC symbol from OpenPositionEntity.
     *
     * Format: {UNDERLYINGSYMBOL}{YYMMDD}{C|P}{STRIKE*1000 padded to 8 digits}
     * Example: SPY230120C00400000 (SPY, Jan 20 2023, Call, $400.00)
     *
     * @param position The option position
     * @return OCC symbol string
     */
    private String constructOccSymbol(OpenPositionEntity position) {
        return constructOccSymbolForStrike(
            position.getUnderlyingSymbol(),
            position.getExpirationDate(),
            position.getPutCall(),
            position.getStrike()
        );
    }

    /**
     * Construct OCC symbol from individual components.
     *
     * @param underlyingSymbol Underlying symbol (e.g., "SPY")
     * @param expirationDate Expiration date
     * @param putCall "P" or "C"
     * @param strikePrice Strike price
     * @return OCC symbol string
     */
    private String constructOccSymbolForStrike(String underlyingSymbol, LocalDate expirationDate,
                                               String putCall, double strikePrice) {
        // Format expiration as YYMMDD
        String expStr = String.format("%02d%02d%02d",
                expirationDate.getYear() % 100,
                expirationDate.getMonthValue(),
                expirationDate.getDayOfMonth());

        // Format strike as 8 digits (strike * 1000)
        long strikeInt = Math.round(strikePrice * 1000);
        String strikeStr = String.format("%08d", strikeInt);

        // Get put/call character
        char typeChar = "P".equalsIgnoreCase(putCall) ? 'P' : 'C';

        return underlyingSymbol + expStr + typeChar + strikeStr;
    }

    // ============ Helper Classes ============

    /**
     * Helper class for strike price range.
     */
    private static class StrikeRange {
        final BigDecimal min;
        final BigDecimal max;

        StrikeRange(BigDecimal min, BigDecimal max) {
            this.min = min;
            this.max = max;
        }
    }

    /**
     * Helper class for expiration date range.
     */
    private static class ExpirationRange {
        final LocalDate min;
        final LocalDate max;

        ExpirationRange(LocalDate min, LocalDate max) {
            this.min = min;
            this.max = max;
        }
    }

    /**
     * Helper class for parsed OCC symbol parts.
     */
    private static class OccSymbolParts {
        final BigDecimal strikePrice;
        final LocalDate expirationDate;
        final String type;

        OccSymbolParts(BigDecimal strikePrice, LocalDate expirationDate, String type) {
            this.strikePrice = strikePrice;
            this.expirationDate = expirationDate;
            this.type = type;
        }
    }
}
