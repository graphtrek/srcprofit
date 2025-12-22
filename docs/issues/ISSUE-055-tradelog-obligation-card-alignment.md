# ISSUE-055: Trade Log Obligation Card Layout Alignment

**Created**: 2025-12-22 (Session)
**Status**: CLOSED
**Completed**: 2025-12-22
**Priority**: MEDIUM
**Category**: UI Enhancement / Code Quality
**Blocking**: N/A

---

## Problem

The Trade Log page obligation cards were inconsistent with the Dashboard layout:
- Buy Obligation card displayed both Cash and Stock information
- Sell Obligation card displayed only the main value
- Dashboard page had the opposite arrangement (Buy shows Cash, Sell shows Stock)

This inconsistency created confusion about which obligation type should display stock information.

---

## Root Cause

The Trade Log template was originally designed with all relevant information in Buy Obligation, but following the TastyTrade methodology:
- Buy obligations = need cash to purchase
- Sell obligations = need stock to sell

Stock information logically belongs with Sell Obligation, not Buy Obligation.

---

## Approach

1. Align Trade Log card layout with Dashboard layout
2. Remove Stock display from Buy Obligation card
3. Add Stock display to Sell Obligation card
4. Update OptionService to use IBKR's pre-calculated P&L instead of recalculating

---

## Success Criteria

- [x] Buy Obligation card shows only Cash
- [x] Sell Obligation card shows Stock
- [x] Layout matches Dashboard page
- [x] OptionService uses IBKR's fifoPnlUnrealized value
- [x] Tests passing
- [x] Build succeeds

---

## Acceptance Tests

**Visual consistency**:
- Trade Log Buy Obligation card displays: Position Value, Cash, Cash label
- Trade Log Sell Obligation card displays: Call Obligation Value, Stock, Stock label
- Matches Dashboard layout exactly

**P&L Calculation**:
- Uses IBKR's pre-calculated unrealized P&L (includes correct multiplier)
- Avoids unnecessary recalculation

---

## Related Issues

- Related: ISSUE-054 - Add Stocks DataTable to Open Positions Page
- Related: ISSUE-033 - Fix Profit/Loss Calculation (TastyTrade Methodology)

---

## Notes

**Files changed**:
- `src/main/jte/tradelog_jte.jte` - Moved Stock display from Buy to Sell Obligation card
- `src/main/java/co/grtk/srcprofit/service/OptionService.java` - Use IBKR's fifoPnlUnrealized instead of recalculating

**Commit**: 3596391 - feat(ISSUE-054): Align tradelog card layout with dashboard

**TastyTrade Methodology Reference**:
- Buy obligations = cash requirement (liquidity)
- Sell obligations = stock requirement (assets needed to deliver)
