# ISSUE-008: Create TradeLogController and Separate Trade Log Functionality

**Created**: 2025-11-07 (Session TBD)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

`PositionController` currently mixes two distinct concerns:
1. **Trade Log Viewing**: Displaying positions/trade history (`/positions`, `/getPositionsFromDate`)
2. **Position Calculation**: Calculator tool for analyzing positions (`/calculatePosition`, `/getPosition/{ticker}`)

This violates the Single Responsibility Principle and makes the controller harder to maintain and test.

---

## Root Cause

When the positions page was initially implemented, all position-related endpoints were grouped in one controller. As features evolved, the distinction between "viewing trade history" and "calculating position metrics" became clear, but the code organization didn't reflect this separation.

---

## Approach

1. Create new `TradeLogController` with:
   - `GET /positions` endpoint
   - `POST /getPositionsFromDate` endpoint
   - `fillPositionsPage()` helper method
   - Related model attribute constants

2. Move from `PositionController` to `TradeLogController`:
   - Method: `positions()` (line 76-81)
   - Method: `getPositionsFromDate()` (line 83-91)
   - Method: `fillPositionsPage()` (line 103-117)
   - Constants: `POSITIONS_PAGE_PATH`, model attribute constants for open/closed positions

3. Inject required services into `TradeLogController`:
   - `OptionService`
   - `InstrumentService`
   - `AlpacaService`
   - `NetAssetValueService`

4. Update navigation references:
   - `/src/main/jte/index_jte.jte` (line 99): sidebar nav - no changes needed (URL stays same)
   - `/src/main/jte/index_jte.jte` (line 54): search form - no changes needed (URL stays same)

5. Refactor `PositionController` to contain only:
   - Position calculator endpoints (`/calculatePosition` GET/POST)
   - Individual position detail view (`/getPosition/{ticker}`)
   - `fillPositionForm()` helper method
   - `getMarketValue()` helper method

---

## Success Criteria

- [ ] New `TradeLogController` created in `src/main/java/co/grtk/srcprofit/controller/`
- [ ] Both endpoints (`/positions`, `/getPositionsFromDate`) moved and working
- [ ] `fillPositionsPage()` and related constants moved to new controller
- [ ] Services injected correctly into `TradeLogController`
- [ ] `PositionController` refactored to contain only calculation-related endpoints
- [ ] All existing navigation and views continue to work without changes
- [ ] Unit tests created for `TradeLogController`
- [ ] No breaking changes to API endpoints (URLs remain the same)
- [ ] Code builds successfully with no compilation errors
- [ ] All existing tests pass

---

## Acceptance Tests

```java
@Test
public void testTradeLogControllerExists() {
    assertNotNull(tradeLogController);
    assertTrue(TradeLogController.class.isAnnotationPresent(RestController.class));
}

@Test
public void testPositionsEndpointMovedToTradeLogController() {
    // GET /positions should return trade log view
    mockMvc.perform(get("/positions"))
        .andExpect(status().isOk())
        .andExpect(view().name("positions_jte"));
}

@Test
public void testGetPositionsFromDateEndpointMovedToTradeLogController() {
    // POST /getPositionsFromDate should return filtered trade log
    mockMvc.perform(post("/getPositionsFromDate")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("fromDate", "2025-01-01"))
        .andExpect(status().isOk())
        .andExpect(view().name("positions_jte"));
}

@Test
public void testPositionControllerStillHasCalculator() {
    // POST /calculatePosition should still work
    mockMvc.perform(post("/calculatePosition")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk());
}
```

---

## Related Issues

- None currently

---

## Notes

**Code Volume**:
- New `TradeLogController`: ~150-200 lines
- Refactored `PositionController`: ~110-130 lines (from ~146)
- No changes to template files (`positions_jte.jte` - 405 lines)
- No changes to DTO files (`PositionDto`)

**Implementation Steps**:
1. Create new controller class
2. Copy relevant methods from `PositionController`
3. Inject required services
4. Update `PositionController` to remove moved code
5. Run full test suite to ensure no regressions
6. Verify all endpoints still respond correctly

**Testing Considerations**:
- Test both endpoints in isolation with `TradeLogController`
- Verify `PositionController` still has calculator endpoints
- Integration tests to ensure UI navigation still works
- No frontend changes required (URLs remain the same)
