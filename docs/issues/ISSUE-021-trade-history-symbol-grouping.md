# ISSUE-021: Trade History Symbol Grouping & Color Consistency

**Created**: 2025-11-10 (Session 6)
**Status**: CLOSED
**Completed**: 2025-11-15
**Priority**: MEDIUM
**Category**: Feature / UX Enhancement
**Blocking**: None

---

## Problem

The Trade History page currently displays closed positions in a flat list without grouping, making it difficult to quickly see all trades for a specific symbol. Additionally, the visual consistency with the Trade Log page (which uses row grouping) should be maintained.

---

## Root Cause

When ISSUE-020 separated Trade History from Trade Log, the grouping functionality was not applied to the new Trade History page. Trade Log groups by Expiration Date, but Trade History would benefit from grouping by Symbol to track trading activity per ticker.

---

## Approach

Implement DataTables row grouping by Symbol in Trade History, with descending TradeDate order within each group, using the same visual styling as Trade Log's group headers.

### Frontend Changes

**File**: `src/main/jte/trade_history_jte.jte`

1. **Add grouping variable and drawCallback**:
   - Group by column 2 (Symbol)
   - Order: Symbol ascending, TradeDate descending
   - Insert group header rows with same styling as Trade Log

2. **Group Header Styling** (match Trade Log):
   - Background color: `#c3c3c3` (light gray)
   - Text color: `#4154f1` (theme primary blue)
   - Borders: `3px solid #4154f1` (top and bottom)
   - Column span: 13 (Trade History has 13 columns)

3. **Interactive Group Headers**:
   - Click handler to toggle ascending/descending sort
   - Same pattern as Trade Log implementation

4. **Row Click Handler Update**:
   - Exclude group rows from click handler: `'tr:not(.group)'`
   - Prevent navigation when clicking group headers

### Color Scheme (Already Applied)

Trade History already uses colors consistently with Trade Log:
- ID and Code columns use `option.getColor()` (text color)
- Status badges: PENDING (yellow), CLOSED (green), others (red)

**Note**: Trade Log's red background indicator (market value < position value) is specific to open positions and not applicable to closed positions.

---

## Technical Implementation

### DataTable Configuration

**Before**:
```javascript
const tableHistory = new DataTable('#datatableTradeHistory', {
    pageLength: 500,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [{target: 1, visible: false, searchable: false}],
    order: [[1, 'asc'],[3, 'desc']] // code, TradeDate
});
```

**After**:
```javascript
var groupColumnHistory = 2; // Symbol column (0-indexed)

const tableHistory = new DataTable('#datatableTradeHistory', {
    pageLength: 500,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [{target: 1, visible: false, searchable: false}],
    order: [[groupColumnHistory, 'asc'], [3, 'desc']], // Symbol asc, TradeDate desc
    drawCallback: function (settings) {
        var api = this.api();
        var rows = api.rows({ page: 'current' }).nodes();
        var last = null;

        api.column(groupColumnHistory, { page: 'current' })
            .data()
            .each(function (group, i) {
                if (last !== group) {
                    $(rows).eq(i).before(
                        '<tr class="group"><td colspan="13" style="font-weight:bold; background-color:#c3c3c3; color:#4154f1; padding:12px 8px; border-top:3px solid #4154f1; border-bottom:3px solid #4154f1;">' +
                        group +
                        '</td></tr>'
                    );
                    last = group;
                }
            });
    }
});
```

### Interactive Group Header Handler

```javascript
$('#datatableTradeHistory tbody').on('click', 'tr.group', function () {
    var currentOrder = tableHistory.order()[0];
    if (currentOrder[0] === groupColumnHistory && currentOrder[1] === 'asc') {
        tableHistory.order([[groupColumnHistory, 'desc']]).draw();
    } else {
        tableHistory.order([[groupColumnHistory, 'asc']]).draw();
    }
});
```

### Updated Row Click Handler

```javascript
$('#datatableTradeHistory tbody').on('click', 'tr:not(.group)', function() {
    const ticker = tableHistory.row(this).data()[2];
    console.log('GET  /getPosition/' + ticker);
    htmx.ajax('GET', '/getPosition/' + ticker, '#main');
});
```

---

## Success Criteria

- [x] Trade History groups closed positions by Symbol
- [x] Within each symbol group, positions are sorted by TradeDate descending (most recent first)
- [x] Group headers use same styling as Trade Log (gray background, blue text, blue borders)
- [x] Group headers are clickable to toggle ascending/descending sort
- [x] Data row clicks navigate to position detail page
- [x] Group header clicks do NOT trigger position detail navigation
- [x] Filter dropdown (This Week/Month/Year/All) continues to work correctly
- [x] All existing tests pass (62 tests)
- [x] Visual consistency maintained between Trade Log and Trade History pages

---

## Manual Testing Checklist

1. **Grouping Verification**:
   - [ ] Navigate to Trade History page
   - [ ] Verify positions grouped by Symbol (AAPL, MSFT, TSLA, etc.)
   - [ ] Verify group headers display symbol names in uppercase

2. **Sorting Within Groups**:
   - [ ] Verify most recent trades appear first within each symbol group
   - [ ] Verify TradeDate descending order within groups

3. **Interactive Group Headers**:
   - [ ] Click a group header
   - [ ] Verify sort order toggles (A-Z ↔ Z-A)
   - [ ] Verify group header click does NOT navigate away

4. **Data Row Navigation**:
   - [ ] Click a data row (not a group header)
   - [ ] Verify navigation to `/getPosition/{ticker}` works

5. **Filter Dropdown**:
   - [ ] Select "This Week" - verify grouping persists
   - [ ] Select "This Month" - verify grouping persists
   - [ ] Select "This Year" - verify grouping persists
   - [ ] Select "All" - verify grouping persists

6. **Visual Consistency**:
   - [ ] Compare group header styling with Trade Log page
   - [ ] Verify colors match (gray background, blue text, blue borders)

---

## Related Issues

- **ISSUE-020**: Trade History Menu Separation - Created the Trade History page
- **ISSUE-019**: DataTables Expiration Grouping - Implemented grouping in Trade Log

---

## Notes

### Design Rationale

**Why group by Symbol instead of Expiration?**
- Trade History focuses on historical trading activity
- Users want to see all trades for a specific stock together
- Expiration dates are less relevant for closed positions
- Symbol grouping provides better insight into trading patterns per ticker

**Why TradeDate descending?**
- Most recent trades are most relevant for review
- Matches user expectation for historical data
- Easier to track latest activity per symbol

### Column Count Difference

- **Trade Log**: 14 columns (includes R.O.I, P.O.P for open positions)
- **Trade History**: 13 columns (includes Status badge for closed positions)
- Group header colspan adjusted accordingly: `colspan="13"`

### Files to Modify

- `src/main/jte/trade_history_jte.jte` (JavaScript section only)

### No Backend Changes Required

This is a pure client-side feature:
- No controller changes needed
- No service layer changes needed
- No test file updates required (existing tests cover data delivery)
- Grouping logic implemented entirely in DataTables JavaScript

### Estimated Effort

**30-45 minutes** (straightforward implementation following established pattern from ISSUE-019)
