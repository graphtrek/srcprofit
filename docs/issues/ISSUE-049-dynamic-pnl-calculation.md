# ISSUE-049: Add Dynamic P&L Calculation to OpenPositionViewDto

## Status: OPEN
## Priority: Medium
## Type: Feature Enhancement
## Created: 2025-12-07

---

## Summary

Add a new `calculatedPnl` field to `OpenPositionViewDto` that dynamically calculates profit/loss based on current market data (quantity, markPrice, costBasisPrice, multiplier) instead of only showing the static `fifoPnlUnrealized` value from IBKR CSV imports.

---

## Problem

Currently, P&L in `convertToOpenPositionViewDto()` is static:
- Read directly from `entity.getFifoPnlUnrealized()` (line 765)
- Comes from IBKR CSV import, not calculated in real-time
- Does not reflect current market conditions
- No way to compare IBKR's FIFO P&L with calculated P&L

---

## Solution

### Add Dynamic P&L Calculation

**Formula:** Market Value - Cost Basis
- Market Value = `quantity × markPrice × multiplier`
- Cost Basis = `quantity × costBasisPrice × multiplier`
- P&L = Market Value - Cost Basis

**Implementation:**
1. Add `calculateUnrealizedPnl()` helper method to `PositionCalculationHelper`
2. Add `calculatedPnl` field to `OpenPositionViewDto` record (10th parameter)
3. Calculate value in `convertToOpenPositionViewDto()` method
4. Keep existing `fifoPnlUnrealized` field unchanged (backwards compatible)

---

## Files to Modify

### Core Implementation
1. **PositionCalculationHelper.java** - Add `calculateUnrealizedPnl()` method
2. **OpenPositionViewDto.java** - Add `calculatedPnl` parameter to record
3. **OpenPositionService.java** - Calculate and pass value in `convertToOpenPositionViewDto()`

### Tests
4. **PositionCalculationHelperTest.java** - Add 9 unit tests
5. **OpenPositionServiceTest.java** - Add 3 integration tests
6. **OpenPositionsControllerTest.java** - Update 3 existing tests

---

## Calculation Examples

### Short PUT (Profit)
- Sold 1 PUT at $5.00, now worth $4.50
- Quantity: -1, markPrice: 4.5, costBasisPrice: 5.0, multiplier: 100
- Market Value = -1 × 4.5 × 100 = -450
- Cost Basis = -1 × 5.0 × 100 = -500
- **P&L = -450 - (-500) = 50** (profit)

### Long CALL (Profit)
- Bought 2 CALLs at $3.00, now worth $5.00
- Quantity: 2, markPrice: 5.0, costBasisPrice: 3.0, multiplier: 100
- Market Value = 2 × 5.0 × 100 = 1000
- Cost Basis = 2 × 3.0 × 100 = 600
- **P&L = 1000 - 600 = 400** (profit)

---

## Edge Cases

1. **Null parameters** - Return `null` if any required field is missing
2. **Zero quantity** - Valid calculation, result = 0.0
3. **Missing multiplier** - Common for non-standard instruments, calculatedPnl = null
4. **P&L mismatch** - IBKR uses FIFO cost basis, we use average → values may differ

---

## Testing Requirements

### Unit Tests (9 tests)
- Short PUT profit/loss scenarios
- Long CALL profit scenario
- Null parameter handling (4 tests: quantity, markPrice, costBasisPrice, multiplier)
- Rounding behavior (2 decimal places)
- Zero quantity edge case

### Integration Tests (3 tests)
- `convertToOpenPositionViewDto_calculatesRealTimePnl` - Complete data
- `convertToOpenPositionViewDto_handlesNullPnlFields` - Missing data
- `convertToOpenPositionViewDto_multiplePositionsPnlVariations` - Mixed scenarios

### Controller Tests (3 updates)
- Update existing tests to include `calculatedPnl` parameter

---

## Success Criteria

- ✅ All 252 tests pass (240 existing + 12 new)
- ✅ Code compiles successfully
- ✅ `calculatedPnl` field accessible in OpenPositionViewDto
- ✅ Null handling works correctly (no NPEs)
- ✅ Existing `pnl` field unchanged (backwards compatible)
- ✅ No performance degradation (O(1) calculation per position)

---

## Benefits

1. **Real-time P&L** - Reflects current market conditions
2. **Comparison capability** - Can compare IBKR FIFO P&L vs calculated P&L
3. **Backwards compatible** - Keeps existing `fifoPnlUnrealized` field
4. **No database changes** - Pure calculation, no schema migration needed
5. **Performance** - O(1) arithmetic operation, no additional queries

---

## Future Enhancements

1. **Live market data** - Replace static `markPrice` with real-time data from Alpaca
2. **P&L comparison dashboard** - Side-by-side comparison of IBKR vs calculated
3. **Historical tracking** - Store calculated P&L snapshots over time
4. **UI toggle** - Switch between IBKR P&L and calculated P&L in UI

---

## Notes

- Formula uses standard accounting convention (positive = profit for long, loss for short)
- Uses `Double` type (not `BigDecimal`) to match existing entity architecture
- Rounds to 2 decimal places using `MapperUtils.round2Digits()`
- No external dependencies required
