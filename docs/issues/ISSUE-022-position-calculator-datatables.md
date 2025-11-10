# ISSUE-022: Position Calculator DataTables Enhancement

**Created**: 2025-11-10 (Session Current)
**Status**: CLOSED
**Completed**: 2025-11-10
**Priority**: MEDIUM
**Category**: Feature / UX Enhancement
**Blocking**: None

---

## Problem

The Position Calculator page displays two tables (Open Positions and Position History) using plain HTML with no DataTables functionality. This results in:
- No sorting capability
- No filtering or search
- No pagination
- Poor UX compared to Trade Log and Trade History pages

---

## Root Cause

The `position-form_jte.jte` template defines HTML tables with `id="datatable"` but never initializes DataTables JavaScript or implements grouping/interactive features like the Trade Log and Trade History pages do.

---

## Approach

Implement DataTables for both tables following existing patterns:

1. **Open Positions Table**: Use Trade Log pattern
   - Group by Expiration Date (column 4)
   - Sort by Expiration asc, then Symbol asc
   - Add HTMX row click handlers for position detail navigation
   - Show all records (pageLength: -1)

2. **Position History Table**: Use Trade History pattern
   - Group by Symbol (column 2)
   - Sort by TradeDate desc, then Symbol asc
   - Add HTMX row click handlers for position detail navigation
   - Higher page limit (pageLength: 500)

3. **Reference Implementations**:
   - Trade Log grouping: `tradelog_jte.jte:345-407`
   - Trade History grouping: `trade_history_jte.jte:119-163`

---

## Success Criteria

- [x] Create issue file
- [x] Open Positions table has DataTables with expiration grouping
- [x] Position History table has DataTables with symbol grouping
- [x] Row click handlers implement HTMX navigation to `/getPosition/{ticker}`
- [x] Tables support sorting, filtering, pagination
- [x] Styling consistent with Trade Log/History (group headers bold, special background)
- [x] Tests pass (`./mvnw test`)
- [x] Build succeeds (`./mvnw clean compile`)

---

## Acceptance Tests

```javascript
// Open Positions table should:
// 1. Group rows by expiration date
// 2. Allow column sorting
// 3. Support search/filter
// 4. Navigate on row click

// Position History table should:
// 1. Group rows by symbol
// 2. Sort by trade date descending
// 3. Allow column sorting
// 4. Support search/filter
// 5. Navigate on row click
```

---

## Related Issues

- Related: ISSUE-018 (DataTables research)
- Related: ISSUE-019 (Expiration grouping implementation - Trade Log)
- Related: ISSUE-021 (Trade History symbol grouping)

---

## Notes

**Files to Modify**:
- `src/main/jte/position-form_jte.jte` - Add DataTables initialization and grouping logic

**DataTables Library** (already included in `index_jte.jte`):
- CSS: `//cdn.datatables.net/2.3.2/css/dataTables.bootstrap5.css`
- JS: `//cdn.datatables.net/2.3.2/js/dataTables.js` and `dataTables.bootstrap5.js`

**Controller/Service**: No changes required - endpoints already provide data in correct format
