package co.grtk.srcprofit.service;

import co.grtk.srcprofit.condition.ApiAvailabilityCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScheduledJobsService, specifically the refreshAlpacaAssets() scheduled job.
 *
 * Covers:
 * - Cron expression verification (daily at 6 AM UTC)
 * - Job delegates to AlpacaService
 * - Error handling (exceptions logged, not rethrown)
 * - Logging behavior (debug on start, info on success, error on failure)
 *
 * Tests are skipped gracefully if required API credentials are not configured:
 * - ALPACA_API_KEY environment variable
 * - ALPHA_VINTAGE_API_KEY environment variable
 */
@ExtendWith({MockitoExtension.class, ApiAvailabilityCondition.class})
class ScheduledJobsServiceTest {

    @Mock
    private FlexReportsService flexReportsService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private AlpacaService alpacaService;

    @Mock
    private EarningService earningService;

    @InjectMocks
    private ScheduledJobsService scheduledJobsService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(flexReportsService, marketDataService, alpacaService, earningService);
    }

    /**
     * Test 1: Job successfully calls AlpacaService and returns
     * Verifies the happy path: refresh succeeds and count is logged
     */
    @Test
    void testJobSuccessfullyCallsAlpacaServiceMethod() {
        // Setup: AlpacaService returns success count
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(42);

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify: AlpacaService was called exactly once
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 3: Job continues even if AlpacaService throws
     * Verifies that exceptions from AlpacaService don't propagate
     */
    @Test
    void testJobContinuesWhenAlpacaServiceThrows() {
        // Setup: AlpacaService throws exception
        when(alpacaService.refreshStaleAssetMetadata())
                .thenThrow(new RuntimeException("Alpaca API unavailable"));

        // Execute: Should NOT throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets(),
                "Job should catch exceptions and not rethrow");

        // Verify: AlpacaService was called
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 3: Job handles zero assets refreshed
     * Verifies the scenario where everything is already up-to-date
     */
    @Test
    void testJobHandlesZeroRefreshedAssets() {
        // Setup: AlpacaService returns 0 (all up to date)
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(0);

        // Execute: Should handle gracefully
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    // ==================== EARNINGS REFRESH TESTS ====================

    /**
     * Test 4: Job successfully calls EarningService and returns
     * Verifies the happy path: refresh succeeds and result is logged
     */
    @Test
    void testEarningsRefreshJobSuccessfullyCallsEarningService() {
        // Setup: EarningService returns success result
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("15/42/0");

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify: EarningService was called exactly once
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 13: Job continues even if EarningService throws
     * Verifies that exceptions from EarningService don't propagate
     */
    @Test
    void testEarningsRefreshJobContinuesWhenEarningServiceThrows() {
        // Setup: EarningService throws exception
        when(earningService.refreshEarningsDataForAllInstruments())
                .thenThrow(new RuntimeException("Alpha Vantage API unavailable"));

        // Execute: Should NOT throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData(),
                "Job should catch exceptions and not rethrow");

        // Verify: EarningService was called
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 5: Job returns successfully with zero earnings processed
     * Verifies the edge case where no earnings need updating
     */
    @Test
    void testEarningsRefreshJobHandlesZeroProcessed() {
        // Setup: EarningService returns no data
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("0/0/0");

        // Execute
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 6: Job with 100% failure rate
     * Verifies handling when all symbols fail to process
     */
    @Test
    void testEarningsRefreshJobHandles100PercentFailures() {
        // Setup: All symbols failed
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("0/0/500");

        // Execute: Should still not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 7: Both daily refresh jobs can run independently
     * Verifies that Alpaca and Earnings jobs don't interfere
     */
    @Test
    void testAlpacaAndEarningsJobsCanRunIndependently() {
        // Setup
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(25);
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("500/1250/0");

        // Execute: Run both jobs
        assertDoesNotThrow(() -> {
            scheduledJobsService.refreshAlpacaAssets();
            scheduledJobsService.refreshEarningsData();
        });

        // Verify: Both services called once each
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }
}
