# ISSUE-017: Scheduled Earnings Calendar Refresh

**Created**: 2025-11-09 (Session current)
**Status**: CLOSED
**Completed**: 2025-11-09 (Session Current)
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

Currently, earnings data is only refreshed through manual CSV imports via `EarningService.saveCSV()`. This creates a dependency on manual uploads and means earnings data becomes stale over time. There is no automated scheduled refresh of earnings calendar data from an external API source.

This is a gap in the automation strategy - while market data (Alpaca quotes), asset metadata (Alpaca assets), and position data (IBKR FLEX) are all automatically refreshed via scheduled jobs, earnings data remains manual.

---

## Root Cause

The `EarningService` was initially designed only for CSV import. No API integration exists for earnings calendar data, and no scheduled job was added to `ScheduledJobsService` to automate the refresh.

---

## Approach

Implement a daily scheduled job that automatically refreshes earnings calendar data for all instruments in the database. Follow the **ISSUE-016 pattern** (Alpaca assets refresh) for consistency.

### Phase 1: API Source Selection (Research)
- Evaluate available earnings calendar APIs:
  - **Alpha Vantage**: Already integrated in codebase, has earnings calendar endpoint
  - **Alpaca**: Check if earnings calendar API is available
  - **Other sources**: Consider alternatives if needed
- Document API rate limits, response format, and data freshness
- Select preferred source based on availability and coverage

### Phase 2: Create Earnings API Service
- Create `AlphaVintageService.getEarningsCalendar(String symbol)` method (or equivalent)
- Implement REST client call to fetch earnings calendar from selected API
- Add error handling for API failures (don't fail entire batch)
- Parse and normalize response to EarningDTO format

### Phase 3: Add Refresh Orchestrator to EarningService
- Add method: `refreshEarningsDataForAllInstruments()` to `EarningService`
- Fetch all distinct symbols from `InstrumentEntity` table
- For each symbol:
  - Call API service to get latest earnings calendar
  - For each earnings record:
    - Check if earnings data already exists (using `findBySymbolAndReportDateAndFiscalDateEnding()`)
    - Create or update `EarningEntity` record
    - Update corresponding `InstrumentEntity.earningDate` if applicable
  - Log per-symbol success/failure
- Return summary: count of symbols processed, count of new records, count of failures
- Implement per-symbol error handling (API failure for one symbol doesn't abort batch)

### Phase 4: Add Scheduled Job to ScheduledJobsService
- Add method: `refreshEarningsData()`
- Schedule: **Daily at 6:00 AM** (`@Scheduled(cron = "0 0 6 * * ?")`)
- Error handling: **Non-critical pattern** (catch exceptions, log, don't rethrow)
  - Pattern reference: `refreshMarketData()` or `refreshAlpacaAssets()` in ScheduledJobsService
- Delegate to `EarningService.refreshEarningsDataForAllInstruments()`
- Log execution time and result summary

### Phase 5: Testing
- **Unit Tests** (ScheduledJobsServiceTest):
  - Mock EarningService
  - Verify scheduled job calls service
  - Verify exception handling (doesn't rethrow)
  - Verify logging of execution time
- **Unit Tests** (EarningServiceTest):
  - Mock API service
  - Test refresh logic with sample earnings data
  - Test deduplication (no duplicate records)
  - Test error handling (per-symbol failures don't stop batch)
  - Test InstrumentEntity update
- **Integration Tests**:
  - Verify cron expression works correctly
  - Test with real database (in-memory or test container)
  - Verify scheduling happens at expected times
- **Validation**:
  - Verify no existing earnings data is lost
  - Verify API rate limits are respected
  - Verify performance (batch size, execution time)

---

## Success Criteria

- [ ] Earnings calendar API source selected and documented
- [ ] API service created with earnings calendar fetch capability
- [ ] EarningService has `refreshEarningsDataForAllInstruments()` method
- [ ] ScheduledJobsService has `refreshEarningsData()` scheduled job
- [ ] Scheduled job executes daily at 6:00 AM
- [ ] All unit tests passing (service and scheduled job)
- [ ] Integration tests verify scheduling works correctly
- [ ] Error handling: API failures for one symbol don't stop batch
- [ ] Execution logs include timing and result summary
- [ ] No existing earnings data is deleted or corrupted during refresh
- [ ] Code follows SrcProfit conventions (same pattern as ISSUE-016)

---

## Acceptance Tests

```python
def test_issue_017_earnings_refresh_scheduled():
    """Verify scheduled earnings refresh job is registered and executes"""
    # 1. Verify ScheduledJobsService.refreshEarningsData() exists
    # 2. Verify @Scheduled annotation with cron="0 0 6 * * ?"
    # 3. Verify job delegates to EarningService
    # 4. Verify exception handling (non-critical pattern)
    assert earnings_refresh_job_registered
    assert earnings_refresh_executes_daily_6am

def test_issue_017_earnings_data_refresh():
    """Verify earnings data is fetched and stored correctly"""
    # 1. Setup: Insert instrument with symbol in database
    # 2. Call EarningService.refreshEarningsDataForAllInstruments()
    # 3. Verify API was called for that symbol
    # 4. Verify EarningEntity records were created/updated
    # 5. Verify InstrumentEntity.earningDate was updated
    # 6. Verify no duplicate records exist
    assert earnings_data_refreshed
    assert no_duplicates_created
    assert instrument_earning_date_updated

def test_issue_017_error_handling():
    """Verify batch refresh continues on per-symbol API failures"""
    # 1. Setup: Insert 3 instruments with symbols
    # 2. Mock API to fail for second symbol
    # 3. Call EarningService.refreshEarningsDataForAllInstruments()
    # 4. Verify first symbol succeeded
    # 5. Verify second symbol failure was logged
    # 6. Verify third symbol succeeded (batch continued)
    assert symbol_1_refreshed
    assert symbol_2_failure_logged
    assert symbol_3_refreshed
```

---

## Related Issues

- Related: ISSUE-016 (Scheduled Alpaca assets refresh) - Use this as pattern reference
- Related: EarningService documentation and CSV import functionality
- Blocks: None (independent enhancement)

---

## Notes

### Reference Implementation (ISSUE-016)
Review `docs/issues/ISSUE-016-scheduled-alpaca-assets-refresh.md` for detailed pattern on:
- How to structure API service integration
- How to implement refresh orchestrator
- How to add scheduled job with proper error handling
- Testing strategy for scheduled jobs

### API Considerations
- **Alpha Vantage**: Already integrated, check API key availability and rate limits
- **Data freshness**: Earnings calendar typically updated daily, weekly refresh might be sufficient, but daily is safe
- **Coverage**: Verify selected API covers all symbols in database

### Performance Considerations
- All instruments refresh daily might be expensive depending on API rate limits
- Consider paginating/batching large result sets
- Monitor execution time to ensure job completes before market open

### Current Earnings Data
- `EarningEntity` supports: symbol, reportDate, fiscalDateEnding, estimate, currency, name
- `InstrumentEntity` has `earningDate` field that should be updated during refresh
- Deduplication: Use `EarningRepository.findBySymbolAndReportDateAndFiscalDateEnding()`

### Configuration
- Schedule: Daily 6:00 AM (before market open)
- Error handling: Non-critical (log and continue to next schedule)
- Logging: Include execution time and summary of results
