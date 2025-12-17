# ISSUE-050: Move Buy/Sell Cards and Weekly Obligations from TradeLog to Dashboard

**Created**: 2025-12-17
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature / UI Enhancement
**Blocking**: None

---

## Problem

The Dashboard currently only shows market tickers and charts, while the TradeLog page contains critical summary information (buy/sell obligation cards and weekly obligations table) that would be more useful on the Dashboard as the primary landing page.

Users need to navigate to TradeLog to see:
- 6 summary cards showing buy obligations, sell obligations, and premium values
- Weekly buy obligations table showing positions expiring within a week

This information would provide better visibility if placed on the Dashboard before the collected premium chart.

---

## Root Cause

Historical page organization where TradeLog was the primary view for position monitoring. The Dashboard was later enhanced with charts but lacks the critical summary metrics.

---

## Approach

Reorganize the Dashboard by moving components from TradeLog:

### Components to Move from TradeLog

**Source File**: `src/main/jte/tradelog_jte.jte`

1. **Six Summary Cards** (lines 26-167):
   - Buy Obligation Market Value Card (lines 28-44)
   - Buy Obligation Card (lines 50-72) - with Cash and Stock
   - PUT Premium Card (lines 77-99) - with trade/market price comparison
   - CALL Premium Card (lines 144-166) - with trade/market price comparison
   - Sell Obligation Market Value Card (lines 103-119)
   - Sell Obligation Card (lines 123-140)

2. **Weekly Buy Obligations Table** (lines 169-213):
   - Card wrapper with DataTable
   - Table ID: `datatableWeeklyOpenPositions`
   - Columns: Expiration, DaysLeft, Value, MrktValue, Price, MrktPrice, P&L, R.O.I, P.O.P

### Dashboard Layout Changes

**Target File**: `src/main/jte/dashboard_jte.jte`

**New Structure**:
```
Dashboard
├── Page Title
├── Three Market Ticker Cards (QQQ, GDX, IBIT) - KEEP AS IS
├── Six Summary Cards - INSERT HERE (NEW)
├── Weekly Buy Obligations Table - INSERT HERE (NEW)
└── Collected Premium + NAV Charts - MOVE DOWN
```

**Insertion Point**: After market tickers (line ~137), before chart card (line 143)

### Controller Changes

**Target File**: `src/main/java/co/grtk/srcprofit/controller/HomeController.java`

**Method**: `positions()` (lines 64-97)

**Required Data Additions**:
```java
// Add these to model
1. positionDto (already exists) - needs full calculation
2. weeklyOpenPositions - List<PositionDto> filtered for next 7 days
3. openOptions - List<PositionDto> for full data

// Service calls to add:
List<PositionDto> openOptions = optionService.getAllOpenOptionDtos();
optionService.calculatePosition(positionDto, openOptions, Collections.emptyList());
List<PositionDto> weeklyOpenPositions = optionService.getWeeklyOpenOptionDtos(openOptions);
netAssetValueService.loadLatestNetAssetValue().ifPresent(nav -> {
    positionDto.setCash(nav.getCash());
    positionDto.setStock(nav.getStock());
});
```

### JavaScript/DataTables

**Source**: `tradelog_jte.jte` (lines 306-383)
- Move DataTables initialization for `datatableWeeklyOpenPositions`
- Keep currency formatting logic
- Keep group row functionality (if needed)

**Target**: `dashboard_jte.jte` JavaScript section (after line 355)

---

## Success Criteria

- [x] Six summary cards appear on Dashboard above the charts
- [x] Weekly Buy Obligations table appears on Dashboard above the charts
- [x] All cards display correct data matching TradeLog behavior
- [x] DataTable for Weekly Obligations initializes and functions correctly
- [x] Dashboard loads without errors
- [x] Premium price comparison colors work (red/green indicators)
- [x] Cash and Stock values display correctly in Buy Obligation card
- [x] TradeLog page remains functional (cards and table removed)
- [x] All controller tests pass
- [x] Visual layout is responsive and follows existing dashboard patterns

---

## Implementation Details

### Files to Modify

1. **`src/main/jte/dashboard_jte.jte`**
   - Add 6 summary cards section before chart card
   - Add Weekly Buy Obligations table section
   - Add DataTables initialization for weekly table
   - Update JavaScript section with table init code

2. **`src/main/java/co/grtk/srcprofit/controller/HomeController.java`**
   - Update `positions()` method
   - Add `openOptions` list to model
   - Add `weeklyOpenPositions` list to model
   - Add service calls: `getAllOpenOptionDtos()`, `calculatePosition()`, `getWeeklyOpenOptionDtos()`
   - Add NAV service call for cash/stock values

3. **`src/main/jte/tradelog_jte.jte`**
   - Remove 6 summary cards section (lines 26-167)
   - Remove Weekly Buy Obligations table section (lines 169-213)
   - Remove corresponding DataTables init code for weekly table
   - Keep Open Positions table and its functionality

4. **`src/test/java/co/grtk/srcprofit/controller/HomeControllerTest.java`** (if exists)
   - Update tests to verify new model attributes
   - Add tests for weeklyOpenPositions data
   - Add tests for positionDto calculation

### Data Dependencies

**PositionDto fields needed**:
- `marketValue` - Buy obligation market value
- `positionValue` - Buy obligation value
- `cash` - Cash portion
- `stock` - Stock portion
- `put` - PUT premium (trade price)
- `putMarketPrice` - PUT premium (market)
- `call` - CALL premium (trade price)
- `callMarketPrice` - CALL premium (market)
- `callObligationValue` - Sell obligation value
- `callObligationMarketValue` - Sell obligation market value

**List<PositionDto> weeklyOpenPositions fields per row**:
- `expirationDate` / `expirationDateString`
- `daysLeft`
- `positionValue`
- `marketValue`
- `tradePrice`
- `marketPrice`
- `unRealizedProfitOrLoss`
- `annualizedRoiPercent`
- `probability`

### Service Layer (No Changes Required)

- `OptionService.getAllOpenOptionDtos()` - Already exists
- `OptionService.calculatePosition()` - Already exists
- `OptionService.getWeeklyOpenOptionDtos()` - Already exists
- `NetAssetValueService.loadLatestNetAssetValue()` - Already exists

---

## Acceptance Tests

Manual testing checklist:

1. **Dashboard Page Load**
   - Navigate to `/dashboard/week`
   - Verify all 6 cards render with correct values
   - Verify Weekly Obligations table renders below cards
   - Verify Collected Premium chart appears below table

2. **Card Data Validation**
   - Compare card values to TradeLog page (should match)
   - Verify color indicators (green/red) work for premium cards
   - Verify Cash and Stock values display in Buy Obligation card

3. **Weekly Table Validation**
   - Verify table contains only positions expiring within 7 days
   - Verify DataTable sorting and filtering work
   - Verify currency formatting displays correctly

4. **TradeLog Page**
   - Navigate to `/tradelog`
   - Verify cards and weekly table are removed
   - Verify Open Positions table still works

5. **Responsive Layout**
   - Test on mobile/tablet/desktop viewports
   - Verify Bootstrap grid layout adapts correctly

---

## Related Issues

- Related: ISSUE-047 - New OpenPositions Menu Page (shows alternative views)
- Related: ISSUE-008 - Dashboard NaN fix (similar dashboard improvements)

---

## Notes

### Design Considerations

1. **Card Grid Layout**: Use same 3-column responsive grid as TradeLog (`col-xxl-4 col-md-6`)
2. **Icon Consistency**: Keep existing icons (bi-inbox, bi-bookmark-check, bi-currency-dollar, etc.)
3. **Color Scheme**: Maintain Bootstrap color classes (success, danger, secondary)
4. **DataTables Version**: Ensure compatibility with existing DataTables initialization patterns

### Alternative Approaches Considered

**Option A** (Chosen): Move components to Dashboard
- Pros: Better visibility, centralized information
- Cons: Dashboard becomes longer page

**Option B**: Keep on TradeLog, add summary widget to Dashboard
- Pros: Smaller dashboard
- Cons: Duplication of code and data

**Option C**: Create new "Overview" page
- Pros: Clean separation
- Cons: Extra navigation step

### Technical Notes

- Bootstrap grid classes handle responsive layout automatically
- ApexCharts position will shift down but remain functional
- No database changes required (pure UI reorganization)
- All data already available through existing service layer
- TradeLog will focus purely on detailed position table view
