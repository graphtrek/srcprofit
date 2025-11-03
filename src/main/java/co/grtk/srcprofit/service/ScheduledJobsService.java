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
 * 1. importFlexTrades() - Every 30 minutes (FLEX API - trades report)
 * 2. importFlexNetAssetValue() - Every 30 minutes (FLEX API - NAV report)
 * 3. refreshMarketData() - Every 60 seconds (Alpaca API - market data refresh)
 *
 * @see FlexReportsService for FLEX import orchestration
 * @see MarketDataService for market data refresh orchestration
 */
@Service
@EnableScheduling
public class ScheduledJobsService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledJobsService.class);

    private final FlexReportsService flexReportsService;
    private final MarketDataService marketDataService;

    public ScheduledJobsService(FlexReportsService flexReportsService,
                                 MarketDataService marketDataService) {
        this.flexReportsService = flexReportsService;
        this.marketDataService = marketDataService;
    }

    /**
     * Scheduled job: Import FLEX Trades report from Interactive Brokers.
     *
     * Schedule: Every 30 minutes, starting 1 minute after application startup
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
    @Scheduled(fixedDelay = 30, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
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
     * Schedule: Every 30 minutes, starting 1 minute after application startup
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
    @Scheduled(fixedDelay = 30, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
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
}
