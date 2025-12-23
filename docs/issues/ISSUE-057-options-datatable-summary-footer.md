# ISSUE-057: Add summary footer to Options datatable on OpenPositions page

**Created**: 2025-12-23
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

The OpenPositions page displays an Options datatable with multiple positions, but there is no summary row showing totals for key financial metrics. Users need to mentally calculate or manually sum values to understand total exposure, total cost basis, total P&L, and overall return.

This makes it difficult to quickly assess portfolio-level metrics when viewing filtered subsets of positions (e.g., only SPY positions, only PUT options, or positions expiring in a specific timeframe).

---

## Root Cause

The current datatable implementation does not include a `<tfoot>` element or `footerCallback` function to display aggregated values. While DataTables supports footer summaries that update dynamically with filters, this feature has not been implemented.

---

## Approach

Implement DataTables footer callback functionality to display filtered summary totals:

1. **Add HTML structure**: Add `<tfoot>` element to the Options datatable with placeholder cells aligned to summary columns (Qty, Strike, Price, CostBase, P&L)

2. **Add JavaScript logic**: Implement `footerCallback` function in the DataTable initialization that:
   - Sums Qty (quantity) for visible/filtered rows
   - Calculates average Strike price for visible/filtered rows (2 decimal places)
   - Calculates average underlying Price for visible/filtered rows (2 decimal places)
   - Sums CostBase (cost basis) for visible/filtered rows (rounded to integer)
   - Sums P&L (profit/loss) for visible/filtered rows (rounded to integer)
   - Uses `{page: 'current', search: 'applied'}` selector to respect search filters

3. **Add CSS styling**: Style footer row to visually distinguish from data rows, with support for both light and dark themes

4. **Preserve existing functionality**: Ensure row grouping, click handlers, and other features continue to work

---

## Success Criteria

- [x] Footer row visible on OpenPositions page Options datatable
- [x] Qty column shows sum of quantities
- [x] Strike column shows average strike price with currency formatting (2 decimals)
- [x] Price column shows average underlying price with currency formatting (2 decimals)
- [x] CostBase column shows sum of cost basis as integer (no decimals)
- [x] P&L column shows sum of profit/loss as integer (no decimals)
- [x] Footer updates correctly when search filter is applied
- [x] Footer styling matches page theme (light/dark mode)
- [x] No regression in existing functionality (row grouping, click handlers)

---

## Acceptance Tests

### Manual Test Cases

1. **No filter applied**:
   - Open `/openpositions` page
   - Verify footer displays summaries for all positions
   - Check Qty (sum), Strike (avg), Price (avg), CostBase (sum as integer), P&L (sum as integer) values are non-zero

2. **Symbol filter**:
   - Search for "SPY" in datatable search box
   - Verify footer updates to show only SPY position totals
   - Verify totals are less than or equal to unfiltered totals

3. **Type filter**:
   - Search for "PUT" in datatable search box
   - Verify footer updates to show only PUT option totals

4. **Empty result**:
   - Search for "XXXXX" (non-existent symbol)
   - Verify footer shows 0 or empty values

5. **Number formatting**:
   - Verify Strike and Price show 2 decimal places (e.g., $125.50)
   - Verify CostBase and P&L show no decimals (e.g., $1,234)

6. **Theme compatibility**:
   - Test in light mode (default)
   - Switch to dark mode via theme toggle
   - Verify footer styling updates appropriately

7. **Existing functionality**:
   - Click on expiration date group header → verify grouping still works
   - Click on data row → verify navigation to position detail still works
   - Verify footer is NOT clickable

---

## Related Issues

- Related: ISSUE-054 (Added stocks datatable to OpenPositions page)
- Future: Consider extending this pattern to Stocks datatable

---

## Notes

- **Implementation file**: `/Users/Imre/IdeaProjects/other/srcprofit/src/main/jte/openpositions_jte.jte`
- **DataTables version**: 2.3.2 with Bootstrap 5 integration
- **Column indices** (0-based): Qty=5, Strike=6, Price=7, CostBase=9, P&L=10
- **Summary types**: Qty (sum), Strike (average), Price (average), CostBase (sum, integer), P&L (sum, integer)
- **Reference**: [DataTables Footer Callback Documentation](https://datatables.net/examples/advanced_init/footer_callback.html)
- **Pattern used**: Similar to existing row grouping implementation in same file

**Calculation Details**:
- **Qty**: Simple sum of all quantities
- **Average Strike**: `Σ(Strike_i) / count` - Simple average of strike prices (2 decimal places)
- **Average Price**: `Σ(Price_i) / count` - Simple average of underlying prices (2 decimal places)
- **CostBase Total**: `Σ(CostBase_i)` - Sum of cost basis rounded to integer (no decimals)
- **P&L Total**: `Σ(P&L_i)` - Sum of profit/loss rounded to integer (no decimals)
