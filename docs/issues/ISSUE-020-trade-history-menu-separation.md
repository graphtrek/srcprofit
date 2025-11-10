# ISSUE-020: Trade History Menu Separation

**Created**: 2025-11-10 (Session 6)
**Completed**: 2025-11-10 (Session 6)
**Status**: CLOSED
**Priority**: HIGH
**Category**: Feature / Code Quality
**Blocking**: None

---

## Problem

The Trade Log page currently displays both Open Positions and Closed Positions in a single view. This creates a cluttered interface and makes it difficult to focus on active trading activity versus historical performance. Users need a dedicated Trade History page to review closed positions separately.

---

## Root Cause

The initial implementation combined all position types (open and closed) into one "Trade Log" page for simplicity. As the application grows, separating concerns improves usability and aligns with the principle of single responsibility for UI pages.

---

## Approach

Create a new "Trade History" menu item and page that contains all Closed Positions UI elements, following the pattern established in ISSUE-008 (Trade Log Controller Separation).

### Backend Changes

1. **Create TradeHistoryController.java**
   - New endpoint: `GET /tradehistory` - renders trade_history_jte.jte
   - New endpoint: `POST /tradehistoryFromDate` - filters by date
   - Inject: `OptionService` (reuse existing `getAllClosedOptions()` method)
   - Model attribute: `"closedPositions"` containing `List<PositionDto>`

2. **Update TradeLogController.java**
   - Rename endpoints: `/tradelog` → `/activetrades`, `/tradelogFromDate` → `/activetradesFromDate`
   - Remove closed positions logic (lines 62-63 in `fillTradeLogPage()`)
   - Keep only open positions functionality

### Frontend Changes

1. **Create trade_history_jte.jte**
   - Copy Closed Positions table from tradelog_jte.jte (lines 325-396)
   - Copy DataTable JS initialization (lines 480-491)
   - Add header search bar for date filtering
   - Implement filter dropdown with options: "This Week", "This Month", "This Year", "All"
   - Update model parameter to `@param List<PositionDto> closedPositions`

2. **Update index_jte.jte (Navigation)**
   - Rename menu item: "Trade Log" → "Active Trades"
   - Update endpoint: `hx-get="/tradelog"` → `hx-get="/activetrades"`
   - Add new menu item: "Trade History" (below Active Trades)
     - Icon: `bi bi-archive`
     - Endpoint: `hx-get="/tradehistory"`

3. **Update tradelog_jte.jte**
   - Remove Closed Positions section (lines 325-396)
   - Remove Closed Positions DataTable JS (lines 480-491)
   - Update page title to "Active Trades" if desired
   - Keep: Open Positions table, Weekly Buy Obligations

### Filter Dropdown Implementation

Implement date filtering for "This Week", "This Month", "This Year", "All":

```javascript
$('#tradeHistoryFilter').on('click', '.dropdown-item', function() {
    const filter = $(this).data('filter');
    let startDate;
    const today = new Date();

    switch(filter) {
        case 'week':
            startDate = new Date(today.setDate(today.getDate() - 7));
            break;
        case 'month':
            startDate = new Date(today.setMonth(today.getMonth() - 1));
            break;
        case 'year':
            startDate = new Date(today.setFullYear(today.getFullYear() - 1));
            break;
        case 'all':
            startDate = null;
            break;
    }

    if (startDate) {
        htmx.ajax('POST', '/tradehistoryFromDate', {
            target: '#main',
            swap: 'innerHTML',
            values: { tradeDate: startDate.toISOString().split('T')[0] }
        });
    } else {
        htmx.ajax('GET', '/tradehistory', '#main');
    }
});
```

---

## Success Criteria

- [x] New `TradeHistoryController.java` created with `/tradehistory` and `/tradehistoryFromDate` endpoints
- [x] New `trade_history_jte.jte` template created with Closed Positions table
- [x] `TradeLogController` updated to remove closed positions logic and rename endpoints
- [x] `tradelog_jte.jte` updated to remove Closed Positions section
- [x] Navigation menu updated with "Active Trades" and "Trade History" items
- [x] Filter dropdown implemented in Trade History page ("This Week", "This Month", "This Year", "All")
- [x] Header search bar works on Trade History page for date filtering
- [x] DataTables initialization works correctly on both pages (no ID conflicts)
- [x] Row click handler navigates to `/getPosition/:ticker` on Trade History
- [x] Unit tests created for `TradeHistoryController`
- [x] Existing `TradeLogControllerTest` updated to verify closed positions are NOT returned
- [x] Manual testing confirms both pages load correctly and independently

---

## Acceptance Tests

```java
@Test
void testTradeHistoryEndpoint() {
    // Verify Trade History endpoint returns only closed positions
    mockMvc.perform(get("/tradehistory"))
        .andExpect(status().isOk())
        .andExpect(view().name("trade_history_jte"))
        .andExpect(model().attributeExists("closedPositions"));

    verify(optionService).getAllClosedOptions(null);
}

@Test
void testTradeHistoryFromDateEndpoint() {
    // Verify date filtering works
    LocalDate filterDate = LocalDate.of(2025, 1, 1);

    mockMvc.perform(post("/tradehistoryFromDate")
            .param("tradeDate", "2025-01-01"))
        .andExpect(status().isOk())
        .andExpect(view().name("trade_history_jte"));

    verify(optionService).getAllClosedOptions(filterDate);
}

@Test
void testActiveTradesDoesNotReturnClosedPositions() {
    // Verify Active Trades (formerly Trade Log) only returns open positions
    mockMvc.perform(get("/activetrades"))
        .andExpect(status().isOk())
        .andExpect(view().name("tradelog_jte"));

    verify(optionService).getAllOpenPositions(any());
    verify(optionService, never()).getAllClosedOptions(any());
}
```

---

## Related Issues

- Related: ISSUE-008 (Trade Log Controller Separation) - Similar pattern for controller/UI separation
- Related: ISSUE-019 (DataTables Expiration Grouping) - Ensure no DataTable ID conflicts

---

## Notes

### Design Decisions (from user input)
- Menu naming: "Active Trades" (existing) + "Trade History" (new)
- Header search bar: Duplicate on both pages for independent date filtering
- Filter dropdown: Implement properly with "This Week", "This Month", "This Year", "All"

### Files to Create
- `src/main/java/co/grtk/srcprofit/controller/TradeHistoryController.java`
- `src/main/jte/trade_history_jte.jte`
- `src/test/java/co/grtk/srcprofit/controller/TradeHistoryControllerTest.java`

### Files to Modify
- `src/main/jte/index_jte.jte` (navigation menu)
- `src/main/jte/tradelog_jte.jte` (remove Closed Positions section)
- `src/main/java/co/grtk/srcprofit/controller/TradeLogController.java` (remove closed logic, rename endpoints)
- `src/test/java/co/grtk/srcprofit/controller/TradeLogControllerTest.java` (update tests)

### Service/Repository Reuse
- `OptionService.getAllClosedOptions(LocalDate startDate)` - already exists, no changes needed
- `OptionRepository.findAllClosed()` and `findAllClosedFromTradeDate()` - already exist

### Estimated Effort
2-3 hours (controller + template + tests + manual verification)
