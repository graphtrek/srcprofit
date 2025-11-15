# ISSUE-030: OptionService saveCSV Should Not Fail on Single CSV Record

**Created**: 2025-11-15
**Status**: OPEN
**Priority**: HIGH
**Category**: Bug Fix / Resilience Enhancement
**Blocking**: CSV import workflow reliability

---

## Problem

The `OptionService.saveCSV()` method uses a **FAIL-FAST** approach where any single CSV record failure aborts the entire import:

- **Current Behavior**: If record #50 fails in a 1000-record CSV, all 1000 records are rolled back (none saved)
- **User Impact**: Manual CSV cleanup and retry required after any data quality issue
- **Data Loss Risk**: No partial success - legitimate records are lost due to one bad record
- **Limited Diagnostics**: Error logs only show CSV size and exception message, not which record failed

**Real-World Scenario**:
```
IBKR FLEX Report with 500 options trades:
- Records 1-49: Valid ✓
- Record 50: Missing 'Conid' field (NumberFormatException) ✗
- Records 51-500: Valid but never processed ✗

Result: Database transaction rolls back, 0 records saved
User sees: "Fail to parse CSV" error
Question: Which record failed? Why? What's the fix?
```

---

## Root Cause

**File**: `src/main/java/co/grtk/srcprofit/service/OptionService.java`

### Current Error Handling (Lines 654-762)

```java
@Transactional
public int saveCSV(String csv) {
    int rowCount = 0;
    // ... initialization ...

    try (CSVParser csvRecords = parse(csv, CSVFormat.Builder.create()...)) {
        for (CSVRecord csvRecord : csvRecords) {
            // Extract CSV fields (lines 665-678)
            String account = csvRecord.get("ClientAccountID");
            // ... 13 more field extractions ...
            Long conid = Long.parseLong(csvRecord.get("Conid"));  // Line 675: Can throw NumberFormatException

            if (AssetClass.OPT.getCode().equals(assetClass) && ...) {
                // Parse numbers and dates
                Double netCash = Double.parseDouble(csvRecord.get("NetCash"));  // Line 694: NumberFormatException
                LocalDate tradeDate = LocalDate.parse(csvRecord.get("TradeDate"));  // Line 712: DateTimeParseException
                Double strike = Double.parseDouble(csvRecord.get("Strike"));  // Line 714: NumberFormatException
                Long underlyingConid = Long.parseLong(csvRecord.get("UnderlyingConid"));  // Line 721: NumberFormatException
                Double fifoPnlRealized = Double.parseDouble(csvRecord.get("FifoPnlRealized"));  // Line 728: NumberFormatException

                // ... create and save OptionEntity ...
                optionRepository.save(optionEntity);
                log.info(csvRecord.toString());
                rowCount++;
            }
        }
        log.info("CSV file parsed in {} sec, records: {}", elapsedSeconds, csvRecords.getRecordNumber());
        return rowCount;
    } catch (Exception e) {
        log.error("Fail to parse CSV size: {}", csv.length(), e);
        throw new RuntimeException("Fail to parse CSV " + e.getMessage(), e);  // Line 762: ABORT EVERYTHING
    }
}
```

### Multiple Parse Failure Points

| Line | Code | Potential Exception |
|------|------|-------------------|
| 675 | `Long.parseLong(csvRecord.get("Conid"))` | **NumberFormatException** |
| 694 | `Double.parseDouble(csvRecord.get("NetCash"))` | **NumberFormatException** |
| 712 | `LocalDate.parse(csvRecord.get("TradeDate"))` | **DateTimeParseException** |
| 713 | `LocalDate.parse(csvRecord.get("Expiry"))` | **DateTimeParseException** |
| 714 | `Double.parseDouble(csvRecord.get("Strike"))` | **NumberFormatException** |
| 721 | `Long.parseLong(csvRecord.get("UnderlyingConid"))` | **NumberFormatException** |
| 728 | `Double.parseDouble(csvRecord.get("FifoPnlRealized"))` | **NumberFormatException** |

### Transaction Context

- `@Transactional` annotation (Line 653) wraps entire method
- Single transaction for entire CSV file
- **Any exception causes transaction rollback** - all saved records are undone
- No savepoint or batch commit strategy

### Error Logging Limitations (Line 761)

```java
log.error("Fail to parse CSV size: {}", csv.length(), e);
```

**What it logs**:
- ✓ CSV file size in bytes
- ✓ Exception class and message

**What it doesn't log**:
- ✗ CSV record number where failure occurred
- ✗ Field name causing the parse error
- ✗ Field value that failed to parse
- ✗ How many records were successfully processed before failure
- ✗ Which records can be safely retried

---

## Impact Assessment

### Current Impact

**Reliability**: ⚠️ LOW
- Production FLEX imports fail when IBKR includes unusual data or format changes
- Data quality issues in 1% of records cause loss of 99% of valid data

**User Experience**: ⚠️ POOR
- No diagnostic information to fix CSV issues
- Manual intervention required to identify and remove bad records
- Entire CSV must be re-imported after fixing (time-consuming)

**Data Consistency**: ✅ GOOD
- Transaction integrity guaranteed (all or nothing)
- No partial/inconsistent data in database

### Affected Workflows

1. **FlexReportsService.processFlex()** (Line 138):
   - Calls `optionService.saveCSV(flexTradesQuery)`
   - Transaction fails → entire FLEX import fails
   - User must manually fix CSV and retry

2. **Manual CSV Uploads** (if any):
   - Any malformed record aborts entire upload
   - Same limitation applies

---

## Approach

### Phase 1: Individual Record Error Handling

**Goal**: Continue processing after single record failures

**Changes to OptionService.java**:

1. **Wrap record processing in try-catch** (around line 664):
   ```java
   for (CSVRecord csvRecord : csvRecords) {
       try {
           // All existing processing logic here (lines 665-752)
       } catch (NumberFormatException e) {
           log.error("CSV Record #{} - NumberFormatException in field parse: {}",
                     csvRecord.getRecordNumber(), e.getMessage());
           // Continue to next record
       } catch (DateTimeParseException e) {
           log.error("CSV Record #{} - DateTimeParseException: {}",
                     csvRecord.getRecordNumber(), e.getMessage());
           // Continue to next record
       } catch (Exception e) {
           log.error("CSV Record #{} - Unexpected error: {}",
                     csvRecord.getRecordNumber(), e.getMessage());
           // Continue to next record
       }
   }
   ```

2. **Collect failed records** (add new field):
   ```java
   List<String> failedRecords = new ArrayList<>();
   List<String> failedErrors = new ArrayList<>();

   // In catch blocks:
   failedRecords.add(csvRecord.toString());
   failedErrors.add(e.getMessage());
   ```

3. **Update outer catch block** (line 760):
   ```java
   } catch (Exception e) {
       // Only catch CSV parsing errors (missing columns, malformed CSV)
       // Individual record errors are handled above
       log.error("CSV parsing configuration error: {}", e.getMessage(), e);
       throw new RuntimeException("CSV parsing failed: " + e.getMessage(), e);
   }
   ```

### Phase 2: Improved Return Type

**Goal**: Report success/failure summary instead of just record count

**Create new class** `CsvImportResult.java`:
```java
public class CsvImportResult {
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private int skippedRecords; // Non-OPT records
    private List<CsvRecordError> errors;

    public static class CsvRecordError {
        int recordNumber;
        String csvRecord;
        String errorMessage;
    }

    // Getters, setters, toString()
}
```

**Update saveCSV signature**:
```java
public CsvImportResult saveCSV(String csv)
```

### Phase 3: Batch Commit Strategy (Optional)

**Goal**: Reduce transaction scope for large files

**Options** (evaluate for performance impact):
1. **Option A**: One transaction per batch (commit every 100 records)
   - Pro: Limits rollback scope
   - Con: Complexity - must handle partial success

2. **Option B**: Keep single transaction (current approach)
   - Pro: Simple, guarantees consistency
   - Con: Large rollback on late failures

3. **Option C**: Transaction per record
   - Pro: Maximum resilience
   - Con: Performance impact, complex error handling

**Recommendation**: Keep Option B initially, monitor for performance issues

### Phase 4: Enhanced Logging

**Destination**:
- ERROR level for failed records (always logged)
- DEBUG level for skipped records (non-OPT)
- INFO level for summary statistics

**Log Format**:
```
CSV Record #50 failed: NumberFormatException parsing 'Conid' field
  Value: "invalid-number"
  Error: For input string: "invalid-number"
  Record: ClientAccountID=U123456,AssetClass=OPT,...

CSV import summary: 500 records, 490 successful, 10 failed, 5 skipped
```

---

## Success Criteria

- [ ] **Resilience**: CSV import continues after single record failure
- [ ] **Diagnostics**: Failed records logged with record number, field name, and error detail
- [ ] **Summary**: Return CsvImportResult with success/failure counts
- [ ] **Backward Compatibility**: FlexReportsService and other callers updated to handle new return type
- [ ] **Logging**: Debug log for each processed record, error log for failures
- [ ] **Database**: Successful records commit even if some records fail
- [ ] **No Regression**: Existing working records still import correctly
- [ ] **Test Coverage**:
  - [ ] Unit test: NumberFormatException handling
  - [ ] Unit test: DateTimeParseException handling
  - [ ] Unit test: Invalid CSV field handling
  - [ ] Integration test: Mixed valid/invalid records
  - [ ] Test: CsvImportResult accuracy

---

## Acceptance Tests

### Test 1: Single Invalid Record in Valid CSV
```
Given: CSV with 10 records, record #5 has invalid 'Conid' field
When: saveCSV() is called
Then:
  - Records 1-4, 6-10 are saved (9 total)
  - Record #5 is logged with error details
  - CsvImportResult.successfulRecords = 9
  - CsvImportResult.failedRecords = 1
  - No transaction rollback occurs
```

### Test 2: Multiple Invalid Records
```
Given: CSV with 100 records, 3 records have invalid data
When: saveCSV() is called
Then:
  - 97 records saved
  - Each failed record logged separately
  - Summary shows 97 successful, 3 failed
```

### Test 3: Invalid Date Format
```
Given: CSV with record having expirationDate = "2025/11/15" (invalid format)
When: saveCSV() is called
Then:
  - Record fails with DateTimeParseException
  - Error logged: "CSV Record #X - DateTimeParseException: Text '2025/11/15' could not be parsed"
  - Other records continue processing
```

### Test 4: Backward Compatibility
```
Given: Existing code calling optionService.saveCSV()
When: Method signature changes to return CsvImportResult
Then:
  - FlexReportsService.processFlex() updated to use new return type
  - csvRecordsCount stored correctly in FlexStatementResponseEntity
  - No breaking changes to public API
```

### Test 5: Empty or Null Fields
```
Given: CSV with record missing optional field
When: saveCSV() is called
Then:
  - If field is required and null, record fails gracefully
  - If field is optional and null, record processed with null value
```

---

## Implementation Checklist

- [ ] Wrap record processing in try-catch blocks (line 664)
- [ ] Collect failed record details (record number, error)
- [ ] Create CsvImportResult class
- [ ] Update saveCSV() return type
- [ ] Update error logging with record numbers and field details
- [ ] Update FlexReportsService to handle CsvImportResult
- [ ] Update NetAssetValueService if it also calls saveCSV
- [ ] Update EarningService if it also calls saveCSV
- [ ] Add unit tests for error scenarios
- [ ] Add integration test for mixed valid/invalid CSV
- [ ] Test database state after partial failure
- [ ] Update transaction handling if batch strategy implemented
- [ ] Verify no performance regression
- [ ] Document in user guide how to handle import failures

---

## Related Issues

- **ISSUE-003**: FLEX Reports Automatic Synchronization (original FLEX import implementation)
- **ISSUE-005**: FLEX Reports Import Monitoring Fields (related to import tracking)

---

## Technical Debt

**Similar Issue in Other CSV Methods**:
- `EarningService.saveCSV()` - Uses same FAIL-FAST pattern
- `NetAssetValueService.saveCSV()` - Uses same FAIL-FAST pattern

**Recommendation**: Apply same fix to all three CSV import methods for consistency

---

## Notes

### CSV Source

IBKR (Interactive Brokers) FLEX Query Reports contain:
- Thousands of records per query
- Mix of asset classes (OPT, STK, CASH, etc.)
- Occasional format variations due to IBKR changes
- Data quality varies (may have missing fields, invalid values)

### Why This Matters

Options traders depend on accurate position data for:
- **Risk Management**: Delta exposure calculations
- **Portfolio Planning**: Position rolling strategies
- **Compliance**: Record keeping for tax reporting

A failed import means **no data update** for the entire reporting period. The ability to salvage valid records from a partially-bad CSV is critical.

### Design Consideration: All-or-Nothing vs Best-Effort

**Current**: All-or-Nothing (FAIL-FAST)
- ✅ Guarantees data consistency
- ❌ Loses valid data on single failure

**Proposed**: Best-Effort with Logging
- ✅ Salvages valid records
- ✅ Identifies problem records for investigation
- ✓ Maintains transaction integrity within what's salvageable
- ⚠️ Must clearly report failed records so user understands data completeness

**User Responsibility**: Review import summary to verify all expected records were imported

---

## Estimated Effort

**Research & Planning**: 1 hour (completed)
**Implementation**: 2-3 hours
**Testing**: 1-2 hours
**Integration & Verification**: 0.5-1 hour

**Total**: 4-7 hours (3-5 if batching is deferred)

---

**Version**: 1.0
**Status**: OPEN - Ready for implementation planning
