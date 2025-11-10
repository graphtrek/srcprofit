# ISSUE-019: Implement DataTables Row Grouping by Expiration Date for Open Positions

**Created**: 2025-11-10 (Session N/A)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

Options traders need to assess risk by expiration cycle. The Open Positions table currently displays positions in a flat list without visual organization by expiration date. This makes it difficult to:

1. **Identify expiration risk concentration** - See how many positions expire on the same date
2. **Plan rolling strategies** - Assess which positions need attention in the upcoming expiration cycle
3. **Manage position lifecycle** - Visually separate positions by time until expiration
4. **Prevent "Friday surprise"** - Easily spot positions expiring soon

Grouping positions by expiration date provides immediate visual hierarchy aligned with options trading time-based risk management.

---

## Root Cause

The Open Positions table uses basic DataTables configuration without row grouping by expiration date. While the table is sortable by expiration column, visual grouping significantly improves:
- Pattern recognition (see all positions expiring 2025-11-15 together)
- Risk assessment (assess concentration per expiration cycle)
- Decision-making speed (quickly identify next expiration requiring action)

---

## Approach

Apply the row grouping pattern from ISSUE-018 (DataTables Row Grouping Research) to the Open Positions table, using **Expiration Date** as the grouping column instead of ticker symbol.

### Implementation Details

**Grouping Column**: Column index 4 (Expiration)
- **Data Source**: `option.getExpirationDateString()`
- **Format**: YYYY-MM-DD (ISO date format, naturally sortable)
- **Current Usage**: `tradelog_jte.jte:299`

**Sort Order**: Primary by expiration (ascending = nearest first), secondary by symbol
- **Configuration**: `order: [[4, 'asc'], [2, 'asc']]`
- **Business Logic**: Show nearest expirations at the top, grouped by date, with symbols alphabetically ordered within each expiration group

**Visual Organization**:
- Group header rows display expiration dates (e.g., "2025-11-15", "2025-11-22")
- Interactive headers toggle sort order (near-to-far vs far-to-near)
- Same CSS styling as ticker grouping from ISSUE-018

### Files to Modify

1. **Primary**: `/Users/Imre/IdeaProjects/other/srcprofit/src/main/jte/tradelog_jte.jte`
   - Lines 420-427: Replace current table initialization
   - Add `drawCallback` function for grouping logic
   - Add event handlers for interactive group headers
   - Modify existing row click handler to exclude group rows

2. **Secondary**: `/Users/Imre/IdeaProjects/other/srcprofit/src/main/resources/static/assets/css/style.css`
   - Add `.group` class styling (can reuse from ISSUE-018 if already implemented)

### Technical Dependencies

**Required** (already in SrcProfit):
- jQuery
- DataTables v2.3.4+
- Modern browser (ES5+)

**No Changes Needed**:
- Backend/server (client-side only)
- Database schema
- Java DTOs or entities

---

## Success Criteria

- [ ] Open Positions table groups by expiration date (column 4)
- [ ] Group headers display dates in YYYY-MM-DD format
- [ ] Positions sorted by expiration (ascending = nearest first), then by symbol within each group
- [ ] Group headers are interactive (click to toggle sort order)
- [ ] Group headers styled with SrcProfit theme colors (primary: rgba(65, 84, 241))
- [ ] Existing row click handlers work (`/getPosition/:ticker` navigation)
- [ ] Grouping persists correctly across pagination
- [ ] Colspan matches table width (14 columns)
- [ ] Performance acceptable with 50+ positions
- [ ] No JavaScript errors in browser console

---

## Acceptance Tests

### Test 1: Group Headers Render by Expiration Date
```
Given: Open Positions table with positions expiring on multiple dates (e.g., 2025-11-15, 2025-11-22, 2025-12-20)
When: Table renders
Then: Group header rows appear before each new expiration date
And: Group headers display dates in YYYY-MM-DD format
And: Group headers have colspan=14
And: Group headers are styled distinctly (background color, bold text)
```

### Test 2: Sort Order - Nearest Expiration First
```
Given: Open Positions table with multiple expiration dates
When: Table renders with default configuration
Then: Nearest expiration date group appears at the top
And: Within each expiration group, positions are sorted by symbol (A-Z)
And: Farthest expiration date group appears at the bottom
```

### Test 3: Interactive Group Header Toggle
```
Given: Open Positions table grouped by expiration in ascending order (nearest first)
When: User clicks on group header "2025-11-15"
Then: Table re-sorts expiration column to descending (farthest first)
And: Group order reverses (farthest expiration at top)
When: User clicks again
Then: Table re-sorts to ascending (nearest first)
```

### Test 4: Pagination Preserves Grouping
```
Given: Open Positions table with 50+ positions across 10+ expiration dates
And: Page length set to 10
When: User navigates between pages
Then: Grouping continues correctly on each page
And: Group headers appear appropriately on page boundaries
And: No duplicate or missing group headers
```

### Test 5: Existing Row Click Handler Still Works
```
Given: Open Positions table grouped by expiration
When: User clicks on a position row (not group header)
Then: /getPosition/:ticker endpoint is called
And: Position detail view opens correctly
When: User clicks on a group header row
Then: Sort order toggles (no position detail navigation)
```

---

## Implementation Code

### JavaScript Implementation (tradelog_jte.jte)

**Current Code** (lines 420-427):
```javascript
const tableOpen = new DataTable('#datatableOpenPositions', {
    pageLength: 10,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
    order: [[4, 'asc'],[1, 'asc']] // daysLeft, code
});
```

**Enhanced with Expiration Grouping**:
```javascript
var groupColumnOpen = 4; // Expiration date column (0-indexed)

const tableOpen = new DataTable('#datatableOpenPositions', {
    pageLength: 10,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [{target: 1, visible: false, searchable: false}], // code hidden
    order: [[groupColumnOpen, 'asc'], [2, 'asc']], // Expiration first, then Symbol
    drawCallback: function (settings) {
        var api = this.api();
        var rows = api.rows({ page: 'current' }).nodes();
        var last = null;

        api.column(groupColumnOpen, { page: 'current' })
            .data()
            .each(function (group, i) {
                if (last !== group) {
                    $(rows).eq(i).before(
                        '<tr class="group"><td colspan="14" style="font-weight:bold;">' +
                        group +
                        '</td></tr>'
                    );
                    last = group;
                }
            });
    }
});

// Interactive group header click handler
$('#datatableOpenPositions tbody').on('click', 'tr.group', function () {
    var currentOrder = tableOpen.order()[0];
    if (currentOrder[0] === groupColumnOpen && currentOrder[1] === 'asc') {
        tableOpen.order([[groupColumnOpen, 'desc']]).draw();
    } else {
        tableOpen.order([[groupColumnOpen, 'asc']]).draw();
    }
});

// Keep existing row click handler - exclude group rows
$('#datatableOpenPositions tbody').on('click', 'tr:not(.group)', function() {
    const ticker = tableOpen.row(this).data()[2];
    console.log('GET  /getPosition/' + ticker);
    htmx.ajax('GET', '/getPosition/' + ticker, '#main');
});
```

### CSS Styling (style.css)

Add the following CSS for group row styling (reusable from ISSUE-018 if already implemented):

```css
/* DataTables row grouping styles */
tr.group {
    background-color: rgba(65, 84, 241, 0.1) !important; /* SrcProfit primary color */
    cursor: pointer;
    font-weight: bold;
}

tr.group:hover {
    background-color: rgba(65, 84, 241, 0.2) !important;
}

tr.group td {
    padding: 12px 8px !important;
    border-top: 2px solid rgba(65, 84, 241, 0.3);
}
```

---

## Key Differences from Ticker Grouping (ISSUE-018)

| Aspect | Ticker Grouping (ISSUE-018) | Expiration Grouping (This Issue) |
|--------|------------------------------|----------------------------------|
| **Group Column** | Column 2 (Symbol/ticker) | Column 4 (Expiration date) |
| **Data Type** | String (uppercase) | String (YYYY-MM-DD format) |
| **Sort Order** | Alphabetical (A-Z) | Chronological (nearest to farthest) |
| **Business Value** | Exposure per underlying | Risk by expiration cycle |
| **Group Header Format** | `QQQ`, `GDX`, `IBIT` | `2025-11-15`, `2025-11-22` |
| **Secondary Sort** | By expiration within ticker | By symbol within expiration |
| **Typical Group Count** | 10-15 tickers | 5-8 expiration dates |
| **User Mental Model** | "What positions do I have in QQQ?" | "What expires this Friday?" |

---

## Related Issues

- Blocks: None
- Blocked by: None
- Related: **ISSUE-018** (DataTables Row Grouping Research - provides pattern and CSS)

---

## Notes

### Business Alignment with TastyTrade Methodology

Grouping by expiration aligns with TastyTrade's emphasis on:
1. **Time decay management** - Theta decay accelerates as expiration approaches
2. **Expiration risk assessment** - Concentration risk per expiration cycle
3. **Rolling strategies** - Plan rolls by identifying positions expiring in next 1-2 weeks
4. **Weekly vs monthly cycles** - Visual separation of weekly (Friday) vs monthly expirations

### Date Format Considerations

The existing `getExpirationDateString()` returns dates in **YYYY-MM-DD format** (ISO 8601):
- **Naturally sortable** as strings (no date parsing needed in JavaScript)
- **Human-readable** (clear, unambiguous date representation)
- **Consistent** with backend `LocalDate` format
- **No timezone issues** (dates only, no time component)

### Performance Characteristics

Expected performance improvements compared to ticker grouping:
- **Fewer groups** (5-8 expiration dates vs 10-15 tickers)
- **Better clustering** (weekly options expire on Fridays, creating natural grouping)
- **Faster redraws** (fewer group headers to insert)
- **Better pagination behavior** (expiration groups less likely to span pages)

### Future Enhancements

Potential Phase 2+ enhancements:
1. **Days until expiration in group headers** - Show "2025-11-15 (5 days)" in header
2. **Aggregate statistics** - Show total positions, delta, theta per expiration group
3. **Color-coded urgency** - Red for positions expiring within 7 days, yellow for 7-14 days
4. **Collapsible groups** - Expand/collapse expiration groups to reduce visual clutter
5. **Multi-level grouping** - Expiration → Symbol (show all QQQ positions expiring 2025-11-15)

### Testing Strategy

1. **Manual Testing**:
   - Verify with current production data (multiple expiration dates)
   - Test pagination with "All" page length option
   - Validate click handlers (both group headers and position rows)
   - Cross-browser testing (Chrome, Firefox, Safari)

2. **Edge Cases**:
   - Single expiration date (should still show group header)
   - Empty table (no crash, graceful handling)
   - Very long expiration lists (50+ different dates - unlikely but test pagination)

3. **Performance Testing**:
   - Load test with 100+ positions across 20+ expiration dates
   - Monitor redraw times (should be <100ms)
   - Check for memory leaks during repeated redraws

### Rollback Plan

If issues arise:
1. Revert `tradelog_jte.jte` lines 420-427 to original configuration
2. Remove group-related event handlers
3. CSS changes are non-breaking (can remain)
4. No database rollback needed (client-side only)

### Next Steps After Completion

1. **User feedback** - Gather feedback from traders using the feature
2. **Consider toggle** - Add UI control to switch between ticker and expiration grouping
3. **Extend to other tables** - Apply to Closed Positions, Weekly Open Positions
4. **Document in user guide** - Update documentation with new grouping feature
