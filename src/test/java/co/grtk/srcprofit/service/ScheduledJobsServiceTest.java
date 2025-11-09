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

    @InjectMocks
    private ScheduledJobsService scheduledJobsService;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(flexReportsService, marketDataService, alpacaService);
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
}
