# ISSUE-056: Add Latest Net Asset Value Fields to Dashboard Chart Section

**Created**: 2025-12-22 (Session N/A)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature / UI Enhancement
**Blocking**: N/A

---

## Problem

The dashboard page displays the Net Asset Values chart but doesn't show the latest values for each component (premium, total, cash, stocks, options) inline near the chart title. Users must hover over the chart to see current values, which is not efficient for quick status checks.

---

## Root Cause

The current implementation at line 387 in `dashboard_jte.jte` only shows the total collected premium in the chart section title. There's no display of the latest values for total, cash, stocks, and options components.

**Current implementation**:
```html
<h5 class="card-title">Premium <span class="fs-5 currency-usd">${positionDto.getCollectedPremium()}</span></h5>
```

---

## Approach

Add inline display of latest net asset value components at line 387 in `dashboard_jte.jte`:

1. **Update chart section title** to display all NAV components with corresponding chart colors:
   - Premium: yellow (new color, currently green '#2eca6a')
   - Total: green '#2eca6a'
   - Cash: orange '#ff771d'
   - Stock: blue '#4154f1'
   - Options: red '#f14161'

2. **Use color-coded badges** matching the chart series colors for visual consistency

3. **Data sources** (from DashboardDto or PositionDto):
   - Premium: `positionDto.getCollectedPremium()` (already available)
   - Total: Latest value from `dailyTotals` array
   - Cash: Latest value from `dailyCash` array
   - Stock: Latest value from `dailyStock` array
   - Options: Latest value from `dailyOptions` array

4. **Backend changes** (if needed):
   - Verify that latest NAV values are exposed in the model
   - May need to add getter methods to DashboardDto for latest values

---

## Success Criteria

- [x] Premium color changed to yellow in chart colors array (line 512)
- [ ] Latest NAV values displayed inline at line 387 with color-coded badges
- [ ] Each value uses the `currency-usd` class for proper formatting
- [ ] Colors match the corresponding chart series colors:
  - Premium: yellow (new color to define)
  - Total: green '#2eca6a'
  - Cash: orange '#ff771d'
  - Stock: blue '#4154f1'
  - Options: red '#f14161'
- [ ] Layout is responsive and doesn't break on mobile
- [ ] Manual test: Dashboard loads and displays all NAV values correctly
- [ ] Manual test: Values update correctly when using time filter (week/month/year/all)

---

## Acceptance Tests

```java
@Test
void testDashboardNavFieldsDisplay() {
    // When: Dashboard is loaded
    DashboardDto dto = dashboardService.getDashboardData();

    // Then: Latest NAV values are available in the model
    assertNotNull(dto.getLatestTotal());
    assertNotNull(dto.getLatestCash());
    assertNotNull(dto.getLatestStock());
    assertNotNull(dto.getLatestOptions());

    // And: Premium is available
    assertNotNull(dto.getCollectedPremium());
}
```

**Manual testing**:
1. Load dashboard page
2. Verify all 5 values (premium, total, cash, stock, options) are displayed
3. Verify each value has the correct color badge
4. Change time filter (week/month/year/all)
5. Verify values update correctly for each filter

---

## Related Issues

- Related: ISSUE-055 (Trade Log card alignment - same visual consistency theme)
- Related: ISSUE-054 (Open Positions DataTable - portfolio view features)

---

## Notes

**Implementation location**: `src/main/jte/dashboard_jte.jte:387`

**Chart colors reference** (line 512):
```javascript
colors: [ '#2eca6a','#ff771d','#4154f1','#f14161']
```

**Proposed layout example**:
```html
<h5 class="card-title">
    Net Asset Values
    <span class="badge" style="background-color: #ffcc00;">Premium: <span class="currency-usd">${premium}</span></span>
    <span class="badge" style="background-color: #2eca6a;">Total: <span class="currency-usd">${total}</span></span>
    <span class="badge" style="background-color: #ff771d;">Cash: <span class="currency-usd">${cash}</span></span>
    <span class="badge" style="background-color: #4154f1;">Stock: <span class="currency-usd">${stock}</span></span>
    <span class="badge" style="background-color: #f14161;">Options: <span class="currency-usd">${options}</span></span>
</h5>
```

**Data model investigation needed**:
- Check if `DashboardDto` or `PositionDto` already has latest NAV values
- If not, add getter methods that return the last element of the respective arrays
- Consider whether to compute in controller or in a service method

**Color palette**: The yellow for premium should be a bright, readable color that stands out. Suggested: `#ffcc00` or `#ffd700` (gold).
