# ISSUE-003: FLEX Reports Automatic Synchronization

**Created**: 2025-11-02 (Session 3)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Infrastructure
**Blocking**: None

---

## Problem

FLEX report imports from IBKR (Interactive Brokers) currently require manual triggering via REST endpoints (`/ibkrFlexTradesImport` and `/ibkrFlexNetAssetValueImport`). Users must remember to trigger these endpoints periodically to keep position data and Net Asset Value tracking up-to-date.

**Current Workflow**:
1. User manually calls `/ibkrFlexTradesImport` endpoint
2. User manually calls `/ibkrFlexNetAssetValueImport` endpoint
3. Data is synced only when user remembers to trigger import

**Pain Points**:
- Manual intervention required daily
- Easy to forget, leading to stale data
- No automated recovery if import fails
- No visibility into last successful sync time

---

## Root Cause

No scheduled background job exists to orchestrate automatic FLEX report synchronization. While all the infrastructure is in place (FLEX API integration in `IbkrService`, CSV parsing in `OptionService` and `NetAssetValueService`, Spring `@EnableScheduling` support), there is no service that ties them together on a schedule.

---

## Approach

Create a new `FlexReportSyncService` that follows the same pattern as the existing `MarketDataService` (which automatically refreshes Alpaca market data every 60 seconds).

### Implementation Steps

1. **Create FlexReportSyncService** (`src/main/java/co/grtk/srcprofit/service/FlexReportSyncService.java`)
   - Annotate with `@Service`
   - Inject `IbkrService`, `OptionService`, `NetAssetValueService`
   - Create two `@Scheduled` methods:
     - `syncFlexTradesReport()` - Calls FLEX Trades import workflow
     - `syncFlexNavReport()` - Calls FLEX NAV import workflow

2. **Add Configuration Properties** (`src/main/resources/application.properties`)
   - `srcprofit.flex.sync.enabled` - Enable/disable auto-sync (default: true)
   - `srcprofit.flex.sync.trades.cron` - Cron expression for trades sync (default: daily at 6 AM)
   - `srcprofit.flex.sync.nav.cron` - Cron expression for NAV sync (default: daily at 6:05 AM)
   - `srcprofit.flex.sync.retry.max-attempts` - Max retry attempts (default: 5)
   - `srcprofit.flex.sync.retry.delay-ms` - Delay between retries (default: 60000)

3. **Orchestrate Existing FLEX Workflow**
   - Call `IbkrService.getFlexWebServiceSendRequest()` to get reference code
   - Wait 15 seconds (IBKR report generation time)
   - Call `IbkrService.getFlexWebServiceGetStatement()` to retrieve CSV
   - Parse CSV using existing services:
     - Trades CSV → `OptionService.saveCSV()`
     - NAV CSV → `NetAssetValueService.saveCSV()`

4. **Implement Error Handling**
   - Retry logic with exponential backoff (max 5 attempts)
   - Log errors with context (timestamp, report type, attempt number)
   - Optional: Send notification on persistent failures (email/Slack)
   - Continue on single report failure (don't block other report)

5. **Add Comprehensive Logging**
   - INFO: Sync start/completion with timestamp
   - INFO: CSV rows processed count
   - WARN: Retry attempts
   - ERROR: Permanent failures after max retries
   - DEBUG: Full CSV content and API responses

6. **Create Unit Tests**
   - Test successful sync workflow
   - Test retry logic on transient failures
   - Test max retry limit enforcement
   - Test configuration property parsing
   - Mock IbkrService responses

7. **Create Integration Test**
   - Test full end-to-end sync with real FLEX API (test environment)
   - Verify CSV backup files created with correct timestamps
   - Verify database records created correctly

---

## Success Criteria

- [ ] `FlexReportSyncService` created with `@Scheduled` methods
- [ ] Scheduled job runs daily at configurable time (cron expression)
- [ ] Both FLEX reports sync automatically without manual intervention (Trades + NAV)
- [ ] Retry logic handles transient API failures (max 5 attempts with delay)
- [ ] CSV backup files created with timestamps in user home directory
- [ ] Errors logged with sufficient context for debugging
- [ ] Configuration allows disabling auto-sync (`srcprofit.flex.sync.enabled=false`)
- [ ] Configuration allows customizing cron schedules
- [ ] Unit tests achieve >80% code coverage for FlexReportSyncService
- [ ] Integration test verifies full sync workflow
- [ ] Manual trigger endpoints still work (`/ibkrFlexTradesImport`, `/ibkrFlexNetAssetValueImport`)
- [ ] Documentation updated in README or docs/trading/ explaining automatic sync behavior

---

## Acceptance Tests

```java
@SpringBootTest
class FlexReportSyncServiceIntegrationTest {

    @Autowired
    private FlexReportSyncService flexReportSyncService;

    @Autowired
    private OptionRepository optionRepository;

    @Autowired
    private NetAssetValueRepository netAssetValueRepository;

    @Test
    void testFlexTradesSyncCreatesRecords() {
        // Given: Empty database
        long initialTradeCount = optionRepository.count();

        // When: Sync trades report
        flexReportSyncService.syncFlexTradesReport();

        // Then: New trade records created
        long finalTradeCount = optionRepository.count();
        assertThat(finalTradeCount).isGreaterThan(initialTradeCount);
    }

    @Test
    void testFlexNavSyncCreatesRecords() {
        // Given: Empty database
        long initialNavCount = netAssetValueRepository.count();

        // When: Sync NAV report
        flexReportSyncService.syncFlexNavReport();

        // Then: New NAV records created
        long finalNavCount = netAssetValueRepository.count();
        assertThat(finalNavCount).isGreaterThan(initialNavCount);
    }

    @Test
    void testSyncRespectsEnabledConfiguration() {
        // Given: Auto-sync disabled
        // (Set srcprofit.flex.sync.enabled=false in test properties)

        // When: Scheduled time triggers
        // Then: No sync occurs (verify via logs or database count unchanged)
    }
}

@ExtendWith(MockitoExtension.class)
class FlexReportSyncServiceUnitTest {

    @Mock
    private IbkrService ibkrService;

    @Mock
    private OptionService optionService;

    @InjectMocks
    private FlexReportSyncService flexReportSyncService;

    @Test
    void testRetryLogicOnTransientFailure() {
        // Given: FLEX API fails twice, succeeds third time
        when(ibkrService.getFlexWebServiceSendRequest(anyString()))
            .thenThrow(new RuntimeException("Network error"))
            .thenThrow(new RuntimeException("Network error"))
            .thenReturn(new FlexStatementResponse("REF123", "Success"));

        // When: Sync triggered
        flexReportSyncService.syncFlexTradesReport();

        // Then: Retries 3 times total, succeeds
        verify(ibkrService, times(3)).getFlexWebServiceSendRequest(anyString());
    }

    @Test
    void testMaxRetriesEnforced() {
        // Given: FLEX API always fails
        when(ibkrService.getFlexWebServiceSendRequest(anyString()))
            .thenThrow(new RuntimeException("Network error"));

        // When: Sync triggered
        assertThrows(RuntimeException.class, () -> {
            flexReportSyncService.syncFlexTradesReport();
        });

        // Then: Retries exactly 5 times (max attempts)
        verify(ibkrService, times(5)).getFlexWebServiceSendRequest(anyString());
    }
}
```

---

## Related Issues

- Related: ISSUE-001 (CALL Option Sell Obligations - uses trade data from FLEX reports)
- Blocks: Future issues requiring up-to-date position tracking
- Enables: Real-time portfolio analytics with fresh data

---

## Notes

### Existing Infrastructure (Already Implemented)

**FLEX API Integration** (`IbkrService.java:61-81`):
- `getFlexWebServiceSendRequest(String queryId)` - Sends query, returns reference code
- `getFlexWebServiceGetStatement(String referenceCode)` - Retrieves CSV report
- Two-step workflow with 15-second delay for report generation

**CSV Parsing Services**:
- `OptionService.saveCSV(String csv)` (line 339+) - Parses trades CSV, saves to `OptionEntity`
- `NetAssetValueService.saveCSV(String csv)` (line 124+) - Parses NAV CSV, saves to `NetAssetValueEntity`
- Both use Apache Commons CSV parser

**Configuration** (docker-compose.yaml):
```yaml
IBKR_FLEX_URL=${IBKR_FLEX_URL}
IBKR_FLEX_API_TOKEN=${IBKR_FLEX_API_TOKEN}
IBKR_FLEX_TRADES_ID=${IBKR_FLEX_TRADES_ID}           # Query ID for trades report
IBKR_FLEX_NET_ASSET_VALUE_ID=${IBKR_FLEX_NET_ASSET_VALUE_ID}  # Query ID for NAV report
```

**Scheduling Pattern** (`MarketDataService.java`):
```java
@Scheduled(fixedDelay = 60000, initialDelay = 10000)
public void refreshAlpacaMarketData() {
    // Pattern to follow for FLEX sync
}
```

**Manual Endpoints** (`IbkrRestController.java:46-112`):
- `/ibkrFlexTradesImport` - Currently manual trigger
- `/ibkrFlexNetAssetValueImport` - Currently manual trigger
- These should remain available for manual on-demand imports

### FLEX Report Types Used

1. **Trades Report** (`IBKR_FLEX_TRADES_ID`)
   - Options trade executions
   - Entry dates, strikes, quantities, premiums
   - Fees and commissions
   - Used for position tracking and P&L calculations

2. **Net Asset Value Report** (`IBKR_FLEX_NET_ASSET_VALUE_ID`)
   - Daily portfolio value breakdown
   - Cash, stock, and options values
   - Used for portfolio analytics and performance tracking

### IBKR FLEX API Quirks

- **Report Generation Time**: 15-second delay required between SendRequest and GetStatement
- **Rate Limits**: Unknown, implement retry with exponential backoff to be safe
- **CSV Format**: Varies by query configuration, existing parsers handle current format
- **Backup Files**: Currently saved to `~/flex_trades.csv` and `~/flex_nav.csv` - consider adding timestamps

### Recommended Cron Schedules

- **Trades Sync**: Daily at 6:00 AM (after market close, before user wakes up)
  - `0 0 6 * * ?` (6 AM every day)
- **NAV Sync**: Daily at 6:05 AM (5 minutes after trades, ensure trades processed first)
  - `0 5 6 * * ?` (6:05 AM every day)

### Implementation Considerations

1. **Idempotency**: Ensure duplicate imports don't create duplicate records (existing CSV parsers should handle this)
2. **Time Zones**: Ensure cron expressions account for server time zone vs market hours
3. **Holidays/Weekends**: FLEX reports may have no new data on non-trading days (handle gracefully)
4. **CSV Backup Rotation**: Consider adding date to filename to avoid overwriting (`flex_trades_2025-11-02.csv`)
5. **Monitoring**: Add metrics/logging for observability (last sync time, success rate, row counts)

### References

- IBKR FLEX API Documentation: https://www.interactivebrokers.com/en/software/am/am/reports/flex_web_service_version_3.htm
- Spring @Scheduled Documentation: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
- Existing MarketDataService: `src/main/java/co/grtk/srcprofit/service/MarketDataService.java`

### Estimated Effort

**Total**: 3-4 hours

**Breakdown**:
- FlexReportSyncService implementation: 1-2 hours
- Configuration properties: 30 minutes
- Error handling and logging: 1 hour
- Unit tests: 45 minutes
- Integration test: 45 minutes
- Documentation: 30 minutes

**Risk**: LOW - All infrastructure exists, just need orchestration layer

---

**Issue Type**: Enhancement
**Complexity**: Medium
**Risk**: Low
