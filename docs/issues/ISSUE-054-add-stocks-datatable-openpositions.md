# ISSUE-054: Add Stocks DataTable to Open Positions Page

**Created**: 2025-12-21
**Status**: CLOSED
**Completed**: 2025-12-21
**Priority**: MEDIUM
**Category**: Feature / UI Enhancement
**Blocking**: N/A

---

## Problem

The Open Positions page currently displays only option positions. Stock positions are imported from IBKR Flex Reports but not visible in the UI. Users cannot view individual stock holdings, P&L, cost basis, or portfolio weight.

---

## Root Cause

Stock positions exist in `OpenPositionEntity` with `assetClass = 'STK'` but the UI only queries and displays options via `findAllOptionsWithUnderlying()`.

---

## Approach

Add a stocks datatable below the existing options table on `/openpositions` page, following established datatable patterns.

**Steps**:
1. Create `StockPositionViewDto` with fields: id, symbol, tradeDate, quantity, costBasisMoney, markPrice, positionValue, pnl, percentOfNAV
2. Add `OpenPositionService.getAllStockPositionViewDtos()` method
3. Update `OpenPositionsController` to fetch and pass stocks to view
4. Add stocks datatable to `openpositions_jte.jte` template
5. Add JavaScript initialization for DataTable
6. Add controller tests

---

## Success Criteria

- [x] StockPositionViewDto created
- [x] Service method returns stock positions with instrument data
- [x] Controller passes stocks to template
- [x] Stocks table displays below options table
- [x] Columns: ID, Symbol, TradeDate, Qty, CostBasis, Price, Value, P&L, % NAV
- [x] Currency formatting applied
- [x] Red background on ID when P&L < 0
- [x] Alphabetical sort by symbol
- [x] Row click navigates to position detail
- [x] Tests passing (11/11 passed)
- [x] Build succeeds

---

## Acceptance Tests

```java
@Test
void testOpenPositionsLoadsStockData() {
    List<StockPositionViewDto> mockStocks = List.of(
        new StockPositionViewDto(1L, "AAPL", LocalDate.of(2025, 1, 15), 100,
            15000.0, 155.50, 15550.0, 550.0, 5.2)
    );
    when(openPositionService.getAllStockPositionViewDtos()).thenReturn(mockStocks);

    String viewName = openPositionsController.openPositions(model);

    assertEquals("openpositions_jte", viewName);
    verify(model).addAttribute("stockPositions", mockStocks);
}
```

---

## Related Issues

- Related: ISSUE-047 - New OpenPositions Menu Page
- Related: ISSUE-041 - JPA Relationships Between OpenPositionEntity and InstrumentEntity
- Related: ISSUE-039 - IBKR Flex Open Positions Import

---

## Notes

**Repository query exists**: `OpenPositionRepository.findAllStocksWithInstrument()` (lines 122-126)

**Fields to display**: Symbol, Qty, Cost Basis, Current Price, P&L, Trade Date, % of NAV

**Sorting**: Simple alphabetical by symbol
