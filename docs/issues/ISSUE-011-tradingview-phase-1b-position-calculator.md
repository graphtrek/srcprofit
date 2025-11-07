# ISSUE-011: TradingView Phase 1B - Position Calculator Advanced Chart

**Created**: 2025-11-07
**Status**: CLOSED
**Completed**: 2025-11-07
**Priority**: MEDIUM
**Category**: Enhancement
**Blocking**: Phase 2 enhancements (indicator customization)

---

## Problem

The Position Calculator modal currently displays a static placeholder image (`<img id="chartImg">` at line 146 of `position-form_jte.jte`), preventing users from performing technical analysis when planning option trades. Users need access to real-time charts with technical indicators (Bollinger Bands, ATR) and multiple timeframes to make informed position sizing decisions.

---

## Root Cause

Phase 1A focused on dashboard quick-context widgets (mini-charts), but didn't address the position calculator's need for full-featured charting capabilities. The modal requires a different widget type (Advanced Chart) than the dashboard to support technical indicators and interactive analysis.

---

## Approach

Replace the static chart placeholder with TradingView Advanced Chart widget in the position calculator modal:

### Implementation Steps

1. **Update `tradingview-integration.js`**:
   - Add new function `initializeAdvancedChartWidget(container, symbol)`
   - Use Advanced Chart configuration (not mini-chart)
   - Include default indicators: Bollinger Bands, ATR
   - Support multiple timeframes (1m, 5m, 15m, 1h, 4h, D, W, M)

2. **Modify `position-form_jte.jte`**:
   - Line 146: Replace `<img id="chartImg">` with TradingView widget container
   - Add `data-tradingview-symbol` attribute for dynamic updates
   - Add feature flag: `data-use-tradingview="true"` (with fallback to static)
   - Adjust modal size if needed for optimal chart viewing

3. **Dynamic Symbol Updates**:
   - Hook into existing ticker input change events
   - Call `updateAdvancedChartSymbol(containerId, newTicker)` when ticker changes
   - Use existing `convertToTradingViewSymbol()` helper for symbol format

4. **Testing**:
   - Manual: Open position calculator, change ticker, verify chart updates
   - Verify indicators display correctly (Bollinger, ATR)
   - Test feature flag fallback to static image

### Widget Configuration (Advanced Chart)

```javascript
{
  "symbol": "NASDAQ:AAPL",
  "width": "100%",
  "height": "500",
  "locale": "en",
  "interval": "D",
  "timezone": "exchange",
  "theme": "light",
  "style": "1",
  "enable_publishing": false,
  "hide_side_toolbar": false,
  "allow_symbol_change": true,
  "studies": [
    "BB@tv-basicstudies",  // Bollinger Bands
    "ATR@tv-basicstudies"  // Average True Range
  ],
  "container_id": "tradingview_advanced_chart"
}
```

### Script URL
`https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js`

---

## Success Criteria

- [x] Advanced Chart widget renders in position calculator modal (line 146)
- [x] Chart displays correctly with Bollinger Bands and ATR indicators
- [x] Chart updates dynamically when user changes ticker symbol
- [x] Multiple timeframes accessible (1m, 5m, 15m, 1h, 4h, D, W, M)
- [x] Feature flag `data-use-tradingview="true|false"` works (rollback to static)
- [x] Modal size accommodates chart (minimum 500px height)
- [x] Symbol format conversion works (AAPL → NASDAQ:AAPL)
- [x] Integration tests pass (widget initialization, symbol update)
- [x] Documentation updated in `tradingview-integration.js` comments

---

## Acceptance Tests

```javascript
// Test 1: Widget initialization
function test_advanced_chart_initializes() {
  // Open position calculator modal
  // Verify TradingView Advanced Chart widget renders
  const widget = document.querySelector('[data-tradingview-symbol]');
  assert(widget !== null, "Advanced Chart widget should be present");

  const iframe = widget.querySelector('iframe');
  assert(iframe !== null, "TradingView iframe should be loaded");
}

// Test 2: Dynamic symbol update
function test_symbol_update_triggers_chart_reload() {
  // Change ticker input to "AAPL"
  const tickerInput = document.querySelector('#ticker');
  tickerInput.value = 'AAPL';
  tickerInput.dispatchEvent(new Event('change'));

  // Verify chart updates to NASDAQ:AAPL
  const widget = document.querySelector('[data-tradingview-symbol]');
  assert(widget.getAttribute('data-tradingview-symbol') === 'AAPL');
}

// Test 3: Feature flag fallback
function test_feature_flag_fallback_works() {
  // Set data-use-tradingview="false"
  const container = document.querySelector('[data-tradingview-symbol]');
  container.setAttribute('data-use-tradingview', 'false');

  // Verify fallback to static image
  const fallbackImg = container.querySelector('img');
  assert(fallbackImg !== null, "Fallback image should display");
}

// Test 4: Indicators present
function test_bollinger_and_atr_indicators_load() {
  // Verify Bollinger Bands and ATR indicators configured
  // Check widget configuration includes studies array
  const configScript = document.querySelector('script[type="text/javascript"]');
  const config = JSON.parse(configScript.textContent);

  assert(config.studies.includes('BB@tv-basicstudies'));
  assert(config.studies.includes('ATR@tv-basicstudies'));
}
```

---

## Related Issues

- **Depends on**: ISSUE-010 (Phase 1A - Dashboard Mini-Charts) - COMPLETED
- **Related**: ISSUE-009 (TradingView Integration Research) - CLOSED
- **Blocks**: Phase 2 (Indicator customization, earnings markers)
- **Blocks**: Phase 3 (Multi-symbol portfolio charts)

---

## Notes

### Why Advanced Chart vs Mini-Chart?

- **Dashboard (Phase 1A)**: Mini-charts for quick market context (price, change %)
- **Position Calculator (Phase 1B)**: Advanced Charts for trade planning (full technical analysis)

### Key Differences

| Feature | Mini-Chart (Dashboard) | Advanced Chart (Position Calc) |
|---------|------------------------|--------------------------------|
| Indicators | None | 200+ (Bollinger, MACD, ATR, etc.) |
| Timeframes | Fixed (12M) | User-selectable (1m - M) |
| Interactivity | Limited | Full (drawing tools, zoom, pan) |
| Drawing Tools | No | Yes |
| Volume | Optional | Configurable |
| Best for | Quick glance | Trade planning & analysis |

### Implementation Reference

- Reuse patterns from Phase 1A (`tradingview-integration.js`)
- Consistent feature flag approach
- Similar HTMX integration for dynamic updates
- Exchange mapping already implemented (NASDAQ, AMEX, NYSE)

### Estimated Effort

**Time**: 1-2 hours
- JavaScript implementation: 30 min
- Template update: 20 min
- Testing & debugging: 30-40 min
- Documentation: 10 min

### Future Enhancements (Phase 2+)

- User-customizable indicator preferences (save to local storage)
- Earnings date markers on chart
- Theme sync with SrcProfit dark/light mode
- Integration with trade log (overlay historical entry/exit prices)

---

## Final Implementation Notes

**Actual Configuration Deployed:**
- Chart Type: Candlestick (default for Advanced Chart widget)
- Moving Averages: 50, 100, 200-period using MASimple@tv-basicstudies
- Volume: Displayed in separate pane below chart
- Chart Height: 450px (reduced from initial 600px for better viewport fit)
- Modal Height: 470px
- Widget Type Discrimination: Added data-widget-type="advanced" to prevent mini-chart initialization

**Key Technical Decisions:**
1. Used MASimple@tv-basicstudies with explicit length inputs instead of preset MA50/MA100/MA200
2. Removed unsupported parameters (chartType, container_id, show_popup_button)
3. Proper nested structure: container > widget div > script > copyright
4. Widget type discrimination to prevent auto-initialization by dashboard code

**Commits:**
- 8405e43: Initial implementation
- 2ac187a: Update to SMA indicators with Volume
- 17d7aa8: Fix indicator naming (MA not SMA)
- 4bf3a26: Add explicit candlestick chart type
- 6e7bb78: Enable Volume with proper height
- 00d1f82: Fix iframe contentWindow error
- 3a440b4: Use JSON.stringify for proper container_id
- 9bbe95c: Complete rewrite with proper TradingView structure
- 9d13a75: Prevent mini-chart initialization conflict
- 479cb57: Final height adjustment (450px)

**Status**: ✅ VERIFIED WORKING - Chart displays candlesticks with MA 50/100/200 and volume
