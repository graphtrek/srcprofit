# ISSUE-045: Migrate Controllers to OpenPositionService.getAllOpenOptionDtos()

**Created**: 2025-12-04
**Status**: OPEN
**Priority**: HIGH
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

Three controllers (TradeLogController, AlpacaRestController, InstrumentController) currently use `optionService.getAllOpenOptionDtos()` to retrieve open option positions. This pulls data from trading history (OptionEntity) instead of live IBKR snapshot data (OpenPositionEntity).

**Issues with current approach**:
- Trading history may be stale if not synchronized with broker
- IBKR Flex Reports provide authoritative "ground truth" data
- Controllers use inconsistent data sources (some use IBKR, others use history)
- ISSUE-044 added `OpenPositionService.getAllOpenOptionDtos()` but controllers weren't migrated
- OpenPositionService method lacks `LocalDate` parameter for date filtering

**Impact**:
- TradeLogController (line 59): Uses `optionService.getAllOpenOptionDtos(positionDto.getPositionsFromDate())`
- AlpacaRestController (line 42): Uses `optionService.getAllOpenOptionDtos(null)` to get symbols for quote retrieval
- InstrumentController (line 33): Uses `optionService.getAllOpenOptionDtos(null)` for P&L aggregation

---

## Root Cause

ISSUE-044 implemented `OpenPositionService.getAllOpenOptionDtos()` (line 461) to provide access to IBKR snapshot data as PositionDTOs with calculated financial metrics. However:

1. The new method has no `LocalDate` parameter (unlike OptionService version)
2. Controllers were not migrated to use the new service
3. No repository query method exists for date-filtered IBKR snapshots
4. Tests mock OptionService instead of OpenPositionService

---

## Approach

Migrate all three controllers to use IBKR snapshot data (OpenPositionService) while preserving date filtering capability.

### Step 1: Add Date Filtering to OpenPositionRepository

**File**: `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java`

Add query method:
```java
@Query("SELECT op FROM OpenPositionEntity op WHERE op.assetClass = 'OPT' " +
       "AND op.reportDate >= :startDate ORDER BY op.reportDate DESC")
List<OpenPositionEntity> findAllOptionsByDate(@Param("startDate") LocalDate startDate);
```

### Step 2: Update OpenPositionService.getAllOpenOptionDtos()

**File**: `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java` (line 461)

Change method signature to accept optional LocalDate parameter:
```java
public List<PositionDto> getAllOpenOptionDtos(LocalDate startDate) {
    List<OpenPositionEntity> openOptions = (startDate != null)
        ? openPositionRepository.findAllOptionsByDate(startDate)
        : openPositionRepository.findAllOptions();
    return convertToPositionDtos(openOptions);
}
```

**Behavior**:
- `startDate == null`: Returns all open options (current behavior)
- `startDate != null`: Returns options where reportDate >= startDate

### Step 3: Migrate TradeLogController

**File**: `src/main/java/co/grtk/srcprofit/controller/TradeLogController.java`

**Add dependency** (line 31-32):
```java
private final OptionService optionService;
private final NetAssetValueService netAssetValueService;
private final OpenPositionService openPositionService;  // ADD
```

**Update constructor**:
```java
public TradeLogController(
    OptionService optionService,
    NetAssetValueService netAssetValueService,
    OpenPositionService openPositionService  // ADD
) {
    this.optionService = optionService;
    this.netAssetValueService = netAssetValueService;
    this.openPositionService = openPositionService;  // ADD
}
```

**Update getAllOpenOptionDtos call** (line 59):
```java
// CHANGE FROM:
List<PositionDto> openOptions = optionService.getAllOpenOptionDtos(
    positionDto.getPositionsFromDate()
);

// CHANGE TO:
List<PositionDto> openOptions = openPositionService.getAllOpenOptionDtos(
    positionDto.getPositionsFromDate()
);
```

### Step 4: Migrate AlpacaRestController

**File**: `src/main/java/co/grtk/srcprofit/controller/AlpacaRestController.java`

**Add dependency**:
```java
private final OpenPositionService openPositionService;  // ADD
```

**Update constructor** to inject OpenPositionService.

**Update getAllOpenOptionDtos call** (line 42):
```java
// CHANGE FROM:
List<PositionDto> openOptions = optionService.getAllOpenOptionDtos(null);

// CHANGE TO:
List<PositionDto> openOptions = openPositionService.getAllOpenOptionDtos(null);
```

**CRITICAL**: Verify OpenPositionService sets the `code` field. AlpacaRestController uses:
```java
String symbols = openOptions.stream()
    .map(dto -> dto.getCode().replaceAll("\\s",""))
    .collect(Collectors.joining(","));
```

If `code` field is not set, add mapping in `OpenPositionService.convertToPositionDto()`.

### Step 5: Migrate InstrumentController

**File**: `src/main/java/co/grtk/srcprofit/controller/InstrumentController.java`

**Add dependency**:
```java
private final OpenPositionService openPositionService;  // ADD
```

**Update constructor** to inject OpenPositionService.

**Update getAllOpenOptionDtos call** (line 33):
```java
// CHANGE FROM:
List<PositionDto> openOptions = optionService.getAllOpenOptionDtos(null);

// CHANGE TO:
List<PositionDto> openOptions = openPositionService.getAllOpenOptionDtos(null);
```

### Step 6: Update TradeLogControllerTest

**File**: `src/test/java/co/grtk/srcprofit/controller/TradeLogControllerTest.java`

**Add mock**:
```java
@Mock
private OpenPositionService openPositionService;  // ADD
```

**Update controller instantiation** to inject OpenPositionService mock.

**Update all verify/when statements** (lines 62, 74, 86, 109):
```java
// CHANGE FROM:
verify(optionService).getAllOpenOptionDtos(null);
when(optionService.getAllOpenOptionDtos(null)).thenReturn(mockOpenPositions);
when(optionService.getAllOpenOptionDtos(fromDate)).thenReturn(mockOpenPositions);

// CHANGE TO:
verify(openPositionService).getAllOpenOptionDtos(null);
when(openPositionService.getAllOpenOptionDtos(null)).thenReturn(mockOpenPositions);
when(openPositionService.getAllOpenOptionDtos(fromDate)).thenReturn(mockOpenPositions);
```

### Step 7: Add Date Filtering Tests

**File**: `src/test/java/co/grtk/srcprofit/service/OpenPositionServiceTest.java`

Add tests for new LocalDate parameter:
```java
@Test
void getAllOpenOptionDtos_withStartDate_shouldFilterByDate() {
    // Given: Repository has positions from different dates
    LocalDate filterDate = LocalDate.now().minusDays(7);
    OpenPositionEntity recent = buildTestEntity("SPY", 600.0, "P",
        LocalDate.now(), LocalDate.now().plusDays(30));
    OpenPositionEntity old = buildTestEntity("AAPL", 200.0, "C",
        LocalDate.now().minusDays(10), LocalDate.now().plusDays(45));

    when(openPositionRepository.findAllOptionsByDate(filterDate))
        .thenReturn(List.of(recent));

    // When
    List<PositionDto> result = openPositionService.getAllOpenOptionDtos(filterDate);

    // Then
    verify(openPositionRepository).findAllOptionsByDate(filterDate);
    verify(openPositionRepository, never()).findAllOptions();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTicker()).isEqualTo("SPY");
}

@Test
void getAllOpenOptionDtos_withNullDate_shouldReturnAll() {
    // Given
    List<OpenPositionEntity> allOptions = List.of(
        buildTestEntity("SPY", 600.0, "P", LocalDate.now(), LocalDate.now().plusDays(30)),
        buildTestEntity("AAPL", 200.0, "C", LocalDate.now(), LocalDate.now().plusDays(45))
    );
    when(openPositionRepository.findAllOptions()).thenReturn(allOptions);

    // When
    List<PositionDto> result = openPositionService.getAllOpenOptionDtos(null);

    // Then
    verify(openPositionRepository).findAllOptions();
    verify(openPositionRepository, never()).findAllOptionsByDate(any());
    assertThat(result).hasSize(2);
}
```

### Step 8: Verify Code Field Mapping

**Investigation**: Check if `OpenPositionService.convertToPositionDto()` sets the `code` field.

**File**: `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java` (lines 519-555)

If `code` field is NOT set:
1. Add `code` field mapping in `convertToPositionDto()`
2. Map from OpenPositionEntity's `symbol` field or construct from components
3. Add test to verify code field is populated

Example mapping:
```java
dto.setCode(entity.getSymbol());  // Use option symbol directly
```

### Step 9: Run Full Test Suite

```bash
./mvnw clean test
```

Verify:
- TradeLogControllerTest passes (all mocks updated)
- OpenPositionServiceTest passes (new date filtering tests)
- AlpacaRestController tests pass (code field works)
- InstrumentController tests pass
- No regressions in other tests

### Step 10: Manual Verification

Start application and test:
1. TradeLogController with date filtering (use positionsFromDate parameter)
2. AlpacaRestController fetches quotes (verify code field works for symbol construction)
3. InstrumentController aggregates P&L by ticker

---

## Success Criteria

- [ ] OpenPositionRepository.findAllOptionsByDate() query method implemented
- [ ] OpenPositionService.getAllOpenOptionDtos(LocalDate) method signature updated
- [ ] Date filtering logic works (null = all, date = filtered by reportDate)
- [ ] TradeLogController uses openPositionService (constructor updated, line 59 changed)
- [ ] AlpacaRestController uses openPositionService (constructor updated, line 42 changed)
- [ ] InstrumentController uses openPositionService (constructor updated, line 33 changed)
- [ ] TradeLogControllerTest mocks updated to use openPositionService
- [ ] OpenPositionServiceTest includes date filtering tests
- [ ] Code field is populated in PositionDto (verify for AlpacaRestController)
- [ ] All unit tests pass (`./mvnw test`)
- [ ] Manual testing confirms all three controllers work correctly

---

## Acceptance Tests

### Test 1: OpenPositionService Date Filtering

```java
@Test
void getAllOpenOptionDtos_withStartDate_shouldFilterByReportDate() {
    // Given: IBKR snapshots from different dates
    LocalDate cutoffDate = LocalDate.of(2025, 12, 1);
    OpenPositionEntity recentSnapshot = buildTestEntity(
        "SPY", 600.0, "P", LocalDate.of(2025, 12, 3), LocalDate.of(2025, 12, 31)
    );
    when(openPositionRepository.findAllOptionsByDate(cutoffDate))
        .thenReturn(List.of(recentSnapshot));

    // When
    List<PositionDto> result = openPositionService.getAllOpenOptionDtos(cutoffDate);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTicker()).isEqualTo("SPY");
    verify(openPositionRepository).findAllOptionsByDate(cutoffDate);
    verify(openPositionRepository, never()).findAllOptions();
}
```

### Test 2: TradeLogController Uses OpenPositionService

```java
@Test
void fillTradeLogPage_shouldUseOpenPositionService() {
    // Given
    PositionDto positionDto = new PositionDto();
    LocalDate fromDate = LocalDate.of(2025, 11, 1);
    positionDto.setPositionsFromDate(fromDate);

    List<PositionDto> mockOpenPositions = List.of(
        createMockPositionDto("SPY", 600.0)
    );
    when(openPositionService.getAllOpenOptionDtos(fromDate))
        .thenReturn(mockOpenPositions);

    Model model = mock(Model.class);

    // When
    controller.fillTradeLogPage(positionDto, model);

    // Then
    verify(openPositionService).getAllOpenOptionDtos(fromDate);
    verify(model).addAttribute("openOptions", mockOpenPositions);
}
```

### Test 3: AlpacaRestController Code Field

```java
@Test
void getLatestQuotes_shouldUseCodeFieldForSymbols() {
    // Given
    PositionDto position1 = new PositionDto();
    position1.setCode("SPY 251231P00600000");  // Option symbol

    PositionDto position2 = new PositionDto();
    position2.setCode("AAPL 251231C00200000");

    when(openPositionService.getAllOpenOptionDtos(null))
        .thenReturn(List.of(position1, position2));

    when(alpacaService.getOptionsLatestQuotes(anyString()))
        .thenReturn(ResponseEntity.ok("quote data"));

    // When
    ResponseEntity<String> result = alpacaRestController.getLatestQuotes();

    // Then
    verify(alpacaService).getOptionsLatestQuotes("SPY251231P00600000,AAPL251231C00200000");
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
}
```

### Test 4: Integration Test with Real Data

```java
@SpringBootTest
@Transactional
class ControllerMigrationIntegrationTest {
    @Autowired
    private OpenPositionService openPositionService;

    @Autowired
    private TradeLogController tradeLogController;

    @Test
    void tradeLogController_shouldUseIBKRSnapshotData() throws IOException {
        // Given: Load IBKR Flex Report CSV
        String csv = loadTestCsv("open_positions_sample.csv");
        openPositionService.saveCSV(csv);

        PositionDto positionDto = new PositionDto();
        positionDto.setPositionsFromDate(LocalDate.now().minusDays(7));
        Model model = new ExtendedModelMap();

        // When
        String viewName = tradeLogController.fillTradeLogPage(positionDto, model);

        // Then
        assertThat(model.containsAttribute("openOptions")).isTrue();
        List<PositionDto> openOptions = (List<PositionDto>) model.getAttribute("openOptions");
        assertThat(openOptions).isNotEmpty();

        // Verify data comes from IBKR (has reportDate mapped to tradeDate)
        openOptions.forEach(dto -> {
            assertThat(dto.getTradeDate()).isNotNull();
            assertThat(dto.getTicker()).isNotNull();
        });
    }
}
```

---

## Related Issues

- **Prerequisite**: ISSUE-044 - Add getAllOpenOptionDtos() method to OpenPositionService (COMPLETED)
- **Related**: ISSUE-042 - Refactor OptionSnapshotService to use OpenPositionEntity
- **Related**: ISSUE-043 - OpenPositionService saveCSV Should Synchronize Instruments
- **Blocks**: Future deprecation of OptionService.getAllOpenOptionDtos()

---

## Notes

### Critical Files

1. **Repository**:
   - `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java`
   - Add: `findAllOptionsByDate(LocalDate startDate)`

2. **Service**:
   - `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java` (line 461)
   - Modify: `getAllOpenOptionDtos()` → `getAllOpenOptionDtos(LocalDate startDate)`

3. **Controllers**:
   - `src/main/java/co/grtk/srcprofit/controller/TradeLogController.java` (line 59)
   - `src/main/java/co/grtk/srcprofit/controller/AlpacaRestController.java` (line 42)
   - `src/main/java/co/grtk/srcprofit/controller/InstrumentController.java` (line 33)

4. **Tests**:
   - `src/test/java/co/grtk/srcprofit/controller/TradeLogControllerTest.java`
   - `src/test/java/co/grtk/srcprofit/service/OpenPositionServiceTest.java`

### Data Semantics Differences

| Aspect | OptionService (Trading History) | OpenPositionService (IBKR Snapshot) |
|--------|--------------------------------|-------------------------------------|
| Data Source | OptionEntity (transaction records) | OpenPositionEntity (IBKR Flex Report) |
| Date Field | `tradeDate` (when position opened) | `reportDate` (snapshot date) |
| Filtering | By transaction date | By report/snapshot date |
| Accuracy | May be stale if not synchronized | Authoritative broker data |
| Use Case | Historical analysis | Current position state |

### Risk Assessment

**HIGH RISK**:
1. **Code field missing**: AlpacaRestController relies on `dto.getCode()` to build option symbols
   - Mitigation: Verify OpenPositionService.convertToPositionDto() sets code field
   - If missing: Add mapping from entity.getSymbol()

**MEDIUM RISK**:
2. **Date semantics change**: reportDate (snapshot) vs tradeDate (transaction)
   - Mitigation: Document the difference, ensure filtering logic is clear
   - TradeLogController may need UI label change ("Positions from date" → "Report date filter")

3. **Test coverage gaps**: Controllers may not have comprehensive test coverage
   - Mitigation: Add integration tests with real IBKR CSV data

**LOW RISK**:
4. **Dependency injection**: Adding OpenPositionService to three controllers
   - Spring Boot handles automatically, low risk
   - Tests need mock updates

### Code Field Mapping

If `code` field is not set in OpenPositionService.convertToPositionDto():

```java
// In convertToPositionDto() method (line 519-555)
// ADD this mapping:
dto.setCode(entity.getSymbol());  // Option symbol like "SPY 251231P00600000"
```

Verify with test:
```java
@Test
void convertToPositionDto_shouldSetCodeField() {
    OpenPositionEntity entity = buildTestEntity(
        "SPY", 600.0, "P", LocalDate.now(), LocalDate.now().plusDays(30)
    );
    entity.setSymbol("SPY 251231P00600000");

    PositionDto result = openPositionService.convertToPositionDto(entity);

    assertThat(result.getCode()).isEqualTo("SPY 251231P00600000");
}
```

### Future Deprecation Path

After this migration:
1. Monitor usage of `OptionService.getAllOpenOptionDtos()` in logs
2. After 1-2 release cycles with no issues, mark OptionService method as `@Deprecated`
3. Eventually remove OptionService.getAllOpenOptionDtos() entirely

---

## Implementation Checklist

- [ ] Read OpenPositionRepository and understand existing query methods
- [ ] Add `findAllOptionsByDate()` query method to OpenPositionRepository
- [ ] Update OpenPositionService.getAllOpenOptionDtos() signature to accept LocalDate
- [ ] Implement conditional logic (null → findAllOptions, date → findAllOptionsByDate)
- [ ] Verify code field is set in convertToPositionDto() (add if missing)
- [ ] Update TradeLogController (dependency + constructor + line 59)
- [ ] Update AlpacaRestController (dependency + constructor + line 42)
- [ ] Update InstrumentController (dependency + constructor + line 33)
- [ ] Update TradeLogControllerTest (mock + verify/when statements)
- [ ] Add date filtering unit tests to OpenPositionServiceTest
- [ ] Run `./mvnw clean test` and verify all tests pass
- [ ] Manual testing: Test all three controllers in running application
- [ ] Update ISSUE-045 status to CLOSED with completion date
