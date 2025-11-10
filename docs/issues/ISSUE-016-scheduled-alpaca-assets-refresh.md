# ISSUE-016: Scheduled Alpaca Assets Metadata Refresh

**Created**: 2025-11-09 (Session Current)
**Status**: CLOSED
**Completed**: 2025-11-09 (Session Current)
**Priority**: MEDIUM
**Category**: Enhancement
**Blocking**: None

---

## Problem

The Alpaca Assets API integration (ISSUE-014) provides comprehensive instrument metadata (tradability, marginability, borrowability, exchange, etc.) but only refreshes on-demand when positions are viewed. This means stale data persists between position views, particularly problematic for dynamic fields like `easy_to_borrow` which changes intraday based on availability.

**Current Issues**:
1. **Stale Data**: Asset metadata can be weeks old without scheduled refresh
2. **No TTL Strategy**: `alpacaMetadataUpdatedAt` timestamp exists but no job uses it
3. **Incomplete ISSUE-014**: API integration is feature-complete but not fully deployed without scheduled refresh
4. **Manual Workaround**: Users must navigate to positions page to trigger cache refresh
5. **Easy-to-Borrow Gap**: Borrowability status changes intraday but app shows cached data until manual refresh

**Impact**:
- Risk management decisions based on outdated position data
- Missed short-selling opportunities (don't know if new shares became borrowable)
- Margin requirement calculations may be stale

---

## Root Cause

ISSUE-014 implemented the Alpaca Assets API integration with cache-first logic in `PositionController`, but no scheduled job was created to proactively refresh stale metadata. The design was intended to be cache-first for performance, but without scheduled refresh, it defaults to "cache-always-stale".

---

## Approach

**Phase 1: Repository Enhancement** (Add query method)
1. Add query method to `InstrumentRepository` to find instruments with stale Alpaca metadata
   - Query: `alpacaMetadataUpdatedAt < NOW() - 24 hours OR alpacaMetadataUpdatedAt IS NULL`
   - Return: List of stale instruments for batch refresh
   - Motivation: Not all instruments are actively traded; only refresh those needing updates

**Phase 2: AlpacaService Enhancement** (Add refresh orchestrator method)
1. Add `refreshStaleAssetMetadata()` method to existing `AlpacaService`
   - Fetches stale instruments from repository
   - For each instrument:
     - Call Alpaca API: `alpacaService.getAsset(ticker)`
     - Update instrument: `alpacaService.saveAssetMetadata(assetDto, instrument)`
     - Save to database
   - Continue on individual failures (don't abort batch)
   - Return count of refreshed instruments for logging
2. Key Implementation Details:
   - Reuse existing `getAsset()` and `saveAssetMetadata()` methods
   - Per-ticker error handling: log warnings but continue
   - Update `alpacaMetadataUpdatedAt` timestamp automatically in `saveAssetMetadata()`

**Phase 3: ScheduledJobsService Enhancement** (Add scheduled job)
1. Add new `refreshAlpacaAssets()` method to `ScheduledJobsService`
2. Schedule: Daily at 6:00 AM (`@Scheduled(cron = "0 0 6 * * ?")`)
   - Rationale: Market closes at 4 PM EST; 6 AM allows full 14-hour staleness threshold
   - After market close, most metadata (marginable, shortable, tradable) remains constant
   - Only `easy_to_borrow` and `status` change intraday, acceptable to refresh once daily
   - Low API volume: Typically 20-50 instruments refreshed per day (not all 500+)
3. Follow existing pattern:
   - Centralized logging (debug on start, info on success, error on failure)
   - Timing metrics (elapsed time)
   - Delegate to `AlpacaService.refreshStaleAssetMetadata()`
   - Non-critical (catch exceptions, don't rethrow like `MarketDataService`)

**Phase 4: Testing & Validation**
1. Unit tests for repository query
2. Integration tests for refresh job
3. Verify all stale instruments refreshed correctly
4. Test error handling (API failure for one ticker doesn't abort batch)
5. Monitor API call volume

---

## Success Criteria

- [ ] Repository: `InstrumentRepository.findStaleAlpacaAssets(Instant threshold)` implemented
- [ ] AlpacaService: `refreshStaleAssetMetadata()` method added and tested
- [ ] ScheduledJobsService: `refreshAlpacaAssets()` job added with daily 6 AM schedule
- [ ] Cron expression: `@Scheduled(cron = "0 0 6 * * ?")`
- [ ] Logging: Debug, info, and error messages follow existing patterns
- [ ] Error handling: Per-ticker exceptions logged but don't abort batch
- [ ] Timestamp: `alpacaMetadataUpdatedAt` updated automatically when metadata refreshed
- [ ] Non-critical: Job continues even if Alpaca API is temporarily unavailable
- [ ] API calls: Minimize to only stale instruments (not all 500+)
- [ ] Performance: Job completes in <30 seconds for typical 20-50 instruments
- [ ] Code coverage: New methods >80% coverage

---

## Acceptance Tests

```java
// Test 1: Only stale instruments are refreshed
@Test
void testRefreshOnlyStaleAssets() {
    // Setup: 2 instruments with stale metadata (>24h old), 1 fresh
    InstrumentEntity stale1 = createInstrument("AAPL", updatedAt: now - 30h);
    InstrumentEntity stale2 = createInstrument("MSFT", updatedAt: now - 25h);
    InstrumentEntity fresh = createInstrument("GOOGL", updatedAt: now - 1h);

    // Execute
    int refreshed = alpacaService.refreshStaleAssetMetadata();

    // Assert: Only 2 instruments fetched from API
    verify(alpacaRestClient, times(2)).getAsset(any());
    assertEquals(2, refreshed);
}

// Test 2: API failure for one ticker doesn't abort batch
@Test
void testBatchContinuesOnSingleFailure() {
    // Setup: 3 stale instruments, mock API to fail on MSFT
    when(alpacaRestClient.getAsset("AAPL")).thenReturn(assetDto);
    when(alpacaRestClient.getAsset("MSFT")).thenThrow(new ApiException());
    when(alpacaRestClient.getAsset("TSLA")).thenReturn(assetDto);

    // Execute
    int refreshed = alpacaService.refreshStaleAssetMetadata();

    // Assert: AAPL and TSLA refreshed, MSFT skipped
    assertEquals(2, refreshed); // Not 3, not 0, just the successful ones
    assertLogContains("Failed to refresh metadata for MSFT");
}

// Test 3: Timestamp is updated on successful refresh
@Test
void testTimestampUpdatedOnRefresh() {
    // Setup: Stale instrument
    InstrumentEntity instrument = createInstrument("AAPL", updatedAt: now - 30h);
    Instant before = Instant.now();

    // Execute
    alpacaService.refreshStaleAssetMetadata();

    // Assert: Timestamp is recent
    Instant after = Instant.now();
    InstrumentEntity refreshed = instrumentRepository.findByTicker("AAPL");
    assertTrue(refreshed.getAlpacaMetadataUpdatedAt().isAfter(before));
    assertTrue(refreshed.getAlpacaMetadataUpdatedAt().isBefore(after.plusSeconds(1)));
}

// Test 4: Scheduled job runs at 6 AM daily
@Test
void testScheduledJobConfiguration() {
    // Verify cron expression
    Method method = ScheduledJobsService.class.getMethod("refreshAlpacaAssets");
    Scheduled annotation = method.getAnnotation(Scheduled.class);
    assertEquals("0 0 6 * * ?", annotation.cron());
}

// Test 5: Job continues despite API unavailability
@Test
void testJobContinuesOnApiUnavailable() {
    // Setup: Mock API failure
    when(alpacaRestClient.getAsset(any())).thenThrow(new ConnectException());

    // Execute: Should not throw
    assertDoesNotThrow(() -> scheduledJobsService.refreshAlpacaAssets());

    // Assert: Error logged but no exception propagates
    assertLogContains("refreshAlpacaAssets() failed");
    assertLogContains("will retry on next schedule");
}
```

---

## Related Issues

- **Depends on**: ISSUE-014 (Alpaca Assets API Integration) - ✅ COMPLETED
- **Completes**: ISSUE-014 (adds the missing scheduled refresh layer)
- **Related**: ISSUE-004 (ScheduledReports Consolidation - established patterns)
- **Related**: ISSUE-003 (FLEX Reports synchronization - similar scheduling patterns)

---

## Implementation Details

### Repository Query

```java
// InstrumentRepository.java
@Query("SELECT i FROM InstrumentEntity i WHERE i.alpacaMetadataUpdatedAt < :threshold OR i.alpacaMetadataUpdatedAt IS NULL ORDER BY i.alpacaMetadataUpdatedAt ASC NULLS FIRST")
List<InstrumentEntity> findStaleAlpacaAssets(@Param("threshold") Instant threshold);

// Usage:
Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
List<InstrumentEntity> stale = instrumentRepository.findStaleAlpacaAssets(threshold);
```

### AlpacaService Method

```java
// AlpacaService.java (existing service)
@Transactional
public int refreshStaleAssetMetadata() {
    Instant threshold = Instant.now().minus(24, ChronoUnit.HOURS);
    List<InstrumentEntity> staleInstruments = instrumentRepository.findStaleAlpacaAssets(threshold);

    int refreshedCount = 0;
    for (InstrumentEntity instrument : staleInstruments) {
        try {
            AlpacaAssetDto assetDto = getAsset(instrument.getTicker());
            saveAssetMetadata(assetDto, instrument);
            instrumentRepository.save(instrument);
            refreshedCount++;
            log.info("Refreshed Alpaca metadata for ticker: {}", instrument.getTicker());
        } catch (Exception e) {
            log.warn("Failed to refresh Alpaca metadata for ticker {}: {}",
                     instrument.getTicker(), e.getMessage());
            // Continue with next instrument
        }
    }

    return refreshedCount;
}
```

### ScheduledJobsService Job

```java
// ScheduledJobsService.java
@Scheduled(cron = "0 0 6 * * ?") // Daily at 6:00 AM
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
```

### Cron Expression Explanation

- `0` - Second: 0
- `0` - Minute: 0 (top of the hour)
- `6` - Hour: 6 AM
- `*` - Day of Month: Every day
- `*` - Month: Every month
- `?` - Day of Week: No specific day (use day of month instead)

Result: **Every day at 6:00:00 AM**

---

## Notes

### Design Decisions

1. **Daily at 6 AM vs. Every 24 Hours**
   - Daily at 6 AM is more predictable and deterministic
   - Helps with monitoring/alerting ("job should run by 6:15 AM")
   - Users see fresh data when they start their trading day
   - Alternative (fixedDelay) would run at different times each day

2. **Only Stale Instruments vs. All Instruments**
   - Reduces API calls from 500+ daily to 20-50
   - More efficient use of Alpaca API rate limits (200 req/min)
   - Stale instruments are rare after first refresh cycle
   - Newly created instruments get refreshed on first manual access (cache-first)

3. **Non-Critical Error Handling**
   - Alpaca API unavailability shouldn't block other jobs
   - Similar to `MarketDataService.refreshAlpacaMarketData()` pattern
   - Continue with next instrument on individual failure
   - Job runs again tomorrow (acceptable for asset metadata)

4. **No Rethrow vs. Rethrow**
   - FLEX jobs (critical): Rethrow (must succeed or alert)
   - Alpaca jobs (non-critical): No rethrow (retry tomorrow)
   - Asset metadata is informational; trading isn't blocked if refresh fails

### Data Model Fields Updated

When `saveAssetMetadata()` is called, these fields are updated:
```java
alpacaAssetId              // UUID from Alpaca
alpacaTradable             // Pre-trade validation
alpacaMarginable           // Margin eligibility
alpacaShortable            // Short selling permission
alpacaEasyToBorrow         // Current borrow availability (DYNAMIC)
alpacaFractionable         // Fractional shares support
alpacaMaintenanceMarginRequirement  // Position sizing
alpacaExchange             // Trading venue
alpacaStatus               // active/inactive
alpacaAssetClass           // us_equity/crypto
alpacaMetadataUpdatedAt    // ⬅️ Updated to NOW()
```

### API Rate Limiting

- **Alpaca Assets API**: 200 requests/minute, 10 requests/second
- **Expected daily load**: 20-50 refresh requests (well below limit)
- **First cycle**: May see more (all instruments marked stale initially)
- **Steady state**: <50 requests/day after instruments are refreshed

---

## Files to Modify

| File | Change | Priority |
|------|--------|----------|
| `src/main/java/co/grtk/srcprofit/repository/InstrumentRepository.java` | Add `findStaleAlpacaAssets()` query | HIGH |
| `src/main/java/co/grtk/srcprofit/service/AlpacaService.java` | Add `refreshStaleAssetMetadata()` method | HIGH |
| `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java` | Add `refreshAlpacaAssets()` scheduled job | HIGH |
| Tests | Unit & integration tests (see Acceptance Tests) | MEDIUM |

---

## Earnings Data Note

Earnings refresh remains manual CSV import (existing workflow). The earnings data service (`EarningService`) currently only handles CSV parsing. Full API integration with Alpha Vantage would require:
- Creating `AlphaVintageService` with earnings calendar endpoint
- Weekly scheduled refresh job
- Significant new code (~300 lines)

**Recommendation**: Keep existing manual process for now; consider ISSUE-017 "Automate Earnings Calendar Refresh" in future sprint if high value.

---

## Related Code References

- **ScheduledJobsService**: `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java`
- **AlpacaService**: `src/main/java/co/grtk/srcprofit/service/AlpacaService.java` (lines 95-135 for existing methods)
- **InstrumentRepository**: `src/main/java/co/grtk/srcprofit/repository/InstrumentRepository.java`
- **InstrumentEntity**: `src/main/java/co/grtk/srcprofit/entity/InstrumentEntity.java` (lines 78-110 for Alpaca fields)
- **MarketDataService**: `src/main/java/co/grtk/srcprofit/service/MarketDataService.java` (pattern reference)
- **ISSUE-014**: `docs/issues/ISSUE-014-alpaca-assets-api-implementation.md` (context)

---

**Version**: 1.0
**Last Updated**: 2025-11-09
