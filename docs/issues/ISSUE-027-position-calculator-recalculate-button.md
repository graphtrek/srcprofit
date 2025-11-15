# ISSUE-027: Add Recalculate Button to Position Calculator

**Created**: 2025-11-13 (Session Current)
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature / UX Enhancement
**Blocking**: None

---

## Problem

The Position Calculator currently has a single "Calculate" button for initial calculations. While users can modify input fields (ticker, dates, prices, values) and recalculate, there's no explicit "Recalculate" button to clearly indicate the ability to re-trigger calculations with updated inputs. This leaves the user experience ambiguous about whether they can modify fields and recalculate.

---

## Root Cause

UI design oversight - the form only provides one button label, making it unclear that calculations can be re-run after input modifications.

---

## Approach

Add a new "Recalculate" button next to the existing "Calculate" button in the position form template (`src/main/jte/position-form_jte.jte`). The buttons will have different behavior:

**Calculate Button**:
1. Submits to `POST /calculatePosition` endpoint
2. Fetches live market data from Alpaca API
3. Recalculates all position metrics using form input values + updated market data

**Recalculate Button**:
1. Submits to new `POST /recalculatePosition` endpoint
2. Skips Alpaca market data fetch (uses existing market value from previous calculation)
3. Recalculates all position metrics using ONLY form input values
4. Faster response time (no API call)

**Common Features**:
- Always visible (no conditional rendering)
- Manually triggered only (no automatic recalculation on field changes)
- Use identical calculation logic (`calculateSinglePosition()` from ISSUE-026)
- Load related position data for display

**Rationale**: This provides two calculation modes for different user scenarios:
- **Calculate**: Get fresh market data when user wants current prices
- **Recalculate**: Quick "what-if" analysis after modifying input fields without waiting for API

---

## Success Criteria

- [ ] "Recalculate" button is visible next to "Calculate" button in the position form
- [ ] "Calculate" button submits to `POST /calculatePosition` with Alpaca market data fetch
- [ ] "Recalculate" button submits to `POST /recalculatePosition` WITHOUT market data fetch
- [ ] "Calculate" button uses `btn btn-primary` styling (solid blue)
- [ ] "Recalculate" button uses `btn btn-outline-primary` styling (outlined blue)
- [ ] Both buttons use HTMX for dynamic form submission
- [ ] Both buttons target `#main` and swap with `innerHTML`
- [ ] Recalculate skips Alpaca API call, retaining existing market value
- [ ] Both buttons recalculate metrics using `calculateSinglePosition()` logic
- [ ] Form data is properly serialized and transmitted to both endpoints
- [ ] Manual testing confirms:
  - Calculate button fetches fresh market data and recalculates
  - Recalculate button modifies only input fields (no API delay) and recalculates
  - Open/closed positions display correctly after both button clicks
- [ ] No regression in existing "Calculate" functionality
- [ ] Build compiles successfully
- [ ] All tests pass

---

## Acceptance Tests

```python
def test_issue_027_recalculate_button_visible():
    """Verify Recalculate button appears next to Calculate button"""
    response = client.get("/calculatePosition")
    assert 'name="recalculate"' in response.text or 'Recalculate' in response.text
    assert 'name="calculate"' in response.text or 'Calculate' in response.text

def test_issue_027_recalculate_triggers_calculation():
    """Verify Recalculate button triggers position calculation"""
    form_data = {
        'id': '',
        'parentId': '',
        'ticker': 'SPY',
        'tradeDate': '2025-10-15',
        'expirationDate': '2025-11-15',
        'tradePrice': '2.50',
        'positionValue': '250.00',
        'marketValue': '300.00'
    }
    response = client.post("/calculatePosition", data=form_data)
    # Verify calculation fields are populated
    assert 'daysLeft' in response.text
    assert 'roiPercent' in response.text

def test_issue_027_both_buttons_identical_behavior():
    """Verify Calculate and Recalculate buttons produce same result"""
    form_data = {
        'id': '',
        'parentId': '',
        'ticker': 'SPY',
        'tradeDate': '2025-10-15',
        'expirationDate': '2025-11-15',
        'tradePrice': '2.50',
        'positionValue': '250.00',
        'marketValue': '300.00'
    }
    response1 = client.post("/calculatePosition", data=form_data)
    response2 = client.post("/calculatePosition", data=form_data)
    # Both responses should produce identical calculations
    assert response1.text == response2.text
```

---

## Implementation Details

### Files Changed

#### 1. `src/main/java/co/grtk/srcprofit/controller/PositionController.java`

**Added New Endpoint** (after line 82):
```java
@PostMapping(path = "/recalculatePosition", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
public String recalculatePosition(@RequestBody MultiValueMap<String, String> formData, Model model) {
    log.info("recalculatePosition formData {}", formData);
    PositionDto positionDto = PositionMapper.mapFromData(formData);

    // Recalculation: use ONLY form values (skip Alpaca market data fetch)
    // Market value is retained from previous calculation
    optionService.calculateSinglePosition(positionDto);

    // Load related data for template
    fillPositionFormData(positionDto, model);
    model.addAttribute(MODEL_ATTRIBUTE_DTO, positionDto);

    return POSITION_FORM_PATH;
}
```

**Key Difference from `POST /calculatePosition`**:
- Does NOT call `getMarketValue()` - skips Alpaca API fetch
- Market value from form is used as-is (from previous Calculate button click)
- Faster execution (no network latency)

#### 2. `src/main/jte/position-form_jte.jte`

**Changes to Form** (line 54):
- Added `id="positionForm"` to the form for JavaScript reference

**Changes to Button Section** (lines 123-124):
```html
<button type="submit" name="calculate" class="btn btn-primary" hx-post="/calculatePosition">Calculate</button>
<button type="button" class="btn btn-outline-primary" hx-post="/recalculatePosition" hx-target="#main" hx-swap="innerHTML">Recalculate</button>
```

**Added JavaScript Handler** (after line 343):
```javascript
// Handle Recalculate button - submit form data to /recalculatePosition
$('button[hx-post="/recalculatePosition"]').on('click', function(e) {
    e.preventDefault();
    const formData = new FormData($('#positionForm')[0]);
    htmx.ajax('POST', '/recalculatePosition', {
        source: '#positionForm',
        target: '#main',
        swap: 'innerHTML',
        values: Object.fromEntries(formData)
    });
});
```

### Backend Logic

**POST /calculatePosition** (existing, line 66-82):
- Fetches live market data from Alpaca
- Calls `calculateSinglePosition()` with updated market value
- Ideal for: Initial calculations, price refreshes

**POST /recalculatePosition** (new, line 84-98):
- Skips market data fetch
- Calls `calculateSinglePosition()` with existing market value
- Ideal for: What-if analysis, quick recalculations after field changes

Both endpoints use the same calculation engine (`OptionService.calculateSinglePosition()`) from ISSUE-026.

---

## Related Issues

- **Related**: ISSUE-026 (Position Calculator Manual Recalculation) - Provides the backend foundation for recalculation without database interference
- **Related**: ISSUE-022 (Position Calculator DataTables Enhancement) - Previous Position Calculator UI improvements

---

## Notes

- This is a pure UI enhancement with zero backend risk
- Both buttons submit to the same endpoint, so behavior is identical
- Future enhancements could add:
  - Automatic recalculation on field change (debounced)
  - Differentiated button behavior (e.g., Calculate = fetch data, Recalculate = skip fetch)
  - Visual feedback showing when fields have been modified since last calculation
  - Keyboard shortcut (e.g., Ctrl+Enter) to trigger recalculation

**Button Styling Notes**:
- "Calculate" = `btn btn-primary` (primary action)
- "Recalculate" = `btn btn-outline-primary` or `btn btn-secondary` (secondary action)
- Both buttons should maintain responsive sizing
- Spacing: 8-12px gap between buttons for readability
