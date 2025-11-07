# ISSUE-010: TradingView Phase 1A Dashboard Integration

**Created**: 2025-11-07 (Session 6)
**Completed**: 2025-11-07
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: ISSUE-011 (Phase 1B - Position Calculator)

---

## Problem

Dashboard displays static FinViz chart images (QQQ, GDX, IBIT) that don't provide interactive features like zoom, pan, or technical indicators. Users cannot perform detailed technical analysis directly within the application.

---

## Root Cause

Current implementation uses static image URLs from FinViz, wrapped in external TradingView links. This approach:
- Requires external navigation (opens new tab)
- Provides no interactivity within SrcProfit
- Cannot display real-time technical indicators
- Limited mobile experience (static images don't scale well)

---

## Approach

**Phase 1A: Dashboard Quick Wins** (ISSUE-009 research completed)

1. Add TradingView CDN script to base template (`index_jte.jte`)
2. Create helper JavaScript file for widget initialization
3. Replace 3 FinViz images with TradingView Symbol Overview widgets
4. Implement feature flag for easy rollback (FinViz ↔ TradingView toggle)
5. Test responsive behavior

### Why Symbol Overview Widget?
- Optimized for dashboard cards (quick at-a-glance view)
- Real-time price quotes with technical indicators
- Mobile-responsive by design
- Simple to configure and maintain

### Technology Stack
- **TradingView Embedding API** (free tier, client-side only)
- **No backend changes required**
- **No new Maven dependencies**
- Feature flag: data attribute (`data-use-tradingview="true"`)

---

## Success Criteria

- [x] Research completed and documented (ISSUE-009)
- [ ] TradingView CDN script added to `index_jte.jte`
- [ ] `tradingview-integration.js` created with:
  - Symbol format conversion (AAPL → NASDAQ:AAPL)
  - Widget initialization function
  - Feature flag support
- [ ] Dashboard cards display TradingView Symbol Overview widgets for:
  - [ ] QQQ chart (replaces line 53)
  - [ ] GDX chart (replaces line 84)
  - [ ] IBIT chart (replaces line 117)
- [ ] Feature flag allows toggle between FinViz and TradingView
- [ ] All 3 widgets load without JavaScript errors
- [ ] Responsive behavior verified on mobile (iPhone/Android)
- [ ] No performance regression vs FinViz images
- [ ] Rollback plan tested (disable feature flag)

---

## Implementation Details

### Files to Modify

#### 1. `src/main/jte/index_jte.jte`
- Add TradingView Symbol Overview script tag
- Location: After HTMX script (line ~33)
```html
<script src="https://s3.tradingview.com/external-embedding/embed-widget-symbol-overview.js"></script>
```

#### 2. Create `src/main/resources/static/assets/js/tradingview-integration.js`
```javascript
// Symbol format conversion
// Widget initialization with feature flag support
// Re-initialization on HTMX updates
```

#### 3. `src/main/jte/dashboard_jte.jte`
- Replace FinViz `<img>` tags with TradingView widget containers
- Add feature flag attribute (`data-use-tradingview`)
- Keep FinViz fallback for disabled state

### Feature Flag Behavior

**Enabled (default)**:
```html
<div data-use-tradingview="true" class="tradingview-widget">
  <!-- TradingView Symbol Overview widget -->
</div>
```

**Disabled (fallback)**:
```html
<div data-use-tradingview="false">
  <!-- Original FinViz image (fallback) -->
  <img src="..." alt="...">
</div>
```

**Rollback**: Change `data-use-tradingview="true"` to `"false"` - no recompile needed.

### Rollback Plan

**Instant Rollback Strategy**:
1. Feature flag enabled by default
2. If issues arise, set `data-use-tradingview="false"` in dashboard template
3. No backend changes, no redeployment of code
4. Application automatically falls back to FinViz images

**Testing Rollback**:
- Build with feature flag disabled
- Verify FinViz images display correctly
- Confirm no JavaScript errors in console

---

## Technical Constraints

### Free Tier Limitations
- TradingView branding/watermark cannot be removed
- Attribution links required
- Public market data only (no options data)
- Symbol format required: "NASDAQ:AAPL" (not "AAPL")

### Widget Limitations
- Cannot overlay custom SrcProfit data (P&L, Greeks)
- Cannot display options chain data
- Requires JavaScript enabled
- May impact page weight (lazy loading recommended for Phase 2)

### Browser Support
- Modern browsers (Chrome, Firefox, Safari, Edge)
- Graceful degradation if JavaScript disabled (fallback to FinViz)

---

## Acceptance Tests

```javascript
// Test 1: Widget initialization
test('TradingView widgets initialize for each symbol', () => {
  // Verify all 3 widgets load without errors
  // Check window.TradingView is defined
  // Verify widget containers rendered
});

// Test 2: Feature flag toggle
test('Feature flag controls FinViz vs TradingView display', () => {
  // Enable: TradingView widgets visible
  // Disable: FinViz images visible
});

// Test 3: Symbol format conversion
test('Symbol format correctly converts to TradingView format', () => {
  expect(convertToTradingViewSymbol('QQQ')).toBe('NASDAQ:QQQ');
  expect(convertToTradingViewSymbol('GDX')).toBe('NYSEARCA:GDX');
  expect(convertToTradingViewSymbol('IBIT')).toBe('NASDAQ:IBIT');
});

// Test 4: Responsive behavior
test('Widget displays correctly on mobile viewport', () => {
  // iPhone 12: 390x844
  // Android: 360x800
  // Verify widget height/width adjusts
});

// Test 5: Fallback behavior
test('Page displays FinViz images if JavaScript disabled', () => {
  // Disable JavaScript
  // Verify FinViz images still load via img tags
});
```

---

## Related Issues

- Blocks: ISSUE-011 (Phase 1B - Position Calculator)
- Related: ISSUE-009 (Research - COMPLETED)
- Related: ISSUE-008 (TradeLogController refactor)

---

## Session Progress

### Session 6 (2025-11-07)
- [x] Analyzed ISSUE-009 research findings
- [x] Created detailed implementation plan
- [x] Created ISSUE-010 document
- [ ] Add TradingView CDN script
- [ ] Create tradingview-integration.js
- [ ] Update dashboard_jte.jte with Symbol Overview widgets
- [ ] Test and verify
- [ ] Create ISSUE-011 for Phase 1B

---

## Notes

### Exchange Mapping Reference
Based on US market symbols:
- **NASDAQ**: QQQ, IBIT
- **NYSE ARCA**: GDX
- **NYSE**: SPY, IWM

Complete exchange mapping available in TradingView documentation.

### Performance Considerations
- Each widget loads independently
- Recommended: 3-4 concurrent widgets max per page
- Future optimization: Lazy load off-screen widgets (Phase 2)

### TradingView Terms
- Free tier requires attribution (cannot remove branding)
- Monitor if SrcProfit becomes commercial service
- Review embedding terms: https://www.tradingview.com/

### Next Steps (Phase 1B)
After Phase 1A verified:
1. Create ISSUE-011 for Position Calculator (Advanced Chart widget)
2. Implement dynamic symbol update logic
3. Add technical indicators (Bollinger Bands, ATR)
4. Test symbol change behavior in modal

---

**Version**: 1.0
**Last Updated**: 2025-11-07
