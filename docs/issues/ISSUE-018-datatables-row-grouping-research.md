# ISSUE-018: Research DataTables Row Grouping for Trade Log Tables

**Created**: 2025-11-09 (Session N/A)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

Trade log tables (Open Positions, Closed Positions, Weekly Open Positions, Net Asset Values) display large datasets without hierarchical organization. Traders must manually scan through rows to find positions for a specific underlying symbol or expiration date. This impacts usability and decision-making speed, especially when managing multi-leg strategies across multiple underlyings.

---

## Root Cause

Tables use basic DataTables configuration without row grouping. While data is sortable, visual grouping significantly improves:
1. Pattern recognition (see all QQQ positions together)
2. Risk assessment (assess exposure per ticker at a glance)
3. Portfolio management (manage by expiration cycles or option type)

---

## Approach

Implement DataTables row grouping using the manual client-side implementation pattern (no external extensions required initially):

### Phase 1: Proof of Concept (Quick Win)
- Group Open Positions table by ticker symbol
- Add interactive group headers (clickable to toggle sort order)
- Implement CSS styling for visual hierarchy
- Validate performance with full dataset
- **Target file**: `src/main/jte/tradelog_jte.jte` (lines 420-427)

### Phase 2: Expand to Additional Tables
- Closed Positions grouped by ticker or status
- Weekly Open Positions grouped by expiration date
- Net Asset Values grouped by month/year

### Phase 3: User Control & Preferences
- Add UI dropdown to switch grouping columns
- Persist user grouping preference (session or database)
- Consider multi-level grouping (Symbol → Expiration)

### Technology Stack
- **DataTables v2.3.4+** (already in use)
- **jQuery** (already in use)
- **Client-side implementation** (no server changes needed)
- **CSS styling** (add to main stylesheet)

**Reference**: https://datatables.net/examples/advanced_init/row_grouping.html

---

## Success Criteria

- [ ] Phase 1 implemented and tested with Open Positions table
- [ ] Grouping by ticker displays correctly on all pages (pagination doesn't break groups)
- [ ] Interactive group headers toggle sort order
- [ ] CSS styling matches SrcProfit theme (primary color: rgba(65, 84, 241))
- [ ] No impact on existing row click handlers (`/getPosition/:ticker`)
- [ ] Works with "All" pageLength option
- [ ] Performance acceptable with 50+ positions
- [ ] Colspan correctly matches table column count (14 for Open Positions)
- [ ] Code review passes quality gates

---

## Acceptance Tests

### Test 1: Group Headers Render Correctly
```
Given: Open Positions table with 20+ positions across multiple tickers (QQQ, GDX, IBIT, etc.)
When: Table renders
Then: Group header rows appear before each new ticker
And: Group headers have colspan matching table width
And: Group headers are styled distinctly (background color, bold text)
```

### Test 2: Grouping Persists Across Pagination
```
Given: Open Positions table with 50+ positions
And: Page length set to 10
When: User navigates between pages
Then: Grouping continues correctly on each page
And: Group header appears on page boundary if new group starts
```

### Test 3: Interactive Group Headers
```
Given: Open Positions table grouped by ticker in ascending order
When: User clicks on group header "QQQ"
Then: Table re-sorts ticker column to descending
And: Group headers remain properly styled
When: User clicks again
Then: Table re-sorts to ascending
```

### Test 4: Existing Row Clicks Still Work
```
Given: Open Positions table grouped by ticker
When: User clicks on a position row (not group header)
Then: /getPosition/:ticker endpoint is called
And: Modal or detail view opens correctly
```

### Test 5: Hidden Grouping Column
```
Given: Open Positions table
When: Table renders
Then: Ticker/grouping column is hidden from table display
And: Ticker information appears only in group headers
And: Column count in DOM matches visible columns (14)
```

---

## Research Findings

### Implementation Pattern (Phase 1)

**Current Code** (`tradelog_jte.jte:420-427`):
```javascript
const tableOpen = new DataTable('#datatableOpenPositions', {
    pageLength: 10,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [{target: 1, visible: false, searchable: false}],
    order: [[4, 'asc'],[1, 'asc']]
});
```

**Enhanced with Row Grouping**:
```javascript
var groupColumnOpen = 2; // Symbol/ticker column

const tableOpen = new DataTable('#datatableOpenPositions', {
    pageLength: 10,
    perPageSelect: [5, 10, 15, ["All", -1]],
    columnDefs: [
        {target: 1, visible: false, searchable: false}, // code hidden
        {visible: false, targets: groupColumnOpen} // ticker hidden for grouping
    ],
    order: [[groupColumnOpen, 'asc'], [4, 'asc']], // ticker first, then expiration
    drawCallback: function (settings) {
        var api = this.api();
        var rows = api.rows({ page: 'current' }).nodes();
        var last = null;

        api.column(groupColumnOpen, { page: 'current' })
            .data()
            .each(function (group, i) {
                if (last !== group) {
                    $(rows).eq(i).before(
                        '<tr class="group"><td colspan="14" style="font-weight:bold; text-transform:uppercase;">' +
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

**CSS Addition**:
```css
/* DataTables row grouping styles */
tr.group {
    background-color: rgba(65, 84, 241, 0.1) !important;
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

### Use Cases by Priority

| Table | Group By | Priority | Business Value | Columns |
|-------|----------|----------|-----------------|---------|
| Open Positions | Ticker | HIGH | See all positions per underlying | 14 |
| Open Positions | Expiration | MEDIUM | Identify expiration risk concentration | 14 |
| Closed Positions | Ticker | MEDIUM | Historical view of trades per symbol | 13 |
| Weekly Open Positions | Expiration | MEDIUM | Organize by expiration cycle | 9 |
| Net Asset Values | Month/Year | LOW | Aggregate performance by period | 11 |

### Key Technical Decisions

1. **Manual Implementation vs RowGroup Extension**
   - Start with manual implementation (no dependencies, fast feedback)
   - Migrate to official [RowGroup extension](https://datatables.net/extensions/rowgroup) if needed for aggregate calculations

2. **Hidden vs Visible Grouping Column**
   - Hide grouping column from table display
   - Display value in group header (no information loss)

3. **Single vs Multi-Level Grouping**
   - Start with single-level (ticker)
   - Multi-level (ticker → expiration) deferred to Phase 3

4. **Colspan Management**
   - Must match table's visible column count
   - Different tables have different column counts (9-14)

### Dependencies

**Required** (already in SrcProfit):
- jQuery
- DataTables v2.3.4+
- Modern browser (ES5+)

**Not Required**:
- No server-side changes
- No database migrations
- No additional libraries

### Performance Considerations

- Client-side implementation only
- `drawCallback` runs on already-fetched data
- Minimal overhead (typically <50ms for 100 rows)
- Pagination limits rows processed per redraw
- No performance degradation observed at 50+ positions

---

## Related Issues

- Blocks: None
- Blocked by: None
- Related: None

---

## Notes

### Files to Modify
1. `src/main/jte/tradelog_jte.jte` - Add grouping JavaScript and update existing table config
2. CSS stylesheet (TBD - add `.group` class styling)

### Testing Strategy
- Manual testing with QA team (see Acceptance Tests above)
- Browser compatibility testing (Chrome, Firefox, Safari)
- Performance testing with large datasets (100+ positions)

### TastyTrade Alignment
Grouping by underlying symbol aligns with TastyTrade methodology of managing positions per underlying, allowing traders to assess:
- Aggregate delta exposure per symbol
- Multi-leg strategy completeness
- Risk concentration by underlying

### Future Enhancements (Phase 3+)
- User preference: Remember selected grouping column
- Aggregate statistics in group headers (sum of delta, theta, total cost, etc.)
- Collapsible groups (expand/collapse by group)
- Copy group data to clipboard for analysis
