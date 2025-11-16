# ISSUE-031: Add failed/skipped records tracking to FlexStatementResponseEntity

**Created**: 2025-11-16
**Status**: CLOSED
**Completed**: 2025-11-16
**Priority**: MEDIUM
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Implementation Notes

### Phase 1 Completion (Initial Implementation)
All core requirements implemented:
- Entity fields added (`csvFailedRecordsCount`, `csvSkippedRecordsCount`)
- Database schema updated with new columns
- Service logic updated to persist failed/skipped counts
- 9 comprehensive unit tests added
- All 197 tests passing

### Phase 2 Completion (Type Consistency Fix)
Fixed `updated_at` field type inconsistency:
- Changed from `Instant` to `LocalDateTime` for SQL TIMESTAMP consistency
- Added explicit `@Column(name = "updated_at")` annotation
- Updated getter/setter signatures
- All 197 tests pass with corrected type

**Lessons Learned**:
- Java's `LocalDateTime` maps directly to SQL TIMESTAMP (no timezone info)
- `Instant` maps to TIMESTAMP WITH TIMEZONE (unnecessary for audit timestamps)
- Consistency with SQL schema is critical for Hibernate mapping

---

## Problem

The `FlexStatementResponseEntity` is used to track IBKR FLEX API import operations, but currently only persists successful record counts. Failed and skipped records are logged but **not saved to the database**, causing loss of observability data about import quality and error rates.

**Current Behavior**:
- `OptionService.saveCSV()` returns `CsvImportResult` with `failedRecords` and `skippedRecords` counts
- `FlexReportsService` logs these counts (lines 145-146 for TRADES, 207-208 for NAV)
- Only `csvRecordsCount` is persisted to `FlexStatementResponseEntity`
- Failed/skipped data is lost after each job run

**Impact**:
- No historical audit trail of import failures
- Cannot analyze import quality trends
- Difficult to debug recurring import issues
- Monitoring/alerting systems cannot access failure metrics

---

## Root Cause

The `FlexStatementResponseEntity` schema and domain object lack fields to store failed and skipped record counts. The entity only tracks:
- `csvRecordsCount` (successful records only)
- `dataFixRecordsCount` (data correction count for TRADES)
- Missing: failed records, skipped records (non-OPT assets)

Additionally, the `updated_at` field type inconsistency: entity declares `Instant updatedAt` but database schema lacks the column, forcing Hibernate to create it implicitly.

---

## Approach

**Phase 1: Database Schema**
1. Add migration or update `init/01-schema.sql`:
   - `csv_failed_records_count INTEGER` column
   - `csv_skipped_records_count INTEGER` column
   - `updated_at TIMESTAMP` column (if not auto-created by Hibernate)

**Phase 2: Entity Model**
1. Update `FlexStatementResponseEntity`:
   - Add `Integer csvFailedRecordsCount` field with `@Column`
   - Add `Integer csvSkippedRecordsCount` field with `@Column`
   - Verify `updated_at` field type is `LocalDateTime` (not `Instant`) for consistency with SQL TIMESTAMP

**Phase 3: FlexReportsService Updates**
1. Update `importFlexTrades()` method (lines 142-151):
   - Extract `failedRecords` from `CsvImportResult`
   - Extract `skippedRecords` from `CsvImportResult`
   - Set both fields on entity before save

2. Update `importFlexNetAssetValue()` method (lines 204-212):
   - Handle failed/skipped counts if `NetAssetValueService.saveCSV()` is enhanced
   - For now, set to 0 or null as appropriate

**Phase 4: Testing**
1. Add unit tests to `ScheduledJobsServiceTest`:
   - Verify failed/skipped counts are persisted
   - Test with sample TRADES and NAV CSV data
   - Verify counts match `CsvImportResult` values

---

## Success Criteria

- [x] `FlexStatementResponseEntity` has `csvFailedRecordsCount` and `csvSkippedRecordsCount` Integer fields
- [x] Database schema includes columns for failed/skipped counts
- [x] `FlexReportsService.importFlexTrades()` persists failed/skipped counts to entity
- [x] `FlexReportsService.importFlexNetAssetValue()` persists failed/skipped counts to entity
- [x] `updated_at` field type is `LocalDateTime` (not `Instant`) for SQL TIMESTAMP consistency
- [x] `updated_at` field has explicit `@Column(name = "updated_at")` annotation
- [x] Unit tests verify persistence of all three counts
- [x] Existing tests pass with schema changes (197 tests pass)
- [x] Import job logs match persisted database values
- [x] Type mapping validated with full test suite

---

## Acceptance Tests

```java
// Test that failed records are persisted
@Test
void testFlexStatementResponsePersistsFailed() {
    // Given a CSV import with 100 records (90 success, 5 failed, 5 skipped)
    CsvImportResult result = createTestResult(100, 90, 5, 5);

    // When the TRADES import completes
    flexReportsService.importFlexTrades();

    // Then the entity persists all counts
    FlexStatementResponseEntity entity = repository.findByReferenceCode("TEST-REF");
    assertEquals(90, entity.getCsvRecordsCount());
    assertEquals(5, entity.getCsvFailedRecordsCount());
    assertEquals(5, entity.getCsvSkippedRecordsCount());
    assertNotNull(entity.getUpdatedAt());
    assertTrue(entity.getUpdatedAt() instanceof LocalDateTime);
}
```

---

## Related Issues

- Blocks: None
- Blocked by: None
- Related: ISSUE-030 (Resilient CSV import error handling)

---

## Notes

**Implementation Details**:

1. **CsvImportResult structure** (src/main/java/co/grtk/srcprofit/dto/CsvImportResult.java):
   - `totalRecords`: Total records in CSV
   - `successfulRecords`: Records parsed and saved successfully
   - `failedRecords`: Records with parsing/validation errors
   - `skippedRecords`: Valid records but non-OPT assets (e.g., STK)
   - `errors`: List of CsvRecordError with field-level details

2. **Update timestamp behavior**:
   - Current: `@UpdateTimestamp(source = SourceType.DB)` with `Instant` type
   - Should be: `LocalDateTime` type for SQL TIMESTAMP consistency
   - Hibernate will auto-update on entity.save()

3. **NAV import tracking**:
   - `NetAssetValueService.saveCSV()` currently returns only int count
   - Does not provide failed/skipped breakdown
   - Consider enhancing later or setting NAV counts to 0 initially

4. **Test pattern**: See `ScheduledJobsServiceTest.java` for mocking FlexReportsService and repository patterns

---

**Version**: 1.0
**Last Updated**: 2025-11-16
