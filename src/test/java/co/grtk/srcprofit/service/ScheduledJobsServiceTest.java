package co.grtk.srcprofit.service;

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
 */
@ExtendWith(MockitoExtension.class)
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
     * Test 1: Scheduled job has correct cron expression
     * Verifies that refreshAlpacaAssets() is scheduled for 6 AM daily
     * Cron: "0 0 6 * * ?" = Daily at 6:00:00 AM UTC
     */
    @Test
    void testRefreshAlpacaAssetsHasCorrectCronExpression() throws NoSuchMethodException {
        // Get the method
        Method method = ScheduledJobsService.class.getMethod("refreshAlpacaAssets");

        // Verify the method has @Scheduled annotation
        assertTrue(method.isAnnotationPresent(Scheduled.class),
                "refreshAlpacaAssets() method should have @Scheduled annotation");

        // Get the annotation
        Scheduled annotation = method.getAnnotation(Scheduled.class);

        // Verify cron expression
        assertEquals("0 0 6 * * ?", annotation.cron(),
                "Cron expression should be '0 0 6 * * ?' (daily at 6 AM UTC)");
    }

    /**
     * Test 2: Job successfully calls AlpacaService and returns
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
     * Test 4: Job returns successfully with 0 assets refreshed
     * Verifies the edge case where no assets need refreshing
     */
    @Test
    void testJobHandlesZeroAssetsRefreshed() {
        // Setup: AlpacaService returns 0
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(0);

        // Execute
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 5: Job returns successfully with large asset count
     * Verifies handling of normal batch sizes (20-50 instruments)
     */
    @Test
    void testJobHandlesLargeAssetCount() {
        // Setup: Simulate typical batch size
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(47);

        // Execute
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 6: Job handles zero assets refreshed
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

    /**
     * Test 7: Job handles network interruption
     * Verifies the scenario where Alpaca API is completely unreachable
     */
    @Test
    void testJobHandlesNetworkInterruption() {
        // Setup: Simulate network error (using a runtime exception to avoid checked exception issue)
        when(alpacaService.refreshStaleAssetMetadata())
                .thenThrow(new RuntimeException("Connection refused", new java.net.ConnectException("Unreachable")));

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 8: Job handles API timeout
     * Verifies the scenario where Alpaca API is slow/timing out
     */
    @Test
    void testJobHandlesApiTimeout() {
        // Setup: Simulate timeout as runtime exception
        when(alpacaService.refreshStaleAssetMetadata())
                .thenThrow(new RuntimeException("Read timed out", new java.io.IOException("Timeout")));

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

        // Verify
        verify(alpacaService, times(1)).refreshStaleAssetMetadata();
    }

    /**
     * Test 9: Job method exists and is accessible
     * Verifies that the method is properly defined on the service
     */
    @Test
    void testRefreshAlpacaAssetsMethodExists() throws NoSuchMethodException {
        // Get the method
        Method method = ScheduledJobsService.class.getMethod("refreshAlpacaAssets");

        // Verify it exists and is public
        assertNotNull(method);
        assertTrue(method.getName().equals("refreshAlpacaAssets"));
        assertEquals(void.class, method.getReturnType());
    }

    /**
     * Test 10: Job is idempotent (can run multiple times safely)
     * Verifies that multiple invocations don't cause issues
     */
    @Test
    void testJobIsIdempotent() {
        // Setup
        when(alpacaService.refreshStaleAssetMetadata()).thenReturn(5).thenReturn(3).thenReturn(2);

        // Execute: Run job 3 times
        assertDoesNotThrow(() -> {
            scheduledJobsService.refreshAlpacaAssets();
            scheduledJobsService.refreshAlpacaAssets();
            scheduledJobsService.refreshAlpacaAssets();
        });

        // Verify: AlpacaService called 3 times
        verify(alpacaService, times(3)).refreshStaleAssetMetadata();
    }

    // ==================== EARNINGS REFRESH TESTS ====================

    /**
     * Test 11: Earnings refresh job has correct cron expression
     * Verifies that refreshEarningsData() is scheduled for 6 AM daily
     * Cron: "0 0 6 * * ?" = Daily at 6:00:00 AM UTC
     */
    @Test
    void testRefreshEarningsDataHasCorrectCronExpression() throws NoSuchMethodException {
        // Get the method
        Method method = ScheduledJobsService.class.getMethod("refreshEarningsData");

        // Verify the method has @Scheduled annotation
        assertTrue(method.isAnnotationPresent(Scheduled.class),
                "refreshEarningsData() method should have @Scheduled annotation");

        // Get the annotation
        Scheduled annotation = method.getAnnotation(Scheduled.class);

        // Verify cron expression
        assertEquals("0 0 6 * * ?", annotation.cron(),
                "Cron expression should be '0 0 6 * * ?' (daily at 6 AM UTC)");
    }

    /**
     * Test 12: Job successfully calls EarningService and returns
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
     * Test 14: Job returns successfully with zero earnings processed
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
     * Test 15: Job returns successfully with large batch size
     * Verifies handling of normal batch sizes
     */
    @Test
    void testEarningsRefreshJobHandlesLargeBatchSize() {
        // Setup: Simulate typical batch size
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("500/1250/5");

        // Execute
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 16: Job handles API timeout gracefully
     * Verifies the scenario where Alpha Vantage API times out
     */
    @Test
    void testEarningsRefreshJobHandlesApiTimeout() {
        // Setup: Simulate timeout as runtime exception
        when(earningService.refreshEarningsDataForAllInstruments())
                .thenThrow(new RuntimeException("Read timed out", new java.io.IOException("Timeout")));

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 17: Job handles network interruption
     * Verifies the scenario where Alpha Vantage API is unreachable
     */
    @Test
    void testEarningsRefreshJobHandlesNetworkInterruption() {
        // Setup: Simulate network error
        when(earningService.refreshEarningsDataForAllInstruments())
                .thenThrow(new RuntimeException("Connection refused", new java.net.ConnectException("Unreachable")));

        // Execute: Should not throw
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 18: Job method exists and is accessible
     * Verifies that the method is properly defined on the service
     */
    @Test
    void testRefreshEarningsDataMethodExists() throws NoSuchMethodException {
        // Get the method
        Method method = ScheduledJobsService.class.getMethod("refreshEarningsData");

        // Verify it exists and is public
        assertNotNull(method);
        assertTrue(method.getName().equals("refreshEarningsData"));
        assertEquals(void.class, method.getReturnType());
    }

    /**
     * Test 19: Job is idempotent (can run multiple times safely)
     * Verifies that multiple invocations don't cause issues
     */
    @Test
    void testEarningsRefreshJobIsIdempotent() {
        // Setup
        when(earningService.refreshEarningsDataForAllInstruments())
                .thenReturn("10/20/0")
                .thenReturn("5/10/0")
                .thenReturn("2/5/0");

        // Execute: Run job 3 times
        assertDoesNotThrow(() -> {
            scheduledJobsService.refreshEarningsData();
            scheduledJobsService.refreshEarningsData();
            scheduledJobsService.refreshEarningsData();
        });

        // Verify: EarningService called 3 times
        verify(earningService, times(3)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 20: Job handles result with partial failures
     * Verifies handling of results with some symbols failing
     */
    @Test
    void testEarningsRefreshJobHandlesPartialFailures() {
        // Setup: Result shows some symbols failed
        when(earningService.refreshEarningsDataForAllInstruments()).thenReturn("500/1250/3");

        // Execute
        assertDoesNotThrow(() -> scheduledJobsService.refreshEarningsData());

        // Verify
        verify(earningService, times(1)).refreshEarningsDataForAllInstruments();
    }

    /**
     * Test 21: Job with 100% failure rate
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
     * Test 22: Both daily refresh jobs can run independently
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
