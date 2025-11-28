package co.grtk.srcprofit.service;

import co.grtk.srcprofit.dto.AlpacaOptionSnapshotDto;
import co.grtk.srcprofit.dto.AlpacaOptionSnapshotsResponseDto;
import co.grtk.srcprofit.entity.InstrumentEntity;
import co.grtk.srcprofit.entity.OptionSnapshotEntity;
import co.grtk.srcprofit.repository.InstrumentRepository;
import co.grtk.srcprofit.repository.OptionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing option snapshots downloaded from Alpaca Data API.
 *
 * Orchestrates batch refresh of option snapshots with filtering based on:
 * - Instrument price < $100
 * - Expiration date < 3 months
 * - Strike price between current price -20% and +10%
 * - Both PUT and CALL options (separate API calls)
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
    private final OptionRepository optionRepository;

    public OptionSnapshotService(AlpacaService alpacaService,
                                OptionSnapshotRepository optionSnapshotRepository,
                                InstrumentRepository instrumentRepository,
                                OptionRepository optionRepository) {
        this.alpacaService = alpacaService;
        this.optionSnapshotRepository = optionSnapshotRepository;
        this.instrumentRepository = instrumentRepository;
        this.optionRepository = optionRepository;
    }

    /**
     * Refresh option snapshots for instruments with open positions.
     *
     * Eligible instruments:
     * - Have at least one open position (status = OPEN)
     *
     * For each eligible instrument:
     * 1. Calculates strike price range: (price * 0.90) to (price * 1.10)
     * 2. Fetches CALL snapshots from Alpaca API
     * 3. Fetches PUT snapshots from Alpaca API
     * 4. Filters by expiration (< 3 months)
     * 5. Saves/updates snapshots in database
     *
     * Per-instrument errors do NOT abort the batch. Continues with next instrument.
     *
     * @return Total number of snapshots saved/updated
     */
    @Transactional
    public int refreshOptionSnapshots() {
        log.debug("OptionSnapshotService: Starting refreshOptionSnapshots()");

        // Find instruments with open positions
        List<InstrumentEntity> eligibleInstruments = optionRepository.findInstrumentsWithOpenPositions();

        log.info("Found {} instruments with open positions", eligibleInstruments.size());

        int totalSaved = 0;
        for (InstrumentEntity instrument : eligibleInstruments) {
            try {
                int saved = refreshSnapshotsForInstrument(instrument);
                totalSaved += saved;
                log.info("Refreshed {} snapshots for {}", saved, instrument.getTicker());
            } catch (Exception e) {
                log.warn("Failed to refresh snapshots for ticker {}: {}",
                        instrument.getTicker(), e.getMessage());
                // Continue with next instrument - don't abort batch
            }
        }

        log.info("OptionSnapshotService: Completed refreshOptionSnapshots() - {} total snapshots saved",
                totalSaved);
        return totalSaved;
    }

    /**
     * Refresh option snapshots for a specific instrument.
     *
     * Makes two API calls: one for CALL options, one for PUT options.
     * Calculates dynamic strike price range based on current price.
     *
     * @param instrument The instrument to refresh snapshots for
     * @return Number of snapshots saved/updated for this instrument
     */
    private int refreshSnapshotsForInstrument(InstrumentEntity instrument) {
        if (instrument.getPrice() == null || instrument.getPrice() <= 0) {
            log.warn("Skipping instrument {} - invalid price {}",
                    instrument.getTicker(), instrument.getPrice());
            return 0;
        }

        String ticker = instrument.getTicker();
        BigDecimal currentPrice = BigDecimal.valueOf(instrument.getPrice());

        // Calculate strike price range: -10% to +10%
        BigDecimal lowerStrike = currentPrice.multiply(BigDecimal.valueOf(MIN_STRIKE_MULTIPLIER))
                .setScale(2, RoundingMode.DOWN);
        BigDecimal upperStrike = currentPrice.multiply(BigDecimal.valueOf(MAX_STRIKE_MULTIPLIER))
                .setScale(2, RoundingMode.UP);

        // Calculate expiration date range: today to +3 months
        LocalDate today = LocalDate.now();
        LocalDate expirationMax = today.plusDays(EXPIRATION_DAYS);

        log.debug("Refreshing snapshots for {} - Strike range: ${} to ${}, Expiration: {} to {}",
                ticker, lowerStrike, upperStrike, today, expirationMax);

        int saved = 0;
        try {
            // Wait 3 seconds before fetching API data to avoid rate limiting
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Snapshot fetch delay interrupted for {}", ticker);
        }

        // Fetch CALL options
        saved += fetchAndSaveSnapshots(instrument, "call", lowerStrike, upperStrike, expirationMax);
        // Fetch PUT options
        saved += fetchAndSaveSnapshots(instrument, "put", lowerStrike, upperStrike, expirationMax);

        return saved;
    }

    /**
     * Fetch option snapshots for a specific type (call/put) and save them.
     *
     * @param instrument The underlying instrument
     * @param type "call" or "put"
     * @param lowerStrike Minimum strike price
     * @param upperStrike Maximum strike price
     * @param maxExpiration Maximum expiration date
     * @return Number of snapshots saved
     */
    private int fetchAndSaveSnapshots(InstrumentEntity instrument, String type,
                                      BigDecimal lowerStrike, BigDecimal upperStrike,
                                      LocalDate maxExpiration) {
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

                    // Parse expiration from OCC symbol and filter
                    LocalDate expiration = parseExpirationFromSymbol(symbol);
                    if (expiration.isAfter(maxExpiration)) {
                        log.debug("Skipping {} - expiration {} beyond 3 months", symbol, expiration);
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
