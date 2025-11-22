# ISSUE-028: Virtual Position What-If Calculator

**Created**: 2025-11-15 (Session 9)
**Status**: CLOSED
**Completed**: 2025-11-22 PROGRESS
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

When users want to explore hypothetical position scenarios in the Position Calculator, they must either:
1. Create positions in the database (polluting real data)
2. Manually track what-if scenarios outside the system
3. Lose portfolio-level calculation context (current calculateSinglePosition ignores open positions)

Users need to test "what if I add this position?" scenarios while seeing the impact on their overall portfolio metrics (ROI, P&L, probability).

---

## Root Cause

The current Position Calculator has two distinct calculation modes:
- `calculateSinglePosition()`: Isolated calculation without portfolio context
- `calculatePosition()`: Portfolio aggregation from database positions only

There's no mechanism to create temporary/virtual positions that participate in portfolio calculations without database persistence.

---

## Approach

Implement session-scoped virtual position functionality:

1. **VirtualPositionService**: Session-scoped service to store single virtual OptionEntity
2. **OptionService modification**: Merge virtual position into open positions during `calculatePosition()`
3. **PositionController updates**: Store form inputs as virtual position, use portfolio calculation
4. **UI enhancement**: Separate "What-If Scenario" section showing virtual position with visual distinction
5. **Session storage**: Virtual position persists during user session, cleared on logout/timeout

Technical implementation:
- Virtual positions: `id=null`, `status=PENDING`, temporary `conid` (timestamp-based)
- Replace previous virtual on each calculation (single scenario at a time)
- Include virtual in position-weighted ROI, probability, P&L aggregations
- No database persistence

---

## Success Criteria

- [x] VirtualPositionService created with session scope
- [x] Virtual positions merged into portfolio calculations
- [x] PositionController stores form inputs as virtual position
- [x] Position form template displays virtual position in separate section
- [x] Virtual position visually distinguished from real positions
- [x] "Clear Scenario" functionality implemented
- [x] Unit tests for VirtualPositionService
- [x] Integration tests verify virtual position affects portfolio metrics
- [x] Virtual position persists across page navigation within session
- [x] Virtual position excluded from database queries

---

## Acceptance Tests

```java
@Test
void testVirtualPositionIncludedInPortfolioCalculations() {
    // Given: 2 real open positions with known ROI
    OptionEntity real1 = createOption("SPY", 100.0, 5.0); // 5% ROI
    OptionEntity real2 = createOption("SPY", 200.0, 10.0); // 5% ROI
    when(optionRepository.findAllOpenByTicker("SPY")).thenReturn(List.of(real1, real2));

    // And: Virtual position with different ROI
    OptionEntity virtual = createOption("SPY", 100.0, 15.0); // 15% ROI
    when(virtualPositionService.getVirtualPosition("SPY")).thenReturn(Optional.of(virtual));

    // When: Calculate position
    PositionDto result = optionService.calculatePosition(new PositionDto("SPY"));

    // Then: Portfolio ROI includes virtual position (weighted average)
    // (100*5 + 200*5 + 100*15) / 400 = 7.5%
    assertThat(result.getAnnualizedRoiPercent()).isCloseTo(7.5, within(0.1));
}

@Test
void testVirtualPositionDisplayedSeparately() {
    // Given: Virtual position in session
    OptionEntity virtual = createVirtualOption("SPY");
    session.setAttribute("virtualPosition", virtual);

    // When: Load position form
    String html = controller.calculatePosition(form, model, session);

    // Then: Virtual position shown in "What-If Scenario" section
    assertThat(model.getAttribute("virtualPosition")).isEqualTo(virtual);
    assertThat(html).contains("what-if-scenario");
}

@Test
void testClearVirtualPosition() {
    // Given: Virtual position in session
    session.setAttribute("virtualPosition", createVirtualOption("SPY"));

    // When: Clear virtual position
    controller.clearVirtualPosition(session);

    // Then: Session no longer contains virtual position
    assertThat(session.getAttribute("virtualPosition")).isNull();
}
```

---

## Related Issues

- Related: ISSUE-026 (Position Calculator Manual Recalculation)
- Related: ISSUE-027 (Position Calculator Recalculate Button)

---

## Notes

**User Requirements** (from clarification):
- Virtual position included in portfolio calculations (aggregated ROI, P&L)
- Displayed in separate "What-If Scenario" section
- Replace previous virtual on each calculation (single scenario)
- HTTP session storage (survives navigation, cleared on session end)

**Design Decisions**:
- Session-scoped storage chosen over request-scoped to persist across navigation
- Single virtual position (vs. multiple scenarios) reduces complexity
- Separate UI section provides clear visual distinction
- Position-weighted calculations automatically include virtual without special logic

**Files Modified**:
- NEW: `src/main/java/co/grtk/srcprofit/service/VirtualPositionService.java`
- MODIFIED: `src/main/java/co/grtk/srcprofit/service/OptionService.java`
- MODIFIED: `src/main/java/co/grtk/srcprofit/controller/PositionController.java`
- MODIFIED: `src/main/jte/position-form_jte.jte`
- NEW: `src/test/java/co/grtk/srcprofit/service/VirtualPositionServiceTest.java`
- MODIFIED: `src/test/java/co/grtk/srcprofit/service/OptionServiceTest.java`
