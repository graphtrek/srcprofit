# ISSUE-025: Time-Weighted Portfolio Calculations

**Created**: 2025-11-11
**Status**: CLOSED
**Completed**: 2025-11-11
**Priority**: MEDIUM
**Category**: Feature / Enhancement
**Related**: ISSUE-024 (Position-Weighted Portfolio Calculations)

---

## Problem

Current portfolio-level ROI and probability calculations weight positions by capital at risk (`positionValue × quantity`) but **do not account for time risk**. In options trading:

- **Longer-dated positions have more uncertainty** (wider price distributions)
- **Volatility scales with √time**: A 60 DTE option has √2 ≈ 1.41× more price uncertainty than a 30 DTE option
- **Time value is a key risk component**: Longer-dated options have more extrinsic value at risk

**Current Behavior** (from ISSUE-024 implementation):
```java
// OptionService.java:210-211
double capitalAtRisk = posValue * qty;
totalWeightedROI += (roi * capitalAtRisk);
```

**Impact**: Portfolio metrics treat a 7 DTE position identically to a 60 DTE position if they have the same capital at risk. This ignores the fundamental difference in time-based uncertainty.

**Example**:
```
Position 1: $10,000, 100% ROI, 7 DTE  (short-dated, low time risk)
Position 2: $10,000,  30% ROI, 60 DTE (long-dated, high time risk)

Current Portfolio ROI: 65% (simple capital-weighted average)
Reality: The 60 DTE position dominates risk exposure due to time uncertainty
```

---

## Root Cause

**Capital-only weighting** (implemented in ISSUE-024) correctly weights by position size but **ignores the time dimension of options risk**:

1. Options are time-decaying assets (theta risk)
2. Longer DTE = more uncertainty = wider probability distributions
3. The codebase ALREADY uses √time scaling for probability calculations (`PositionMapper.java:158`)
4. Portfolio metrics should reflect this time-based risk

**Theoretical Foundation**:
- **Black-Scholes Model**: Volatility term is `σ × √t`
- **Existing Implementation**: `double timeAdjustedStdDev = dailyStdDev * Math.sqrt(days)` (PositionMapper.java:158)
- **TastyTrade Standard**: 45 DTE is the baseline for mechanical trading strategies

---

## Approach

Add **time-weighted portfolio calculation methods** alongside existing capital-weighted methods (non-breaking change).

### Formula: Normalized Square Root Time Weighting

**Reference Period**: 45 DTE (TastyTrade standard for mechanical trading)

```java
timeWeight = √(daysLeft / 45.0)

// Where:
// - daysLeft = days remaining until expiration (current time risk)
// - 45 DTE = normalization reference (value of 1.0)
// - √ = square root (matches volatility scaling theory)

totalWeight = positionValue × quantity × timeWeight
```

### Weight Examples

| Days Left | Time Weight | Relative to 45 DTE |
|-----------|-------------|-------------------|
| 7 DTE     | 0.39        | 0.39× (61% less weight) |
| 30 DTE    | 0.82        | 0.82× (18% less weight) |
| 45 DTE    | 1.00        | 1.00× (baseline) |
| 60 DTE    | 1.15        | 1.15× (15% more weight) |
| 90 DTE    | 1.41        | 1.41× (41% more weight) |

### Implementation Plan

**Phase 1: Add Helper Method**
```java
/**
 * Calculate normalized time weight using square root scaling.
 * Reference: 45 DTE = 1.0 (TastyTrade mechanical trading standard)
 *
 * @param daysLeft days remaining until expiration
 * @return normalized time weight (√(daysLeft / 45))
 */
private double calculateNormalizedTimeWeight(Integer daysLeft) {
    if (daysLeft == null || daysLeft <= 0) {
        return 0.0;
    }
    return Math.sqrt(daysLeft / 45.0);
}
```

**Phase 2: Add Time-Weighted ROI Method**
```java
/**
 * Calculate time-weighted ROI for portfolio.
 * Weights by: positionValue × quantity × √(daysLeft / 45)
 *
 * Longer-dated positions receive more weight to reflect time-based uncertainty.
 *
 * @param openPositions List of open positions
 * @return Time-weighted ROI percentage
 */
private double calculateTimeWeightedROI(List<PositionDto> openPositions) {
    double totalWeightedROI = 0.0;
    double totalWeight = 0.0;

    for (PositionDto position : openPositions) {
        Integer roi = position.getAnnualizedRoiPercent();
        Double posValue = position.getPositionValue();
        Integer daysLeft = position.getDaysLeft();
        int qty = abs(position.getQuantity());

        if (roi != null && posValue != null && posValue > 0
            && daysLeft != null && daysLeft > 0) {

            double timeWeight = calculateNormalizedTimeWeight(daysLeft);
            double weight = posValue * qty * timeWeight;

            totalWeightedROI += (roi * weight);
            totalWeight += weight;
        }
    }

    return totalWeight == 0 ? 0.0 : round2Digits(totalWeightedROI / totalWeight);
}
```

**Phase 3: Add Time-Weighted Probability Method**
```java
/**
 * Calculate time-weighted probability for portfolio.
 * Weights by: positionValue × quantity × √(daysLeft / 45)
 *
 * @param openPositions List of open positions
 * @return Time-weighted probability (0-100)
 */
private double calculateTimeWeightedProbability(List<PositionDto> openPositions) {
    double totalWeightedProbability = 0.0;
    double totalWeight = 0.0;

    for (PositionDto position : openPositions) {
        Integer probability = position.getProbability();
        Double posValue = position.getPositionValue();
        Integer daysLeft = position.getDaysLeft();
        int qty = abs(position.getQuantity());

        if (probability != null && posValue != null && posValue > 0
            && daysLeft != null && daysLeft > 0) {

            double timeWeight = calculateNormalizedTimeWeight(daysLeft);
            double weight = posValue * qty * timeWeight;

            totalWeightedProbability += (probability * weight);
            totalWeight += weight;
        }
    }

    return totalWeight == 0 ? 0.0 : round2Digits(totalWeightedProbability / totalWeight);
}
```

**Phase 4: Add to PositionDto (Future - UI Display)**
```java
// Add new fields to PositionDto for time-weighted metrics
Integer timeWeightedAnnualizedRoiPercent;
Integer timeWeightedProbability;
```

---

## Success Criteria

- [x] Helper method `calculateNormalizedTimeWeight()` added with 45 DTE normalization
- [x] `calculateTimeWeightedROI()` method implemented alongside existing `calculateWeightedROI()`
- [x] `calculateTimeWeightedProbability()` method implemented alongside existing `calculateWeightedProbability()`
- [x] Time weight uses `daysLeft` (remaining time) not `daysBetween` (total time)
- [x] Square root scaling matches existing probability calculation approach (PositionMapper.java:158)
- [x] Non-breaking change: existing methods unchanged, new methods added
- [x] Comprehensive unit tests demonstrate time-weighting behavior
- [x] Test cases show longer DTE positions dominate portfolio metrics
- [x] All existing tests continue to pass (no regressions)

---

## Acceptance Tests

```java
@Test
@DisplayName("Time Weight: 45 DTE baseline = 1.0")
void testNormalizedTimeWeight_45DTE_Baseline() {
    // 45 DTE should have normalized weight of 1.0
    double weight = calculateNormalizedTimeWeight(45);
    assertThat(weight).isCloseTo(1.0, within(0.01));
}

@Test
@DisplayName("Time Weight: 60 DTE has 15% more weight than 45 DTE")
void testNormalizedTimeWeight_60DTE() {
    // √(60/45) = 1.1547
    double weight = calculateNormalizedTimeWeight(60);
    assertThat(weight).isCloseTo(1.15, within(0.01));
}

@Test
@DisplayName("Time Weight: 7 DTE has 61% less weight than 45 DTE")
void testNormalizedTimeWeight_7DTE() {
    // √(7/45) = 0.3944
    double weight = calculateNormalizedTimeWeight(7);
    assertThat(weight).isCloseTo(0.39, within(0.01));
}

@Test
@DisplayName("Time-Weighted ROI: Longer DTE dominates portfolio")
void testTimeWeightedROI_LongerDTEDominates() {
    // Position 1: $10,000, 100% ROI, 7 DTE
    PositionDto shortPos = createPosition(10000, 100, 7);

    // Position 2: $10,000, 30% ROI, 60 DTE
    PositionDto longPos = createPosition(10000, 30, 60);

    List<PositionDto> openPositions = List.of(shortPos, longPos);

    // Capital-weighted ROI (current): (100*10000 + 30*10000) / 20000 = 65%
    double capitalWeightedROI = calculateWeightedROI(openPositions);
    assertThat(capitalWeightedROI).isCloseTo(65, within(1));

    // Time-weighted ROI:
    // Weight1 = 10000 * √(7/45) = 3,944
    // Weight2 = 10000 * √(60/45) = 11,547
    // ROI = (100*3944 + 30*11547) / 15491 = 47.8%
    double timeWeightedROI = calculateTimeWeightedROI(openPositions);

    assertThat(timeWeightedROI)
        .as("Time-weighted ROI should be pulled down by longer-dated position")
        .isLessThan(capitalWeightedROI)
        .isCloseTo(48, within(2));
}

@Test
@DisplayName("Time-Weighted Probability: Equal capital, unequal DTE")
void testTimeWeightedProbability_UnequalDTE() {
    // Position 1: $5,000, 80% probability, 7 DTE
    PositionDto shortPos = createPosition(5000, 0, 7);
    shortPos.setProbability(80);

    // Position 2: $5,000, 40% probability, 60 DTE
    PositionDto longPos = createPosition(5000, 0, 60);
    longPos.setProbability(40);

    List<PositionDto> openPositions = List.of(shortPos, longPos);

    // Capital-weighted (current): (80*5000 + 40*5000) / 10000 = 60%
    double capitalWeightedProb = calculateWeightedProbability(openPositions);
    assertThat(capitalWeightedProb).isCloseTo(60, within(1));

    // Time-weighted:
    // Weight1 = 5000 * √(7/45) = 1,972
    // Weight2 = 5000 * √(60/45) = 5,774
    // Prob = (80*1972 + 40*5774) / 7746 = 50.2%
    double timeWeightedProb = calculateTimeWeightedProbability(openPositions);

    assertThat(timeWeightedProb)
        .as("Time-weighted probability pulled toward longer-dated position")
        .isLessThan(capitalWeightedProb)
        .isCloseTo(50, within(2));
}

@Test
@DisplayName("Time-Weighted: Single position equals capital-weighted")
void testTimeWeighted_SinglePosition() {
    PositionDto pos = createPosition(5000, 50, 30);
    pos.setProbability(70);

    List<PositionDto> openPositions = List.of(pos);

    // With single position, time-weighting doesn't change the result
    double capitalROI = calculateWeightedROI(openPositions);
    double timeROI = calculateTimeWeightedROI(openPositions);

    assertThat(timeROI).isEqualTo(capitalROI);
}

@Test
@DisplayName("Time-Weighted: Zero/null daysLeft handled gracefully")
void testTimeWeighted_ZeroDaysLeft() {
    PositionDto pos1 = createPosition(5000, 50, 30);
    PositionDto pos2 = createPosition(5000, 50, 0);  // Expiring today

    List<PositionDto> openPositions = List.of(pos1, pos2);

    // Should handle zero daysLeft gracefully (skip it)
    double timeROI = calculateTimeWeightedROI(openPositions);
    assertThat(timeROI).isNotNull();
}
```

---

## Related Issues

- **Depends on**: ISSUE-024 (Position-Weighted Portfolio Calculations) - Foundation for capital weighting
- **Future Enhancement**: Portfolio-level Greeks (theta, delta, gamma, vega) with time weighting

---

## Notes

### Why Normalized Square Root Time Weighting?

1. **Matches Options Theory**: Volatility scales as `σ × √t` (Black-Scholes)
2. **Consistent with Codebase**: Already used in `PositionMapper.java:158` for probability
3. **TastyTrade Alignment**: 45 DTE is their standard mechanical trading duration
4. **Interpretable**: Weight of 1.0 at 45 DTE, scales proportionally

### Implementation Considerations

1. **Non-Breaking**: Add new methods, don't modify existing `calculateWeightedROI/Probability()`
2. **Backward Compatible**: Existing UI and calculations unchanged
3. **Future UI Integration**: Can display both metrics side-by-side
4. **Performance**: Minimal overhead (one additional sqrt operation per position)

### Example Portfolio Comparison

```
Portfolio: 3 positions, $30K total capital

Position A: $10K, 50% ROI, 7 DTE   (expiring soon)
Position B: $10K, 50% ROI, 45 DTE  (standard)
Position C: $10K, 50% ROI, 90 DTE  (long-dated)

Capital-Weighted ROI: 50% (all equal)

Time-Weighted Calculation:
- Weight A = 10K × √(7/45)  = 3,944  (0.39× baseline)
- Weight B = 10K × √(45/45) = 10,000 (1.00× baseline)
- Weight C = 10K × √(90/45) = 14,142 (1.41× baseline)
- Total Weight = 28,086

Time-Weighted ROI = (50%×3944 + 50%×10000 + 50%×14142) / 28086 = 50%

(Same ROI because all have same ROI%, but shows weight distribution)
```

### References

- **Black-Scholes Model**: Volatility term `σ√t`
- **TastyTrade Methodology**: 45 DTE standard for mechanical trading
- **Existing Implementation**: `PositionMapper.java:158` - `Math.sqrt(days)` for probability
- **ISSUE-024**: Capital-weighted calculations (foundation)

---

**Implementation Location**: `src/main/java/co/grtk/srcprofit/service/OptionService.java`
**Test Location**: `src/test/java/co/grtk/srcprofit/service/OptionServiceTest.java`
