# ISSUE-024: Position-Weighted Portfolio Probability and ROI Calculations

**Created**: 2025-11-11
**Status**: CLOSED
**Completed**: 2025-11-11
**Priority**: HIGH
**Category**: Code Quality / Technical Debt
**Blocking**: Accurate portfolio-level metrics, risk management decisions

---

## Problem

The current portfolio-level probability and ROI calculations in `OptionService.calculatePosition()` use simple averaging across positions, which is mathematically incorrect and produces misleading metrics.

**Current Implementation** (`OptionService.java:294-301`):
```java
if(positionDto.getPositionValue() == 0) {
    positionDto.setProbability(0);
    positionDto.setAnnualizedRoiPercent(0);
} else if(openPositionsSize > 0) {
    if (allpop > 0)
        positionDto.setProbability((int) (allpop / openPositionsSize));

    if (allRoi > 0)
        positionDto.setAnnualizedRoiPercent((int) (allRoi / openPositionsSize));
}
```

**Impact Examples**:

1. **ROI Inflation**: Simple averaging treats all positions equally regardless of capital deployed
   ```
   Position 1: $100 premium, 100% ROI → $100 annual return
   Position 2: $10,000 premium, 10% ROI → $1,000 annual return

   Current calculation: (100 + 10) / 2 = 55% average ROI
   Actual portfolio ROI: $1,100 / $10,100 = 10.89%

   Result: 5x INFLATED metric (55% vs 10.89%)
   ```

2. **Probability Distortion**: Averaging probabilities ignores position size differences
   ```
   Position 1: $10,000 capital at risk, 80% probability
   Position 2: $1,000 capital at risk, 20% probability

   Current calculation: (80 + 20) / 2 = 50% average

   Result: The $10,000 position dominates portfolio risk,
           but simple averaging gives equal weight to both
   ```

---

## Root Cause

**Mathematical Error**: Averaging intensive properties (percentages) without weighting by extensive properties (capital, position value) produces meaningless results.

**Specific Issues**:
1. **No position weighting**: Lines 257-258 accumulate raw values without considering position sizes
2. **Equal treatment fallacy**: $100 position weighted identically to $10,000 position
3. **Probability misunderstanding**: Independent event probabilities cannot be arithmetically averaged
4. **Zero test coverage**: Portfolio aggregation logic is completely untested (only mocked in controller tests)

**Code Location**:
- Method: `OptionService.calculatePosition()` at lines 189-322
- Accumulation: Lines 206-208 (initialization), 257-258 (accumulation)
- Calculation: Lines 294-301 (simple division)
- File: `src/main/java/co/grtk/srcprofit/service/OptionService.java`

---

## Approach

Replace simple averaging with position-weighted calculations following TastyTrade methodology.

### Phase 1: Implement Position-Weighted ROI

**Formula** (capital-weighted):
```java
totalPremiumCollected = sum(position.tradePrice * position.quantity)
totalCapitalAtRisk = sum(position.positionValue * position.quantity)
totalAnnualReturn = sum((position.tradePrice / position.daysBetween) * 365 * position.quantity)

portfolioWeightedROI = (totalAnnualReturn / totalCapitalAtRisk) * 100
```

**Alternative Formula** (buying power weighted, per TastyTrade):
```java
// For each position:
positionBuyingPower = position.buyingPowerEffect
positionROI = position.annualizedRoiPercent
positionWeight = abs(positionBuyingPower) / totalBuyingPower

portfolioWeightedROI = sum(positionROI * positionWeight)
```

### Phase 2: Implement Position-Weighted Probability

**Option 1: Capital-Weighted Average** (simpler, good first approach)
```java
totalCapitalAtRisk = sum(position.positionValue * position.quantity)
weightedProbability = sum(position.probability * position.positionValue * position.quantity)
                      / totalCapitalAtRisk
```

**Option 2: Combined Portfolio Distribution** (more accurate, future enhancement)
```java
// Combine all position Greeks (delta, gamma, theta)
// Recalculate probability from portfolio-level statistics
// Using portfolio standard deviation and mean return
// This matches TastyTrade's beta-weighted delta approach
```

### Phase 3: Add Comprehensive Test Coverage

Create `OptionServiceTest.java` with tests for:
1. Simple two-position weighted ROI (like examples above)
2. Multiple positions with varying capital sizes
3. Edge cases (zero positions, single position, all equal positions)
4. Weighted probability calculations
5. Comparison tests showing difference from simple averaging

**Reference**: Individual calculation tests in `PositionCalculationHelperTest.java` (41 tests)

---

## Success Criteria

- [x] Portfolio ROI calculation uses position weighting by capital at risk or buying power
- [x] Portfolio probability calculation uses position weighting by capital at risk
- [x] Simple averaging logic removed from lines 294-301
- [x] New weighted calculation methods are testable (separated from UI logic)
- [x] Comprehensive unit tests added for `calculatePosition()` aggregation logic
- [x] Test cases demonstrate difference between weighted and unweighted calculations
- [x] All existing tests continue to pass
- [x] Documentation updated with correct formulas and rationale

---

## Acceptance Tests

```java
@Test
public void testWeightedROI_TwoPositionsUnequalCapital() {
    // Position 1: $100 premium, 100% ROI
    PositionDto pos1 = createPosition(100, 100.0, 365);

    // Position 2: $10,000 premium, 10% ROI
    PositionDto pos2 = createPosition(10000, 10.0, 365);

    List<PositionDto> openPositions = List.of(pos1, pos2);
    PositionDto portfolio = new PositionDto();

    optionService.calculatePosition(portfolio, openPositions, List.of());

    // Weighted ROI should be ~10.89%, NOT 55%
    // Calculation: ($100 + $1,000) / ($100 + $10,000) * 100 = 10.89%
    assertThat(portfolio.getAnnualizedRoiPercent())
        .isCloseTo(10.89, within(0.1));

    // Simple average would incorrectly give 55%
    double simpleAverage = (100.0 + 10.0) / 2;
    assertThat(portfolio.getAnnualizedRoiPercent())
        .isNotCloseTo(simpleAverage, within(20.0)); // Should differ significantly
}

@Test
public void testWeightedProbability_TwoPositionsUnequalRisk() {
    // Position 1: $10,000 capital at risk, 80% probability
    PositionDto pos1 = createPosition(10000, 50.0, 365);
    pos1.setProbability(80);
    pos1.setPositionValue(10000);

    // Position 2: $1,000 capital at risk, 20% probability
    PositionDto pos2 = createPosition(1000, 50.0, 365);
    pos2.setProbability(20);
    pos2.setPositionValue(1000);

    List<PositionDto> openPositions = List.of(pos1, pos2);
    PositionDto portfolio = new PositionDto();

    optionService.calculatePosition(portfolio, openPositions, List.of());

    // Weighted probability: (80*10000 + 20*1000) / (10000 + 1000)
    // = 820,000 / 11,000 = 74.5%
    assertThat(portfolio.getProbability())
        .isCloseTo(74.5, within(0.5));

    // Simple average would incorrectly give 50%
    assertThat(portfolio.getProbability())
        .isNotEqualTo(50);
}

@Test
public void testWeightedCalculations_EdgeCaseEqualPositions() {
    // When all positions are equal, weighted should equal simple average
    PositionDto pos1 = createPosition(1000, 50.0, 365);
    pos1.setProbability(70);

    PositionDto pos2 = createPosition(1000, 50.0, 365);
    pos2.setProbability(70);

    List<PositionDto> openPositions = List.of(pos1, pos2);
    PositionDto portfolio = new PositionDto();

    optionService.calculatePosition(portfolio, openPositions, List.of());

    // When positions are equal, weighted average = simple average
    assertThat(portfolio.getAnnualizedRoiPercent()).isEqualTo(50.0);
    assertThat(portfolio.getProbability()).isEqualTo(70);
}
```

---

## Related Issues

- Related: ISSUE-023 (Refactor annualized ROI calculation) - Individual position calculations are correct
- Blocks: Accurate portfolio risk metrics
- Blocks: Proper position sizing decisions based on portfolio heat

---

## Notes

### TastyTrade Methodology References

From `docs/trading/tastytrade-cli-option-analysis.md`:
- **Buying Power Percentage** uses position value weighting (lines 217-220)
- **Never uses simple averaging** for financial metrics

From `docs/trading/tastytrade-cli-feature-gap-analysis.md`:
- Beta-weighted delta calculations (position-weighted, not averaged)
- Delta target warnings for delta-neutral portfolios

From `docs/trading/README.md`:
- Portfolio Heat: Sum of all position risks (not averaged)
- Max 5% portfolio heat per position

### Current Test Coverage Status

**Individual Position Calculations** (✅ Well tested):
- File: `src/test/java/co/grtk/srcprofit/mapper/PositionCalculationHelperTest.java`
- Coverage: 41 comprehensive tests
- Methods: `calculateDaysBetween`, `calculateDaysLeft`, `estimateTradePrice`, `calculateBreakEven`, `calculateAnnualizedRoiPercent`, `calculateProbability`

**Portfolio Aggregation Logic** (❌ Zero tests):
- File: `src/test/java/co/grtk/srcprofit/controller/TradeLogControllerTest.java`
- Status: Only mocked (`doNothing().when(optionService).calculatePosition(...)`)
- Result: Simple averaging bugs are COMPLETELY UNTESTED

### Implementation Considerations

1. **Backward Compatibility**: Portfolio-level calculations will change significantly
   - Document expected changes in metrics
   - Consider adding migration notes for users

2. **Performance**: Weighted calculations require one additional pass through positions
   - Acceptable overhead (<1ms for typical portfolio sizes)
   - Can optimize later if needed

3. **UI Impact**: Portfolio metrics displayed on positions page will show correct (lower) values
   - Previously inflated ROI will decrease to accurate levels
   - Users will see more conservative (realistic) probability estimates

4. **Data Requirements**: Ensure all positions have required fields:
   - `positionValue` (capital at risk)
   - `tradePrice` (premium collected)
   - `quantity` (number of contracts)
   - `daysBetween` (holding period)

### Future Enhancements

1. **Beta-Weighted Portfolio Greeks** (like TastyTrade):
   - Combine position deltas weighted by beta to SPX
   - Calculate portfolio-level gamma, theta, vega

2. **Portfolio Standard Deviation**:
   - Use combined position statistics
   - Calculate portfolio-level probability from combined distribution

3. **Risk-Adjusted Metrics**:
   - Sharpe ratio for portfolio
   - Maximum drawdown tracking
   - Portfolio heat percentage

---

**References**:
- TastyTrade methodology: `docs/trading/tastytrade-*.md`
- Individual calculation tests: `src/test/java/co/grtk/srcprofit/mapper/PositionCalculationHelperTest.java`
- Current implementation: `src/main/java/co/grtk/srcprofit/service/OptionService.java:189-322`
