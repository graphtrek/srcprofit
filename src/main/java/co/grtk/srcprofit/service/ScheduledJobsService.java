package co.grtk.srcprofit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Centralized service for managing all scheduled jobs in the application.
 *
 * Consolidates @Scheduled annotations from multiple services to provide:
 * - Single source of truth for job scheduling
 * - Centralized logging and observability
 * - Consistent retry/error handling patterns
 * - Foundation for future monitoring/metrics
 *
 * Each scheduled method delegates to its corresponding orchestrator service.
 * Services are responsible for business logic; this service is responsible for scheduling.
 *
 * Scheduled Jobs:
 * 1. importFlexTrades() - Every 6 hours (FLEX API - trades report)
 * 2. importFlexNetAssetValue() - Every 6 hours (FLEX API - NAV report)
 * 3. refreshMarketData() - Every 5 minutes (Alpaca API - market data refresh)
 * 4. refreshAlpacaAssets() - Every 12 hours (Alpaca Assets API - metadata refresh)
 * 5. refreshEarningsData() - Every 12 hours (Alpha Vantage - earnings calendar refresh)
 * 6. refreshOptionSnapshots() - Every 15 minutes (Alpaca Data API - option snapshots refresh)
 * 7. cleanupExpiredOptionSnapshots() - Every 24 hours (Option snapshots cleanup)
 *
 * @see FlexReportsService for FLEX import orchestration
 * @see MarketDataService for market data refresh orchestration
 * @see AlpacaService for Alpaca assets metadata refresh orchestration
 * @see EarningService for earnings calendar refresh orchestration
 * @see OptionSnapshotService for option snapshots refresh orchestration
 */
@Service
@EnableScheduling
public class ScheduledJobsService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobsService.class);

    private final FlexReportsService flexReportsService;
    private final MarketDataService marketDataService;
    private final AlpacaService alpacaService;
    private final EarningService earningService;
    private final OptionSnapshotService optionSnapshotService;

    public ScheduledJobsService(FlexReportsService flexReportsService,
                                 MarketDataService marketDataService,
                                 AlpacaService alpacaService,
                                 EarningService earningService,
                                 OptionSnapshotService optionSnapshotService) {
        this.flexReportsService = flexReportsService;
        this.marketDataService = marketDataService;
        this.alpacaService = alpacaService;
        this.earningService = earningService;
        this.optionSnapshotService = optionSnapshotService;
    }

    /**
     * Scheduled job: Import FLEX Trades report from Interactive Brokers.
     *
     * Schedule: Every 6 hours, starting 1 minute after application startup
     * Delegates to: FlexReportsService.importFlexTrades()
     *
     * Retry Logic:
     * - Automatically retried on failure (see FlexReportsService for details)
     * - Max 5 retry attempts
     * - Returns status string for logging
     *
     * @return Status string: "{csvRecords}/{dataFixRecords}/{counter}" on success
     *         or "WAITING_FOR REPORT /{counter}" if still waiting for API response
     */
    @Scheduled(fixedDelay = 360, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public String importFlexTrades() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting importFlexTrades() job");
            String result = flexReportsService.importFlexTrades();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed importFlexTrades() in {}ms with result: {}", elapsedTime, result);
            return result;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: importFlexTrades() failed after {}ms - {}", elapsedTime, e.getMessage(), e);
            throw new RuntimeException("importFlexTrades job failed", e);
        }
    }

    /**
     * Scheduled job: Import FLEX Net Asset Value report from Interactive Brokers.
     *
     * Schedule: Every 6 hours, starting 1 minute after application startup
     * Delegates to: FlexReportsService.importFlexNetAssetValue()
     *
     * Retry Logic:
     * - Automatically retried on failure (see FlexReportsService for details)
     * - Max 5 retry attempts
     * - Returns status string for logging
     *
     * @return Status string: "{records}/{counter}" on success
     *         or "WAITING_FOR REPORT /{counter}" if still waiting for API response
     */
    @Scheduled(fixedDelay = 360, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public String importFlexNetAssetValue() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting importFlexNetAssetValue() job");
            String result = flexReportsService.importFlexNetAssetValue();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed importFlexNetAssetValue() in {}ms with result: {}", elapsedTime, result);
            return result;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: importFlexNetAssetValue() failed after {}ms - {}", elapsedTime, e.getMessage(), e);
            throw new RuntimeException("importFlexNetAssetValue job failed", e);
        }
    }

    /**
     * Scheduled job: Refresh market data from Alpaca API.
     *
     * Schedule: Every 60 seconds, starting 10 seconds after application startup
     * Delegates to: MarketDataService.refreshAlpacaMarketData()
     *
     * Updates:
     * - Stock quotes for all instruments
     * - Option market prices for all open options
     *
     * This is a stateless operation with no retry logic (simple API call).
     */
    @Scheduled(fixedDelay = 5, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void refreshMarketData() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting refreshMarketData() job");
            marketDataService.refreshAlpacaMarketData();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed refreshMarketData() in {}ms", elapsedTime);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: refreshMarketData() failed after {}ms - {}", elapsedTime, e.getMessage(), e);
            // Don't rethrow - market data refresh failures shouldn't crash the app
            log.debug("ScheduledJobsService: Market data refresh will retry on next schedule");
        }
    }

    /**
     * Scheduled job: Refresh stale Alpaca asset metadata.
     *
     * Schedule: Every 12 hours, starting 1 minute after application startup
     * Only refreshes instruments with metadata older than 24 hours (not all 500+).
     * Delegates to: AlpacaService.refreshStaleAssetMetadata()
     *
     * Updates:
     * - Instrument tradability, marginability, shortability
     * - Easy-to-borrow status (dynamic field, changes intraday)
     * - Exchange, asset class, maintenance margin requirement
     *
     * Non-critical job: Errors are logged but don't abort the batch.
     * Individual instrument failures don't prevent others from being refreshed.
     *
     * Performance: Typically 20-50 instruments refreshed (~5-10 seconds)
     * API Rate Limit: Alpaca Assets API allows 200 requests/minute (well below limit)
     */
    @Scheduled(fixedDelay = 720, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void refreshAlpacaAssets() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting refreshAlpacaAssets() job");
            int refreshedCount = alpacaService.refreshStaleAssetMetadata();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed refreshAlpacaAssets() in {}ms, refreshed {} instruments",
                    elapsedTime, refreshedCount);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: refreshAlpacaAssets() failed after {}ms - {}",
                    elapsedTime, e.getMessage(), e);
            log.debug("ScheduledJobsService: Asset refresh will retry on next schedule");
        }
    }

    /**
     * Scheduled job: Refresh earnings calendar data for all instruments.
     *
     * Schedule: Every 12 hours, starting 1 minute after application startup
     * Delegates to: EarningService.refreshEarningsDataForAllInstruments()
     *
     * Updates:
     * - EarningEntity records with new earnings calendar data from Alpha Vantage API
     * - InstrumentEntity.earningDate with earliest future earnings date
     *
     * Non-critical job: Errors are logged but don't abort the batch.
     * Per-symbol API failures don't prevent others from being processed.
     *
     * Performance: Typically processes all instruments once daily (~10-20 seconds)
     * API Rate Limit: Alpha Vantage API allows 5 requests/minute (batch request used)
     *
     * Returns: Summary string "{processed}/{newRecords}/{failures}"
     */
    @Scheduled(fixedDelay = 720, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void refreshEarningsData() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting refreshEarningsData() job");
            String result = earningService.refreshEarningsDataForAllInstruments();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed refreshEarningsData() in {}ms with result: {}",
                    elapsedTime, result);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: refreshEarningsData() failed after {}ms - {}",
                    elapsedTime, e.getMessage(), e);
            log.debug("ScheduledJobsService: Earnings refresh will retry on next schedule");
        }
    }

    /**
     * Scheduled job: Refresh option snapshots for instruments with open positions.
     *
     * Schedule: Every 15 minutes, starting 5 minutes after application startup
     * Delegates to: OptionSnapshotService.refreshOptionSnapshots()
     *
     * Fetches latest trading data (prices, quotes, Greeks) from Alpaca Data API.
     *
     * Filters:
     * - Only instruments with at least one open position (status = OPEN)
     * - Only snapshots with expiration <= 3 months from today
     * - Only snapshots with strike price between (price * 0.90) and (price * 1.10)
     * - Both CALL and PUT options (separate API calls)
     *
     * Error Handling:
     * - Per-instrument failures don't abort the batch
     * - Continues with next instrument on error
     * - Returns count of successfully saved snapshots
     *
     * Non-critical job: Errors are logged but don't crash the application.
     */
    @Scheduled(fixedDelay = 15, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void refreshOptionSnapshots() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting refreshOptionSnapshots() job");
            int count = optionSnapshotService.refreshOptionSnapshots();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed refreshOptionSnapshots() in {}ms - {} snapshots saved",
                    elapsedTime, count);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: refreshOptionSnapshots() failed after {}ms - {}",
                    elapsedTime, e.getMessage(), e);
            log.debug("ScheduledJobsService: Option snapshot refresh will retry on next schedule");
        }
    }

    /**
     * Scheduled job: Delete expired option snapshots.
     *
     * Schedule: Every 24 hours, starting 30 minutes after application startup
     * Delegates to: OptionSnapshotService.deleteExpiredSnapshots()
     *
     * Removes:
     * - All snapshots with expiration_date < today
     *
     * Non-critical job: Errors are logged but don't crash the application.
     */
    @Scheduled(fixedDelay = 1440, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredOptionSnapshots() {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("ScheduledJobsService: Starting cleanupExpiredOptionSnapshots() job");
            int count = optionSnapshotService.deleteExpiredSnapshots();
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("ScheduledJobsService: Completed cleanupExpiredOptionSnapshots() in {}ms - {} snapshots deleted",
                    elapsedTime, count);
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("ScheduledJobsService: cleanupExpiredOptionSnapshots() failed after {}ms - {}",
                    elapsedTime, e.getMessage(), e);
            log.debug("ScheduledJobsService: Option snapshot cleanup will retry on next schedule");
        }
    }
}
