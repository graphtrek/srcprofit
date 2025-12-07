# ISSUE-047: New OpenPositions Menu Page

**Created**: 2025-12-07
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

Currently, there is no dedicated page to view open positions in a simplified, focused manner. While TradeLog shows all open options with extensive summary cards and multiple tables, users need a simpler view showing just the essential open position data with a single DataTable containing key fields.

---

## Root Cause

The application lacks a focused "Open Positions" menu item and dedicated view page. TradeLog serves as the current default, but it's designed for broader portfolio analysis rather than a quick review of open option positions.

---

## Approach

Create a new `/openpositions` page following the same patterns as TradeLog but simplified:

1. **DTO Layer**: Create `OpenPositionViewDto` with only the fields needed for display
2. **Service Layer**: Add `getAllOpenPositionViewDtos()` method to OpenPositionService
3. **Controller**: Create OpenPositionsController with `/openpositions` endpoint
4. **View**: Create `openpositions_jte.jte` template with single DataTable
5. **Navigation**: Add "Open Positions" menu item to sidebar (before Trade Log)
6. **Testing**: Unit tests for controller

**Data Source**: OpenPositionEntity via OpenPositionRepository, with related InstrumentEntity for underlying prices.

---

## Success Criteria

- [ ] OpenPositionViewDto created with all 12 required fields
- [ ] OpenPositionService.getAllOpenPositionViewDtos() method implemented
- [ ] OpenPositionsController with GET /openpositions endpoint
- [ ] openpositions_jte.jte template renders single DataTable with proper formatting
- [ ] "Open Positions" menu item appears in sidebar before Trade Log
- [ ] Unit tests cover controller functionality
- [ ] Build passes (mvn clean compile)
- [ ] All tests pass (mvn test)
- [ ] Row clicks navigate to position details (/getPosition/{symbol})

---

## Acceptance Tests

```java
@SpringBootTest
public class OpenPositionsControllerTest {

    @Test
    void testOpenPositionsEndpoint_ReturnsCorrectView() {
        // GET /openpositions returns openpositions_jte view
        // Model contains openPositions attribute
        // openPositions list has correct data structure
    }

    @Test
    void testOpenPositionViewDto_HasAllFields() {
        // Verify DTO has: id, symbol, tradeDate, expirationDate, daysLeft, qty,
        // strikePrice, underlyingPrice, pnl, roi, pop, type
    }

    @Test
    void testDataTable_FormatsMonetaryValues() {
        // strikePrice, underlyingPrice, P&L formatted as currency
        // ROI, POP formatted as percentages
    }
}
```

---

## DataTable Columns

| Column | Format | Source |
|--------|--------|--------|
| id | Number | OpenPositionEntity.id |
| symbol | Text | OpenPositionEntity.underlyingSymbol |
| tradeDate | Date | OpenPositionEntity.reportDate |
| expirationDate | Date | OpenPositionEntity.expirationDate |
| daysLeft | Number | Calculated |
| qty | Number | OpenPositionEntity.quantity |
| strikePrice | Currency | OpenPositionEntity.strike |
| underlyingPrice | Currency | InstrumentEntity.price |
| P&L | Currency (red if negative) | OpenPositionEntity.fifoPnlUnrealized |
| ROI | Percentage | Calculated: annualizedRoiPercent |
| POP | Percentage | Calculated: probability |
| type | Text (PUT/CALL) | OpenPositionEntity.putCall |

---

## Files to Create/Modify

| Action | File | Purpose |
|--------|------|---------|
| CREATE | `src/main/java/co/grtk/srcprofit/dto/OpenPositionViewDto.java` | DTO for view layer |
| MODIFY | `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java` | Add getAllOpenPositionViewDtos() method |
| CREATE | `src/main/java/co/grtk/srcprofit/controller/OpenPositionsController.java` | Handle /openpositions requests |
| CREATE | `src/main/jte/openpositions_jte.jte` | JTE template with DataTable |
| MODIFY | `src/main/jte/index_jte.jte` | Add menu navigation item |
| CREATE | `src/test/java/co/grtk/srcprofit/controller/OpenPositionsControllerTest.java` | Unit tests |

---

## Related Issues

- Related: ISSUE-045 (Migrated controllers to OpenPositionService.getAllOpenOptionDtos)
- Related: ISSUE-044 (Added getAllOpenOptionDtos to OpenPositionService)

---

## Notes

- Use existing calculation helpers from PositionCalculationHelper for daysLeft, ROI, POP
- Follow TradeLog template patterns for consistency
- Menu item should use icon `bi-folder2-open`
- Use HTMX for menu integration (hx-get, hx-trigger, hx-target, hx-swap)
- Row click handler should navigate to `/getPosition/{symbol}`
- Negative P&L should have red background styling for visual prominence
