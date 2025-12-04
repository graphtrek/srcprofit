# ISSUE-046: OpenPositionService.saveCSV Should Delete Positions Not in CSV

**Created**: 2025-12-04
**Status**: OPEN
**Priority**: HIGH
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

`OpenPositionService.saveCSV()` currently retains all existing positions indefinitely, even when they are no longer present in imported IBKR Flex Report CSV data. This causes stale data accumulation and data integrity issues.

**Current Behavior**:
- Import 1: CSV contains positions [A, B, C] → Database: [A, B, C]
- Import 2: CSV contains positions [A, B] → Database: **[A, B, C]** (C not deleted!)

**Impact**:
- **Stale data**: Database shows closed positions that no longer exist in IBKR account
- **Incorrect counts**: UI/API display more open positions than actually exist
- **Data integrity**: Mismatch between "ground truth" (IBKR) and database state
- **Database bloat**: Accumulation of historical position records

**Example Scenario**:
1. Day 1: Import flex report → Database has 50 open positions
2. Day 2: Close 10 positions in IBKR
3. Day 3: Import new flex report → Database still has 50 positions (10 are stale)
4. Day 4: UI shows 50 open positions, but IBKR only has 40

---

## Root Cause

`OpenPositionService.saveCSV()` (lines 119-295) implements upsert-only logic:
- **Creates** new positions if `conid` not found
- **Updates** existing positions if `conid` found
- **Never deletes** positions not present in CSV

```java
// Current implementation (simplified)
for (CSVRecord csvRecord : csvRecords) {
    Long conid = parseLong(csvRecord.get("Conid"));

    // Upsert logic: create or update
    OpenPositionEntity entity = openPositionRepository.findByConid(conid);
    if (entity == null) {
        entity = new OpenPositionEntity();
        entity.setConid(conid);
    }

    // Update all fields
    entity.setAccount(account);
    // ... 60+ field assignments
    openPositionRepository.save(entity);
}

// NO DELETION STEP - positions not in CSV remain in database
```

**Design Rationale**: Originally designed as accumulative snapshot storage, not complete state replacement.

**Comparison**: `OptionService.saveCSV()` includes a `dataFix()` cleanup step that removes closed positions. OpenPositionService lacks equivalent logic.

---

## Approach

Add **account-scoped deletion logic** to `OpenPositionService.saveCSV()` that removes positions from CSV accounts that are not present in the incoming data.

### Implementation Strategy

**Algorithm**:
1. Parse CSV and track processed `conid` values in a Set
2. Track unique `ClientAccountID` values seen in CSV
3. Perform existing upsert logic (create or update positions)
4. After successful CSV processing, query all positions for CSV accounts
5. Delete positions from those accounts where `conid` NOT IN processed set
6. Return "saved/deleted" count format

**Key Design Decisions** (User-Approved):
- ✅ **Account-scoped deletion**: Only delete from accounts present in CSV (preserves other accounts)
- ✅ **Always enabled**: No configuration flag (breaking change but cleaner API)
- ✅ **String return format**: Return "saved/deleted" (e.g., "50/10")

### Changes Required

**1. Add Repository Method**

**File**: `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java`

Add query method:
```java
/**
 * Find all positions for a specific account.
 * Used by saveCSV() deletion logic to scope cleanup to CSV accounts.
 *
 * @param account the client account ID
 * @return list of positions for that account
 */
List<OpenPositionEntity> findByAccount(String account);
```

**2. Update OpenPositionService.saveCSV()**

**File**: `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java`

**Method signature** (line 119):
```java
// OLD:
public int saveCSV(String csv) throws IOException

// NEW:
public String saveCSV(String csv) throws IOException
```

**Implementation**:
```java
@Transactional
public String saveCSV(String csv) throws IOException {
    Set<Long> processedConids = new HashSet<>();
    Set<String> csvAccounts = new HashSet<>();
    int savedCount = 0;

    try (CSVParser csvRecords = parse(csv, /* ... */)) {

        for (CSVRecord csvRecord : csvRecords) {
            try {
                // Extract key fields
                String account = csvRecord.get("ClientAccountID");
                Long conid = parseLong(csvRecord.get("Conid"));

                // Track for deletion logic
                csvAccounts.add(account);
                processedConids.add(conid);

                // EXISTING UPSERT LOGIC (unchanged)
                // ... instrument synchronization
                // ... position upsert
                // ... field mappings

                openPositionRepository.save(entity);
                savedCount++;

            } catch (Exception e) {
                log.error("Error processing CSV record #{}: {}",
                    csvRecord.getRecordNumber(), e.getMessage(), e);
                throw new RuntimeException("Failed to parse record " +
                    csvRecord.getRecordNumber(), e);
            }
        }

        // NEW: DELETE POSITIONS NOT IN CSV (account-scoped)
        int deletedCount = 0;
        if (!csvAccounts.isEmpty()) {
            // Query positions from CSV accounts
            List<OpenPositionEntity> accountPositions = new ArrayList<>();
            for (String account : csvAccounts) {
                accountPositions.addAll(openPositionRepository.findByAccount(account));
            }

            // Filter to positions not processed (closed in IBKR)
            List<OpenPositionEntity> toDelete = accountPositions.stream()
                .filter(entity -> !processedConids.contains(entity.getConid()))
                .toList();

            if (!toDelete.isEmpty()) {
                openPositionRepository.deleteAll(toDelete);
                deletedCount = toDelete.size();
                log.info("Deleted {} closed positions not in CSV from accounts: {}",
                    deletedCount, csvAccounts);
            }
        }

        log.info("OpenPositionService.saveCSV() completed: {} saved, {} deleted",
            savedCount, deletedCount);

        // NEW: Return format "saved/deleted"
        return savedCount + "/" + deletedCount;

    } catch (IOException e) {
        log.error("CSV parsing error: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to parse CSV", e);
    }
}
```

**3. Update Caller: FlexReportsService**

**File**: `src/main/java/co/grtk/srcprofit/service/FlexReportsService.java`

**Method**: `importFlexOpenPositions()` (line 294)

```java
// OLD (line 294):
int records = openPositionService.saveCSV(csvData);
return String.valueOf(records) + "/0";

// NEW:
String result = openPositionService.saveCSV(csvData);
return result;  // Already in "saved/deleted" format
```

**4. Add Tests**

**File**: `src/test/java/co/grtk/srcprofit/service/OpenPositionServiceTest.java`

Add comprehensive test suite (see Acceptance Tests section below).

---

## Success Criteria

### Repository
- [ ] `OpenPositionRepository.findByAccount(String account)` method added
- [ ] Method uses JPA method naming convention (auto-implemented by Spring Data)

### Service Implementation
- [ ] `OpenPositionService.saveCSV()` return type changed to `String`
- [ ] Account tracking implemented (`Set<String> csvAccounts`)
- [ ] Conid tracking implemented (`Set<Long> processedConids`)
- [ ] Deletion logic queries positions by account (`findByAccount()`)
- [ ] Deletion filters by `conid NOT IN processedConids`
- [ ] Return format is "saved/deleted" (e.g., "50/10")
- [ ] Logging includes deletion count and affected accounts

### Caller Updates
- [ ] `FlexReportsService.importFlexOpenPositions()` updated to handle String return
- [ ] Return value format preserved (pass-through "saved/deleted")

### Testing
- [ ] Unit test: Deletion of positions not in CSV
- [ ] Unit test: Account-scoped deletion (preserves other accounts)
- [ ] Unit test: Return format "saved/deleted"
- [ ] Unit test: Empty CSV behavior (no deletion if no accounts)
- [ ] Unit test: Multiple accounts in single CSV
- [ ] Unit test: Zero deletions when all positions still open
- [ ] All existing OpenPositionServiceTest tests pass with new return type
- [ ] All existing FlexReportsServiceTest tests pass

---

## Acceptance Tests

### Test 1: Basic Deletion - Positions Not in CSV Are Deleted

```java
@Test
void saveCSV_shouldDeletePositionsNotInCsv() {
    // Given: Database has 3 positions for account DU12345
    OpenPositionEntity pos1 = buildTestEntity("DU12345", 123L, "SPY");
    OpenPositionEntity pos2 = buildTestEntity("DU12345", 456L, "AAPL");
    OpenPositionEntity pos3 = buildTestEntity("DU12345", 789L, "TSLA");

    when(openPositionRepository.findByConid(123L)).thenReturn(pos1);
    when(openPositionRepository.findByConid(456L)).thenReturn(pos2);
    when(openPositionRepository.findByConid(789L)).thenReturn(null);  // New position
    when(openPositionRepository.findByAccount("DU12345"))
        .thenReturn(List.of(pos1, pos2, pos3));

    // CSV only contains conids 123 and 789 (456 is missing = closed)
    String csv = buildCsv(
        "DU12345", 123L, "SPY",   // Existing, update
        "DU12345", 789L, "TSLA"   // New, insert
    );

    // When
    String result = openPositionService.saveCSV(csv);

    // Then
    assertThat(result).isEqualTo("2/1");  // 2 saved, 1 deleted

    // Verify deletion of position 456 (AAPL)
    ArgumentCaptor<List<OpenPositionEntity>> deleteCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(openPositionRepository).deleteAll(deleteCaptor.capture());

    List<OpenPositionEntity> deleted = deleteCaptor.getValue();
    assertThat(deleted).hasSize(1);
    assertThat(deleted.get(0).getConid()).isEqualTo(456L);
    assertThat(deleted.get(0).getSymbol()).isEqualTo("AAPL");
}
```

### Test 2: Account-Scoped Deletion - Other Accounts Preserved

```java
@Test
void saveCSV_accountScoped_shouldPreserveOtherAccounts() {
    // Given: Database has positions from two different accounts
    OpenPositionEntity du12345_pos1 = buildTestEntity("DU12345", 123L, "SPY");
    OpenPositionEntity du12345_pos2 = buildTestEntity("DU12345", 456L, "AAPL");
    OpenPositionEntity du99999_pos1 = buildTestEntity("DU99999", 789L, "TSLA");
    OpenPositionEntity du99999_pos2 = buildTestEntity("DU99999", 111L, "QQQ");

    when(openPositionRepository.findByConid(123L)).thenReturn(du12345_pos1);
    when(openPositionRepository.findByConid(456L)).thenReturn(null);  // New
    when(openPositionRepository.findByAccount("DU12345"))
        .thenReturn(List.of(du12345_pos1, du12345_pos2));

    // CSV only contains DU12345 account (DU99999 not in CSV)
    String csv = buildCsv(
        "DU12345", 123L, "SPY",   // Update
        "DU12345", 456L, "AAPL"   // Insert
    );

    // When
    String result = openPositionService.saveCSV(csv);

    // Then
    assertThat(result).isEqualTo("2/0");  // 2 saved, 0 deleted

    // DU99999 positions should NOT be touched (account not in CSV)
    verify(openPositionRepository, never()).findByAccount("DU99999");

    // Only DU12345 positions queried for deletion
    verify(openPositionRepository).findByAccount("DU12345");
}
```

### Test 3: Return Format - Saved and Deleted Counts

```java
@Test
void saveCSV_returnFormat_shouldBeSavedSlashDeleted() {
    // Given: 5 existing positions, CSV has 3 (2 updated, 1 new), 2 deleted
    OpenPositionEntity pos1 = buildTestEntity("DU12345", 1L, "SPY");
    OpenPositionEntity pos2 = buildTestEntity("DU12345", 2L, "AAPL");
    OpenPositionEntity pos3 = buildTestEntity("DU12345", 3L, "TSLA");  // Will be deleted
    OpenPositionEntity pos4 = buildTestEntity("DU12345", 4L, "QQQ");   // Will be deleted
    OpenPositionEntity pos5 = buildTestEntity("DU12345", 5L, "MSFT");  // Will be deleted

    when(openPositionRepository.findByConid(1L)).thenReturn(pos1);  // Update
    when(openPositionRepository.findByConid(2L)).thenReturn(pos2);  // Update
    when(openPositionRepository.findByConid(6L)).thenReturn(null);  // Insert
    when(openPositionRepository.findByAccount("DU12345"))
        .thenReturn(List.of(pos1, pos2, pos3, pos4, pos5));

    String csv = buildCsv(
        "DU12345", 1L, "SPY",    // Update
        "DU12345", 2L, "AAPL",   // Update
        "DU12345", 6L, "NVDA"    // Insert
    );

    // When
    String result = openPositionService.saveCSV(csv);

    // Then
    assertThat(result).isEqualTo("3/3");  // 3 saved (2 updates + 1 insert), 3 deleted
}
```

### Test 4: Empty CSV - No Deletions

```java
@Test
void saveCSV_emptyAccounts_shouldNotDeleteAnything() {
    // Given: Database has positions, but CSV is empty (no data rows)
    OpenPositionEntity existingPos = buildTestEntity("DU12345", 123L, "SPY");

    String csv = buildEmptyCsv();  // Header only, no data rows

    // When
    String result = openPositionService.saveCSV(csv);

    // Then
    assertThat(result).isEqualTo("0/0");  // 0 saved, 0 deleted

    // No deletion queries should be made
    verify(openPositionRepository, never()).findByAccount(any());
    verify(openPositionRepository, never()).deleteAll(any());
}
```

### Test 5: Multiple Accounts in Single CSV

```java
@Test
void saveCSV_multipleAccounts_shouldDeleteFromAllCsvAccounts() {
    // Given: CSV contains two accounts (DU11111 and DU22222)
    OpenPositionEntity du11111_pos1 = buildTestEntity("DU11111", 1L, "SPY");
    OpenPositionEntity du11111_pos2 = buildTestEntity("DU11111", 2L, "AAPL");  // Delete
    OpenPositionEntity du22222_pos1 = buildTestEntity("DU22222", 3L, "TSLA");
    OpenPositionEntity du22222_pos2 = buildTestEntity("DU22222", 4L, "QQQ");   // Delete

    when(openPositionRepository.findByConid(1L)).thenReturn(du11111_pos1);
    when(openPositionRepository.findByConid(3L)).thenReturn(du22222_pos1);
    when(openPositionRepository.findByAccount("DU11111"))
        .thenReturn(List.of(du11111_pos1, du11111_pos2));
    when(openPositionRepository.findByAccount("DU22222"))
        .thenReturn(List.of(du22222_pos1, du22222_pos2));

    String csv = buildCsv(
        "DU11111", 1L, "SPY",    // Update
        "DU22222", 3L, "TSLA"    // Update
    );

    // When
    String result = openPositionService.saveCSV(csv);

    // Then
    assertThat(result).isEqualTo("2/2");  // 2 saved, 2 deleted (1 from each account)

    // Both accounts queried for deletion
    verify(openPositionRepository).findByAccount("DU11111");
    verify(openPositionRepository).findByAccount("DU22222");

    // Verify deletions include positions from both accounts
    ArgumentCaptor<List<OpenPositionEntity>> deleteCaptor =
        ArgumentCaptor.forClass(List.class);
    verify(openPositionRepository).deleteAll(deleteCaptor.capture());

    List<OpenPositionEntity> deleted = deleteCaptor.getValue();
    assertThat(deleted).hasSize(2);
    assertThat(deleted).extracting(OpenPositionEntity::getConid)
        .containsExactlyInAnyOrder(2L, 4L);  // AAPL and QQQ
}
```

### Test 6: Integration Test with Real CSV Data

```java
@SpringBootTest
@Transactional
class OpenPositionServiceIntegrationTest {

    @Autowired
    private OpenPositionService openPositionService;

    @Autowired
    private OpenPositionRepository openPositionRepository;

    @Test
    void saveCSV_realData_shouldDeleteClosedPositions() throws IOException {
        // Given: Import initial snapshot with 10 positions
        String csvDay1 = loadTestCsv("open_positions_day1.csv");  // 10 positions
        String result1 = openPositionService.saveCSV(csvDay1);

        assertThat(result1).isEqualTo("10/0");  // 10 saved, 0 deleted
        assertThat(openPositionRepository.findAll()).hasSize(10);

        // When: Import next day snapshot with 7 positions (3 closed)
        String csvDay2 = loadTestCsv("open_positions_day2.csv");  // 7 positions
        String result2 = openPositionService.saveCSV(csvDay2);

        // Then
        assertThat(result2).isEqualTo("7/3");  // 7 saved, 3 deleted
        assertThat(openPositionRepository.findAll()).hasSize(7);

        // Verify database reflects current IBKR state
        List<OpenPositionEntity> remaining = openPositionRepository.findAll();
        assertThat(remaining).extracting(OpenPositionEntity::getConid)
            .doesNotContain(/* conids of 3 closed positions */);
    }
}
```

---

## Breaking Changes

### API Change

**Return Type**:
```java
// OLD:
public int saveCSV(String csv) throws IOException

// NEW:
public String saveCSV(String csv) throws IOException
```

**Return Format**:
- Old: Integer count of saved records (e.g., `50`)
- New: String format "saved/deleted" (e.g., `"50/10"`)
- Parsing: Callers can split on "/" to extract counts

### Behavior Change

**Deletion Logic**:
- Old: Positions retained indefinitely (accumulative)
- New: Positions from CSV accounts not in CSV are deleted (snapshot replacement)

**Account Scoping**:
- Deletion is scoped to accounts present in CSV
- Positions from accounts NOT in CSV are preserved
- Safe for multi-account scenarios

### Migration Required

**FlexReportsService** (minimal change):
```java
// OLD:
int records = openPositionService.saveCSV(csvData);
return String.valueOf(records) + "/0";

// NEW:
String result = openPositionService.saveCSV(csvData);
return result;  // Already correct format
```

**Other Potential Callers**:
- Search codebase for calls to `openPositionService.saveCSV()`
- Update to handle String return instead of int
- Parse "saved/deleted" format if individual counts needed

---

## Related Issues

- **Prerequisite**: ISSUE-043 - OpenPositionService saveCSV Should Synchronize Instruments (CLOSED)
- **Related**: ISSUE-044 - Add getAllOpenOptionDtos() to OpenPositionService (OPEN)
- **Related**: ISSUE-045 - Migrate Controllers to OpenPositionService (OPEN)
- **Pattern Reference**: OptionService.saveCSV() + dataFix() cleanup pattern

---

## Notes

### Similar Pattern: OptionService

`OptionService.saveCSV()` includes a `dataFix()` method that removes closed positions:
```java
// OptionService pattern
public int saveCSV(String csv) {
    // ... parse and save records
    dataFix();  // Cleanup: delete closed positions
    return rowCount;
}
```

OpenPositionService should follow this pattern but with:
- Account-scoped deletion (not global)
- Inline deletion logic (no separate dataFix method needed)
- String return format for visibility

### Transaction Boundary

The `@Transactional` annotation ensures:
- All CSV records saved OR entire import rolled back
- Deletion happens in same transaction as upsert
- Atomic operation: no partial state (some saved, some not deleted)

### Performance Considerations

**Query Efficiency**:
- `findByAccount()` called once per unique account in CSV
- Typically 1-2 accounts per CSV (IBKR Flex Reports usually single-account)
- Deletion uses `deleteAll()` batch operation (efficient for multiple records)

**Memory Usage**:
- `Set<Long> processedConids`: ~8 bytes per position (typically < 1KB for 100 positions)
- `Set<String> csvAccounts`: ~50 bytes per account (typically < 100 bytes)
- `List<OpenPositionEntity>` for deletion: Temporary, garbage collected after transaction

### Edge Cases Handled

1. **Empty CSV**: No accounts tracked → No deletion queries → No changes
2. **All positions still open**: Deletion list empty → No deleteAll() call → Return "N/0"
3. **New account in CSV**: Account not in database → findByAccount() returns empty → No deletions
4. **Multiple accounts**: Loop through all CSV accounts → Query and delete from each

### Logging Strategy

**INFO Level** (production visibility):
- Deletion count and affected accounts
- Example: "Deleted 3 closed positions not in CSV from accounts: [DU12345]"

**DEBUG Level** (development debugging):
- Individual conids deleted (if needed for troubleshooting)

### Future Enhancements

Consider for future issues:
- Add audit trail (SoftDelete pattern with deletedAt timestamp)
- Configurable deletion (flag or property to disable)
- Dry-run mode (report what would be deleted without deleting)
- Deletion history table (track when positions were removed)

---

## Implementation Checklist

- [ ] Read OpenPositionRepository and understand JPA method conventions
- [ ] Add `findByAccount(String account)` to OpenPositionRepository
- [ ] Update OpenPositionService.saveCSV() signature to return String
- [ ] Add `Set<Long> processedConids` tracking in saveCSV()
- [ ] Add `Set<String> csvAccounts` tracking in saveCSV()
- [ ] Add account extraction in CSV parsing loop
- [ ] Add deletion logic after CSV processing
- [ ] Update return statement to "saved/deleted" format
- [ ] Update FlexReportsService.importFlexOpenPositions() to handle String
- [ ] Write test: Basic deletion (positions not in CSV deleted)
- [ ] Write test: Account-scoped deletion (other accounts preserved)
- [ ] Write test: Return format "saved/deleted"
- [ ] Write test: Empty CSV (no deletions)
- [ ] Write test: Multiple accounts in single CSV
- [ ] Write integration test with real CSV data
- [ ] Update existing tests to expect String return
- [ ] Run `./mvnw test` and verify all tests pass
- [ ] Manual testing: Import flex report, verify deletion
- [ ] Update ISSUE-046 status to CLOSED with completion date
