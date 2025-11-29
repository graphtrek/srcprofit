# ISSUE-038: Add ROI, POP, and daysLeft to Option Snapshots

**Created**: 2025-11-28
**Status**: IN PROGRESS
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

Option snapshots (OptionSnapshotEntity) contain raw market data (prices, Greeks) but lack derived financial metrics that traders use for decision-making:

1. **ROI (Return on Investment)**: Need to calculate both ROI on collateral (capital at risk) and ROI on premium (return basis) to assess capital efficiency
2. **POP (Probability of Profit)**: Need probability estimate to evaluate trade quality using delta approximation
3. **Days Left**: Need to display time remaining until expiration (DTE) for time decay analysis

Currently, these calculations must be done manually or in the UI, making it harder to filter, sort, and analyze option opportunities.

---

## Root Cause

OptionSnapshotEntity was designed as a pure data transfer object that mirrors Alpaca API response structure. It stores raw market data but doesn't calculate derived trading metrics.

The `saveOrUpdateSnapshot()` method in OptionSnapshotService maps API data directly to entity fields without performing any financial calculations.

---

## Approach

Add 4 calculated fields to OptionSnapshotEntity and compute them in `saveOrUpdateSnapshot()`:

### 1. Database Schema
Add 4 columns to OPTION_SNAPSHOT table:
- `roi_on_collateral` (INTEGER) - Annualized ROI on capital at risk (strike price)
- `roi_on_premium` (INTEGER) - Annualized ROI on premium (midPrice)
- `pop` (INTEGER) - Probability of Profit using delta approximation (0-100)
- `days_left` (INTEGER) - Days until expiration (can be negative)

### 2. Entity Fields
Add 4 fields to OptionSnapshotEntity with appropriate Javadoc:
```java
public Integer daysLeft;          // Days until expiration
public Integer roiOnCollateral;   // (midPrice / strikePrice) * (365 / daysLeft) * 100
public Integer roiOnPremium;      // (365 / daysLeft) * 100
public Integer pop;               // (1 - |delta|) * 100
```

### 3. Calculation Method
Add `calculateDerivedMetrics()` private method in OptionSnapshotService:
- Calculate daysLeft using existing `PositionCalculationHelper.calculateDaysLeft()`
- Calculate ROI on Collateral: `(midPrice / strikePrice) * (365 / daysLeft) * 100`
- Calculate ROI on Premium: `(365 / daysLeft) * 100`
- Calculate POP using delta approximation: `(1 - |delta|) * 100`

### 4. Null Handling
- Require `instrument.getPrice()` to be non-null (already validated)
- If `getMidPrice()` is null (no bid/ask): set ROI fields to null
- If `delta` is null: set POP to null
- If `daysLeft <= 0`: set ROI to null (expired options)
- If `strikePrice` is zero or null: set ROI to null

### 5. Testing
Add 3 unit tests to OptionSnapshotServiceTest:
- Test 8: Happy path with known values
- Test 9: Null handling (missing bid/ask, missing delta)
- Test 10: Expired options (negative daysLeft)

---

## Success Criteria

- [ ] V003 database migration created and runs successfully
- [ ] OptionSnapshotEntity has 4 new fields with getters/setters
- [ ] OptionSnapshotService has `calculateDerivedMetrics()` method
- [ ] Method called from `saveOrUpdateSnapshot()` after Greeks mapping
- [ ] Test 8: Happy path calculations verified
- [ ] Test 9: Null handling verified
- [ ] Test 10: Expired options verified
- [ ] All existing tests pass (no regressions)
- [ ] Code coverage >= 100% on new code

---

## Acceptance Tests

See OptionSnapshotServiceTest.java for test implementations.

---

## Related Issues

- Depends on: ISSUE-037 (Alpaca Option Snapshots Download) - CLOSED
- Related: ISSUE-023 (Refactor Annualized ROI Calculation)
- Related: ISSUE-033 (Fix P&L Calculation)

---

## Notes

### Design Decisions

1. **Two ROI Metrics**: Calculate both ROI on collateral and ROI on premium
2. **POP via Delta**: Use delta approximation (TastyTrade methodology)
3. **Require Instrument Price**: Must have instrument.getPrice() for validation
4. **Integer Percentages**: Store as Integer (consistent with existing patterns)
