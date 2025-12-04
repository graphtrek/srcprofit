# ISSUE-044: Add getAllOpenOptionDtos() method to OpenPositionService

**Created**: 2025-12-04 (Session N/A)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

`OpenPositionService` currently only provides CSV import functionality via `saveCSV()`. There is no way to query and retrieve open option positions as DTOs with calculated financial metrics (ROI, probability, break-even).

Users cannot:
- Get all open options from IBKR Flex Report data through the service layer
- Filter open options by underlying ticker
- Access calculated financial metrics (annualized ROI, probability of profit) for IBKR positions
- Leverage IBKR position data in the same way as `OptionService` data

This creates an inconsistency: `OptionService.getAllOpenOptionDtos()` exists for trading history, but there's no equivalent for current IBKR positions.

---

## Root Cause

`OpenPositionService` was designed primarily for CSV import/persistence. Query and retrieval functionality was not implemented. The repository layer has the necessary query methods (`findAllOptions()`, `findOptionsByUnderlyingTicker()`), but they are unused by the service layer.

---

## Approach

Add two public query methods to `OpenPositionService` that:

1. **Retrieve open options** using existing repository queries:
   - `getAllOpenOptionDtos()` - Returns all open option positions
   - `getOpenOptionsByTickerDto(String ticker)` - Returns options for a specific ticker

2. **Convert `OpenPositionEntity` to `PositionDto`** with field mapping:
   - Use `underlyingSymbol` for ticker (not the full option symbol)
   - Map strike to `positionValue`
   - Map cost basis to `tradePrice`
   - Map mark price to both `marketPrice` and `marketValue`
   - Map report date to `tradeDate`
   - Convert PUT/CALL ("P"/"C") to OptionType enum

3. **Calculate derived metrics** using existing `PositionMapper`:
   - Call `PositionMapper.calculateAndSetAnnualizedRoi(dto)` for each position
   - This calculates: daysBetween, daysLeft, breakEven, annualizedRoiPercent, probability

**Pattern**: Follow the same structure as `OptionService.getAllOpenOptionDtos()` and `OptionService.getOpenOptionsByTicker()` for API consistency.

**Implementation Details**:
- Manual field mapping (entity names don't align with DTO names)
- No new dependencies needed (use existing services/mappers)
- Reuse repository queries already in `OpenPositionRepository`
- No changes to constructor (no new dependencies)

---

## Success Criteria

- [ ] `getAllOpenOptionDtos()` returns `List<PositionDto>` of all OPT positions from database
- [ ] `getOpenOptionsByTickerDto(String ticker)` returns options for specified underlying ticker only
- [ ] DTOs include calculated fields:
  - `annualizedRoiPercent` - Calculated via `PositionMapper`
  - `probability` - Calculated via `PositionMapper`
  - `daysLeft` - Calculated via `PositionMapper`
  - `breakEven` - Calculated via `PositionMapper`
- [ ] Field mapping is accurate:
  - Ticker uses `underlyingSymbol` (not option symbol)
  - Strike is treated as dollars (not cents)
  - Report date maps to trade date
  - PUT/CALL strings convert to OptionType enum
- [ ] Methods handle edge cases:
  - Empty repository returns empty list (not null)
  - Null fields (costBasisPrice, markPrice) handled gracefully
  - Expired options (daysLeft < 0) processed correctly
- [ ] Existing `saveCSV()` functionality unchanged
- [ ] Unit tests pass (mock-based)
- [ ] Integration tests pass (real CSV data)
- [ ] No breaking changes to API

---

## Acceptance Tests

### Unit Test: getAllOpenOptionDtos

```java
@Test
void getAllOpenOptionDtos_shouldReturnCalculatedDtos() {
    // Given: Mock repository returns 2 open options
    OpenPositionEntity spy = buildTestEntity("SPY", 600.0, "P", LocalDate.now().plusDays(30));
    OpenPositionEntity aapl = buildTestEntity("AAPL", 200.0, "C", LocalDate.now().plusDays(45));
    when(openPositionRepository.findAllOptions()).thenReturn(List.of(spy, aapl));

    // When
    List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getTicker()).isEqualTo("SPY");
    assertThat(result.get(0).getAnnualizedRoiPercent()).isNotNull();  // Calculated
    assertThat(result.get(0).getDaysLeft()).isGreaterThan(0);  // Calculated
}
```

### Unit Test: getOpenOptionsByTickerDto

```java
@Test
void getOpenOptionsByTickerDto_shouldFilterByUnderlying() {
    // Given: Mock repository returns only SPY options
    OpenPositionEntity spy1 = buildTestEntity("SPY", 600.0, "P", LocalDate.now().plusDays(30));
    OpenPositionEntity spy2 = buildTestEntity("SPY", 605.0, "C", LocalDate.now().plusDays(30));
    when(openPositionRepository.findOptionsByUnderlyingTicker("SPY"))
        .thenReturn(List.of(spy1, spy2));

    // When
    List<PositionDto> result = openPositionService.getOpenOptionsByTickerDto("SPY");

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(dto -> dto.getTicker().equals("SPY"));
}
```

### Integration Test: With Real CSV Data

```java
@SpringBootTest
@Transactional
class OpenPositionServiceIntegrationTest {
    @Autowired
    private OpenPositionService openPositionService;

    @Autowired
    private OpenPositionRepository openPositionRepository;

    @Test
    void getAllOpenOptionDtos_withRealData_shouldCalculateMetrics() throws IOException {
        // Given: Load IBKR Flex Report CSV with open positions
        String csv = loadTestCsv("open_positions_sample.csv");
        openPositionService.saveCSV(csv);

        // When
        List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

        // Then
        assertThat(result).isNotEmpty();
        result.forEach(dto -> {
            assertThat(dto.getTicker()).isNotNull();
            assertThat(dto.getExpirationDate()).isNotNull();
            assertThat(dto.getAnnualizedRoiPercent()).isNotNull();
            assertThat(dto.getDaysLeft()).isNotNull();
        });
    }
}
```

### Edge Case Test: Empty Repository

```java
@Test
void getAllOpenOptionDtos_emptyRepository_shouldReturnEmptyList() {
    when(openPositionRepository.findAllOptions()).thenReturn(Collections.emptyList());

    List<PositionDto> result = openPositionService.getAllOpenOptionDtos();

    assertThat(result).isEmpty();
    assertThat(result).isNotNull();  // Not null, but empty
}
```

---

## Related Issues

- Related: ISSUE-039 - IBKR Flex Open Positions Import (created infrastructure)
- Related: ISSUE-042 - Refactor OptionSnapshotService to use OpenPositionEntity
- Related: ISSUE-043 - OpenPositionService saveCSV Should Synchronize Instruments
- Reference: OptionService.getAllOpenOptionDtos() (pattern to follow)

---

## Notes

### Implementation Files

1. **Service**: `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java`
   - Add `getAllOpenOptionDtos()` public method
   - Add `getOpenOptionsByTickerDto(String ticker)` public method
   - Add `convertToPositionDtos(List<OpenPositionEntity>)` private helper
   - Add `convertToPositionDto(OpenPositionEntity)` private helper
   - Add imports: `PositionMapper`, `OptionType`, `OptionStatus`, `AssetClass`

2. **Tests**: Create or extend `src/test/java/co/grtk/srcprofit/service/OpenPositionServiceTest.java`
   - Unit tests with mocked repository
   - Integration tests with real CSV data
   - Edge case tests (empty results, null fields, type conversions)

### Field Mapping Reference

| OpenPositionEntity | PositionDto | Notes |
|---|---|---|
| `underlyingSymbol` | `ticker` | Use underlying, not option symbol |
| `quantity` | `quantity` | Direct mapping |
| `reportDate` | `tradeDate` | Snapshot date used for calculations |
| `expirationDate` | `expirationDate` | Direct mapping |
| `strike` | `positionValue` | Already in dollars, no conversion |
| `costBasisPrice` | `tradePrice` | Acquisition cost per unit |
| `markPrice` | `marketPrice`, `marketValue` | Current market price |
| `fifoPnlUnrealized` | `unRealizedProfitOrLoss` | IBKR FIFO P&L |
| `putCall` | `type` | "P" → PUT, "C" → CALL |
| N/A | `status` | Always OPEN |
| N/A | `assetClass` | Always OPT |

### Calculated Fields (via PositionMapper)

- `daysBetween` - Days from reportDate to expiration
- `daysLeft` - Days from now to expiration
- `breakEven` - Strike price ± trade price (depends on PUT/CALL)
- `annualizedRoiPercent` - Annualized return on investment
- `probability` - Probability of profit at expiration (if marketValue available)

### Design Decisions

**Question**: Should we calculate ROI/probability or return raw data?
**Answer**: Calculate (user selected this option) - Matches OptionService pattern

**Question**: Why no `startDate` parameter?
**Answer**: OpenPositionEntity represents snapshots, not transactional history. `reportDate` can be filtered later if needed.

**Question**: Why manual field mapping instead of ObjectMapper?
**Answer**: Field names don't align (e.g., `underlyingSymbol` → `ticker`). Manual mapping is clearer and more maintainable.

---

## Implementation Checklist

- [ ] Read PositionMapper to understand calculation logic
- [ ] Add new imports to OpenPositionService
- [ ] Implement `getAllOpenOptionDtos()` method
- [ ] Implement `getOpenOptionsByTickerDto(String ticker)` method
- [ ] Implement `convertToPositionDtos()` helper
- [ ] Implement `convertToPositionDto()` helper with full field mapping
- [ ] Write unit test for `getAllOpenOptionDtos()`
- [ ] Write unit test for `getOpenOptionsByTickerDto()`
- [ ] Write unit test for empty repository edge case
- [ ] Write unit test for null fields edge case
- [ ] Write integration test with real CSV data
- [ ] Run `./mvnw test` and verify all pass
- [ ] Run `./mvnw clean compile` for compile verification
- [ ] Create PR with description from this issue
- [ ] Request code review
- [ ] Address review feedback
- [ ] Merge to master
