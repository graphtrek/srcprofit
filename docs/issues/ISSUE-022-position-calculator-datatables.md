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

2. **Position History Table**: Custom implementation
   - Group by Trade Date (column 3)
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
// 1. Group rows by expiration date with interactive headers
// 2. Click group header to toggle sort order (asc/desc)
// 3. Allow column sorting, filtering, and pagination
// 4. Click row to navigate to position details via HTMX
// 5. Show all records (pageLength: -1)

// Position History table should:
// 1. Group rows by trade date with interactive headers
// 2. Default sort: TradeDate desc (most recent first), then Symbol asc
// 3. Click group header to toggle sort order (desc/asc)
// 4. Allow column sorting, filtering, and pagination
// 5. Click row to navigate to position details via HTMX
// 6. Show 500 records per page
```

---

## Related Issues

- Related: ISSUE-018 (DataTables research)
- Related: ISSUE-019 (Expiration grouping implementation - Trade Log)
- Related: ISSUE-021 (Trade History symbol grouping)

---

## Implementation Details

**Files Modified**:
- `src/main/jte/position-form_jte.jte` - Full DataTables implementation with grouping logic
- `src/main/jte/tradelog_jte.jte` - Group header styling updated (lighter background)
- `src/main/jte/trade_history_jte.jte` - Group header styling updated (lighter background)

**Open Positions Table** (`position-form_jte.jte:343-388`):
- Table ID: `datatableOpenPositions`
- Group column: 4 (Expiration Date)
- Default order: `[[4, 'asc'], [2, 'asc']]` (Expiration asc, Symbol asc)
- Page length: -1 (show all)
- Group header styling: Gray background (#d7deea), blue text (#4154f1), 2px borders
- Colspan: 14 columns

**Position History Table** (`position-form_jte.jte:390-435`):
- Table ID: `datatablePositionHistory`
- Group column: 3 (Trade Date)
- Default order: `[[3, 'desc'], [2, 'asc']]` (TradeDate desc, Symbol asc)
- Page length: 500
- Group header styling: Gray background (#d7deea), blue text (#4154f1), 2px borders
- Colspan: 13 columns

**DataTables Library** (already included in `index_jte.jte`):
- CSS: `//cdn.datatables.net/2.3.2/css/dataTables.bootstrap5.css`
- JS: `//cdn.datatables.net/2.3.2/js/dataTables.js` and `dataTables.bootstrap5.js`

**Features**:
- Currency formatting with `Intl.NumberFormat` (USD, no decimals)
- HTMX integration for row click navigation
- Code column hidden but searchable in DataTables
- Conditional row styling (red background for loss positions)
- Smaller font size (0.85rem) for compact display

**Controller/Service**: No changes required - existing endpoints provide data in correct format
