# ISSUE-026: Position Calculator Manual Recalculation

**Created**: 2025-11-11
**Status**: CLOSED
**Completed**: 2025-11-11
**Priority**: HIGH
**Category**: Feature / Enhancement
**Related**: ISSUE-024 (Position-Weighted Portfolio), ISSUE-022 (Position Calculator DataTables)

---

## Problem

The Position Calculator currently **aggregates metrics from database positions** whenever a user clicks the Calculate button for a ticker that has existing open positions. This prevents **what-if analysis** - users cannot explore hypothetical position parameters without affecting or considering existing positions.

**Current Behavior**:
```java
// PositionController.java:98
optionService.calculatePosition(positionDto, openPositions, closedPositions);
```

When a user enters a ticker with existing database positions:
1. The calculator loads ALL open positions for that ticker
2. All metrics are AGGREGATED from database positions
3. Form-entered values are OVERRIDDEN by database data
4. User cannot recalculate based on hypothetical inputs

**Impact**:
- Cannot do what-if analysis (e.g., "What if I entered this position at $X trade price?")
- Cannot preview calculations for new position ideas
- Cannot manually adjust parameters and see immediate results
- Calculator is locked to existing portfolio state

**Example Scenario**:
```
User has:
- SPY position 1: $100 trade, $110 market, 30 DTE
- SPY position 2: $150 trade, $140 market, 45 DTE

User wants to analyze:
"What if I enter a new SPY trade at $120 with $115 market value?"

Current behavior:
- Calculator shows aggregated values of positions 1 & 2
- Ignores user's hypothetical $120/$115 inputs

Desired behavior:
- Recalculate using ONLY $120 trade / $115 market inputs
- Show what-if ROI/probability for that single position
```

---

## Root Cause

The `fillPositionForm()` method in PositionController (line 86-98) unconditionally loads all database positions and passes them to `optionService.calculatePosition()`. There is **no way to perform manual-only calculations** on form inputs.

**Code Flow** (PositionController.java:66-73):
```java
@PostMapping("/calculatePosition")
public String calculatePosition(PositionDto positionDto, Model model) {
    PositionDto dto = getMarketValue(positionDto);  // Fetch live Alpaca data
    fillPositionForm(dto, model);  // Loads database positions and aggregates
    return "position-form_jte";
}

// Line 86-98
private void fillPositionForm(PositionDto positionDto, Model model) {
    List<PositionDto> closedPositions = optionRepository.findClosedByTicker(ticker);
    List<PositionDto> openPositions = optionRepository.findByTicker(ticker);  // ← Always loads
    optionService.calculatePosition(positionDto, openPositions, closedPositions);  // ← Aggregates
}
```

**Problem**: No parameter to control whether to aggregate from database or use form-only values.

---

## Approach

Implement **manual recalculation mode** that calculates metrics using **ONLY form-input values**, while still fetching live market data from Alpaca.

### High-Level Flow

```
User submits form with values:
  Trade Date: 2025-11-11
  Expiration Date: 2025-12-19
  Trade Price: $120
  Position Value: $10,000
  Market Value: $9,800

POST /calculatePosition (with calculation mode = MANUAL)
  ↓
PositionController.calculatePosition()
  ↓
getMarketValue(Alpaca)  ← Fetch live market data
  ↓
calculateSinglePosition()  ← NEW: Manual-only calculation
  ↓
- Call PositionMapper.calculateAndSetAnnualizedRoi()
- Calculate days, ROI, probability using form values
- Clear aggregated fields (Realized P&L = 0, etc)
  ↓
Render template with calculated values
```

### Phase 1: Add Calculation Mode Parameter

Add `calculationMode` parameter to track manual vs aggregate mode:
```java
// PositionDto.java
private String calculationMode = "MANUAL";  // "MANUAL" or "AGGREGATE"
```

### Phase 2: Create calculateSinglePosition Helper

Create new method in OptionService to handle single-position calculations:
```java
/**
 * Calculate metrics for a single position using ONLY form input values.
 * Does NOT load or aggregate from database positions.
 *
 * @param positionDto position with form input values
 * @return positionDto with calculated metrics
 */
public PositionDto calculateSinglePosition(PositionDto positionDto) {
    // 1. Validate required fields
    if (positionDto.getTradeDate() == null || positionDto.getExpirationDate() == null) {
        return positionDto;
    }

    // 2. Calculate individual position metrics
    calculateAndSetAnnualizedRoi(positionDto);  // Calculates ROI, days, probability

    // 3. Clear aggregated fields (require database positions)
    positionDto.setRealizedProfitOrLoss(0.0);
    positionDto.setCollectedPremium(positionDto.getTradePrice() != null ?
        positionDto.getTradePrice() : 0.0);

    // 4. Set market price and P&L
    if (positionDto.getMarketValue() != null && positionDto.getPositionValue() != null) {
        double marketPrice = positionDto.getMarketValue() - positionDto.getPositionValue();
        positionDto.setMarketPrice(marketPrice);
        positionDto.setCoveredPositionValue(marketPrice);
    }

    return positionDto;
}
```

### Phase 3: Modify calculatePosition Endpoint

Update PositionController to support manual mode:
```java
@PostMapping("/calculatePosition")
public String calculatePosition(PositionDto positionDto, Model model) {
    // Fetch live market data from Alpaca
    PositionDto dto = getMarketValue(positionDto);

    // Manual recalculation: use ONLY form values
    // Do NOT load database positions
    optionService.calculateSinglePosition(dto);

    // Bind to model
    model.addAttribute("positionDto", dto);
    return "position-form_jte";
}
```

### Phase 4: Remove Database Loading from fillPositionForm

Simplify `fillPositionForm()` to skip database queries in manual mode:
```java
private void fillPositionForm(PositionDto positionDto, Model model) {
    // REMOVED: Loading from database
    // REMOVED: optionRepository.findClosedByTicker()
    // REMOVED: optionRepository.findByTicker()
    // REMOVED: optionService.calculatePosition() with database positions

    // Now handled in calculateSinglePosition() above
    model.addAttribute("positionDto", positionDto);
}
```

---

## Success Criteria

- [x] Manual recalculation uses ONLY form-input values
- [x] No database position loading when calculating
- [x] Live Alpaca market data still fetched on every calculation
- [x] Aggregated metrics (Realized P&L, Market Price from database) set to zero/empty
- [x] All calculated fields update based on form inputs only
- [x] Trade Date, Expiration Date, Trade Price, Position Value, Market Value all editable
- [x] Days Left, Days Between, ROI %, P.O.P % all recalculate on form changes
- [x] Non-breaking change: existing aggregate mode preserved if needed in future
- [x] Comprehensive test coverage for manual calculation
- [x] All existing tests continue to pass

---

## Acceptance Tests

```java
@Test
@DisplayName("Manual Recalculation: Uses only form values, ignores database")
void testManualRecalculation_FormValuesOnly() {
    // Given: User has existing positions in database
    // (SPY position: $100 trade, 30 DTE)

    // When: User submits form with different values
    PositionDto formInput = new PositionDto();
    formInput.setTicker("SPY");
    formInput.setTradeDate(LocalDate.now());
    formInput.setExpirationDate(LocalDate.now().plusDays(45));
    formInput.setTradePrice(120.0);
    formInput.setPositionValue(10000.0);
    formInput.setMarketValue(9800.0);

    // When: Calculate using manual mode
    PositionDto result = optionService.calculateSinglePosition(formInput);

    // Then: Calculation uses form values, NOT database positions
    assertThat(result.getAnnualizedRoiPercent())
        .as("ROI should be calculated from form inputs")
        .isNotNull();

    assertThat(result.getRealizedProfitOrLoss())
        .as("Realized P&L should be zero (no database positions)")
        .isEqualTo(0.0);
}

@Test
@DisplayName("Manual Recalculation: Fetches live market data")
void testManualRecalculation_FetchesLiveData() {
    // Given: User submits form
    PositionDto input = createPositionWithValues(120.0, 10000.0);

    // When: getMarketValue is called before calculateSinglePosition
    PositionDto withMarketData = controller.getMarketValue(input);
    PositionDto calculated = optionService.calculateSinglePosition(withMarketData);

    // Then: Market Value should be from Alpaca, not form
    assertThat(calculated.getMarketValue())
        .as("Market value should come from Alpaca API")
        .isNotNull();
}

@Test
@DisplayName("Manual Recalculation: Days and ROI calculated from form dates")
void testManualRecalculation_DaysAndROI() {
    PositionDto input = new PositionDto();
    input.setTradeDate(LocalDate.of(2025, 11, 11));
    input.setExpirationDate(LocalDate.of(2025, 12, 26));
    input.setTradePrice(100.0);
    input.setPositionValue(10000.0);
    input.setMarketValue(10100.0);

    PositionDto result = optionService.calculateSinglePosition(input);

    // daysBetween should be 45 (Nov 11 to Dec 26)
    assertThat(result.getDaysBetween())
        .as("Days between should be calculated from form dates")
        .isEqualTo(45);

    // ROI should be calculated
    assertThat(result.getAnnualizedRoiPercent())
        .as("ROI should be calculated and not null")
        .isNotNull();
}

@Test
@DisplayName("Manual Recalculation: No database queries executed")
void testManualRecalculation_NoDatabaseAccess() {
    // Given: optionRepository is mocked
    verify(optionRepository, never()).findByTicker(anyString());
    verify(optionRepository, never()).findClosedByTicker(anyString());

    // When: calculateSinglePosition is called
    optionService.calculateSinglePosition(positionDto);

    // Then: No repository methods were called
    verify(optionRepository, never()).findByTicker(anyString());
    verify(optionRepository, never()).findClosedByTicker(anyString());
}

@Test
@DisplayName("Manual Recalculation: Aggregated fields are zero")
void testManualRecalculation_AggregatedFieldsZero() {
    PositionDto input = createPositionWithValues(120.0, 10000.0);
    PositionDto result = optionService.calculateSinglePosition(input);

    // Fields that require database aggregation should be zero
    assertThat(result.getRealizedProfitOrLoss())
        .as("Realized P&L should be zero (no closed positions)")
        .isZero();

    assertThat(result.getCallObligationValue())
        .as("CALL obligation should be zero (not aggregating)")
        .isZero();
}
```

---

## Implementation Details

### Modified Files

1. **PositionController.java** (lines 66-73, 86-98)
   - Update `calculatePosition()` POST endpoint
   - Simplify `fillPositionForm()` to skip database queries
   - Add call to `calculateSinglePosition()`

2. **OptionService.java**
   - Add `calculateSinglePosition()` helper method
   - No changes to existing `calculatePosition()` (keep for backward compatibility)

3. **position-form_jte.jte** (optional UI enhancement)
   - Add hidden field `calculationMode="MANUAL"` if needed
   - Current form behavior already submits to POST endpoint

### No Changes Needed
- **PositionDto.java** - No new fields required (form submission handles it)
- **PositionMapper.java** - Existing `calculateAndSetAnnualizedRoi()` works as-is
- **PositionCalculationHelper.java** - No changes, reuse existing methods

---

## Backward Compatibility

This is a **non-breaking change**:
- Existing `calculatePosition()` method in OptionService remains unchanged
- If future code needs aggregate mode, it can still call original method with database positions
- Current UI behavior is preserved: form calculates on submit

---

## Related Issues

- **ISSUE-024**: Position-Weighted Portfolio Calculations (aggregation logic)
- **ISSUE-022**: Position Calculator DataTables Enhancement (recent completion)
- **ISSUE-023**: Refactor calculateAndSetAnnualizedRoi (uses this method)

---

## Notes

### Why This Approach?

1. **What-if Analysis**: Users can explore position scenarios without database interference
2. **Live Market Data**: Still fetches current prices from Alpaca for accuracy
3. **Single Position Focus**: Calculates ROI, probability, days for entered position only
4. **Zero Aggregation**: Clear that no database positions are included
5. **Simple Implementation**: Minimal code changes, reuses existing calculation logic

### Data Flow Comparison

**Before (Aggregate Mode)**:
```
Form Submit → Load Database Positions → Aggregate & Calculate → Display
```

**After (Manual Mode)**:
```
Form Submit → Fetch Alpaca Market Data → Calculate Single Position → Display
```

### User Experience

**What Users Can Now Do**:
1. Enter hypothetical trade parameters (date, price, size)
2. Click Calculate
3. See projected ROI, probability, break-even
4. Adjust parameters and recalculate instantly
5. No database interference in calculations

---

**Implementation Location**:
- Controller: `src/main/java/co/grtk/srcprofit/controller/PositionController.java`
- Service: `src/main/java/co/grtk/srcprofit/service/OptionService.java`
- Tests: `src/test/java/co/grtk/srcprofit/service/OptionServiceTest.java`
