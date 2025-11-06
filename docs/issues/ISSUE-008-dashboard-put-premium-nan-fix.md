# ISSUE-008: Dashboard Put Premium Displaying NaN Values

**Created**: 2025-11-06 (Session N/A)
**Status**: OPEN
**Priority**: HIGH
**Category**: Bug
**Blocking**: User confidence in dashboard data accuracy

---

## Problem

The dashboard page displays "NaN" for put premium values in both:
1. The collected premium field shown in position summaries
2. The ApexCharts visualization for daily premium tracking

This breaks the user experience and makes financial data unreliable.

---

## Root Cause

**Primary Root Cause**: Null `tradePrice` values in OptionEntity

- `OptionService.java:164` calculates premium without null checks:
  ```java
  BigDecimal.valueOf(option.getTradePrice() * abs(option.getQuantity()))
  ```
- When `tradePrice` is null, Java auto-unboxing produces `NaN`
- This NaN propagates through BigDecimal → CSV string → JavaScript → rendered as "NaN"

**Contributing Factors**:

1. **No null validation** in premium calculation flow
2. **BigDecimal.toString()** (MapperUtils.java:113) converts NaN to "NaN" string
3. **Division by zero** in percentage calculations (OptionService.java:286, 292):
   ```java
   double marketVsPositionsPercentage = ((marketValue / positionValue) * 100) - 100;
   ```
4. **Unsafe template rendering** (dashboard_jte.jte:220) allows invalid values:
   ```javascript
   const dailyPremium = [$unsafe{premiumsCsv}];
   ```

---

## Approach

### Phase 1: Add Null Guards
1. Add null check in `OptionService.getDailyPremium()` before calculating premium
2. Skip options with null tradePrice or log warning
3. Add null/zero checks before division operations

### Phase 2: Validate Data Flow
1. Add validation in `MapperUtils.getValuesCsv()` to filter NaN/Infinity
2. Add defensive parsing in `ChartDataDto.setDailyPremium()`
3. Ensure CSV contains only valid numeric strings

### Phase 3: Frontend Safety
1. Validate chart data before rendering in template
2. Consider replacing `$unsafe` with proper escaping
3. Add JavaScript-side validation for NaN values

### Phase 4: Database Investigation
1. Query database for NULL tradePrice values
2. Determine why options lack pricing data
3. Consider adding NOT NULL constraint or default value

---

## Success Criteria

- [ ] No "NaN" values displayed on dashboard page
- [ ] Null `tradePrice` values handled gracefully (skip or show 0)
- [ ] CSV output validated to contain only valid numbers
- [ ] Division by zero prevented in percentage calculations
- [ ] Dashboard loads correctly with:
  - [ ] Empty options table
  - [ ] Partial data (some null tradePrices)
  - [ ] Zero position values
- [ ] Unit tests cover null/edge cases:
  - [ ] Test `getDailyPremium()` with null tradePrice
  - [ ] Test `getValuesCsv()` with NaN BigDecimal
  - [ ] Test percentage calculations with zero denominators
- [ ] Integration test verifies dashboard renders without NaN

---

## Acceptance Tests

```java
@Test
void testGetDailyPremium_withNullTradePrice() {
    // Given: Option with null tradePrice
    OptionEntity option = new OptionEntity();
    option.setTradePrice(null);
    option.setQuantity(-1);
    option.setTradeDate(LocalDate.now());

    when(optionRepository.findAll()).thenReturn(List.of(option));

    // When: Calculate daily premium
    Map<LocalDate, BigDecimal> result = optionService.getDailyPremium(chartDataDto);

    // Then: Should skip or use 0, not produce NaN
    assertThat(result.values()).allMatch(v -> !v.toString().equals("NaN"));
}

@Test
void testGetValuesCsv_withValidBigDecimals() {
    // Given: Map with valid BigDecimal values
    Map<LocalDate, BigDecimal> data = Map.of(
        LocalDate.now(), new BigDecimal("100.50")
    );

    // When: Convert to CSV
    String csv = MapperUtils.getValuesCsv(data);

    // Then: Should not contain "NaN" or "Infinity"
    assertThat(csv).doesNotContain("NaN", "Infinity");
}

@Test
void testDashboard_withNullPremiumData() {
    // Given: Database with options having null tradePrice
    // When: Load /dashboard/1D endpoint
    // Then: Page renders successfully without "NaN" text
    mockMvc.perform(get("/dashboard/1D"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("NaN"))));
}
```

---

## Related Issues

- Related: ISSUE-001 (Position display improvements)
- Related: ISSUE-003 (FLEX import - data source for tradePrices)

---

## Notes

### Affected Files
- `OptionService.java:164` - Premium calculation
- `OptionService.java:286,292` - Division operations
- `MapperUtils.java:113` - CSV conversion
- `ChartDataDto.java:84` - Data transformation
- `dashboard_jte.jte:181,220` - Display layer
- `PositionDto.java:255` - Data model

### Investigation Results
Full investigation completed by Plan agent - identified 5 critical code locations where NaN can be introduced:
1. Null tradePrice unboxing (CRITICAL)
2. BigDecimal.toString() on NaN (HIGH)
3. Array indexing on empty CSV (MEDIUM)
4. Unsafe template rendering (HIGH)
5. Null collectedPremium reference (MEDIUM)

### Database Query to Run
```sql
SELECT COUNT(*), COUNT(tradePrice), COUNT(*) - COUNT(tradePrice) AS null_count
FROM OPTION;

SELECT * FROM OPTION WHERE tradePrice IS NULL LIMIT 10;
```

---

## Implementation Checklist

- [ ] Add null checks in OptionService.getDailyPremium()
- [ ] Add zero-division guards in percentage calculations
- [ ] Add NaN/Infinity filtering in MapperUtils.getValuesCsv()
- [ ] Add validation in ChartDataDto.setDailyPremium()
- [ ] Write unit tests for null/edge cases
- [ ] Write integration test for dashboard rendering
- [ ] Query database for null tradePrice occurrences
- [ ] Fix or document why tradePrices can be null
- [ ] Verify dashboard displays correctly after fixes
- [ ] Consider adding database constraints
