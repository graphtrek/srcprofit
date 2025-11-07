# ISSUE-009: TradingView Chart Integration Research

**Created**: 2025-11-07 (Session N/A)
**Completed**: 2025-11-07
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None yet - research phase

---

## Problem

Currently, SrcProfit uses static FinViz chart images embedded in the dashboard (QQQ, GDX, IBIT cards) and position calculator. These static images:
- Do not update in real-time
- Cannot be interacted with (no zoom, pan, indicators)
- Require manual refresh to see new data
- Provide limited technical analysis capabilities

Meanwhile, TradingView is already referenced in the codebase as external navigation links, but not leveraged as embedded charts.

---

## Root Cause

Chart visualization strategy was built pragmatically (FinViz images + ApexCharts) without evaluating modern embeddable chart widgets that offer better UX at no cost.

---

## Approach

**Research Phase Completed**: Document findings from TradingView Advanced Chart widget integration feasibility.

**Implementation Strategy (3 phases)**:

### Phase 1: Quick Win (1-2 hours)
Replace FinViz static images in dashboard with TradingView interactive charts:
- Dashboard cards (lines 53, 84, 117 in `dashboard_jte.jte`)
- Default symbols: QQQ, GDX, IBIT
- Configuration: Daily timeframe, light/dark theme support

### Phase 2: Enhanced Position Calculator (2-4 hours)
- Replace modal chart with TradingView widget (line 146 in `position-form_jte.jte`)
- Dynamic symbol updates based on ticker input
- Add technical indicators relevant to options (Bollinger Bands, ATR)
- Support for user indicator customization

### Phase 3: Advanced Visualizations (8-16 hours)
- Dedicated charts/portfolio page showing multi-symbol grid
- Integration with earnings calendar (mark dates on charts)
- Options strategy visualization (entry/exit prices, strike overlays)
- Trade log historical analysis (price action at execution)

---

## Technical Details

### Widget Capabilities
- **Real-time charting** for stocks, ETFs, forex, futures, crypto
- **Technical indicators**: 200+ built-in indicators (Bollinger Bands, MACD, Stochastic, etc.)
- **Multiple timeframes**: 1m, 5m, 15m, 1h, 4h, D, W, M
- **Themes**: Light/dark with automatic detection
- **Comparison mode**: Overlay multiple symbols
- **Image export**: User can save chart images
- **Watchlist**: Track multiple symbols
- **Customizable**: Symbol, interval, toolbar visibility, grid colors, timezone, locale

### Integration Requirements
- **No API key required** for basic embedding (free tier)
- **JavaScript-based**: Loads via CDN (`https://s3.tradingview.com/external-embedding/embed-widget-advanced-chart.js`)
- **Client-side only**: Zero backend changes needed
- **Dependencies**: None (self-contained widget)
- **Symbol format**: Must use TradingView format (e.g., "NASDAQ:AAPL" not "AAPL")

### Code Changes Required
Files to modify:
- `src/main/jte/index_jte.jte`: Add TradingView script tag to head
- `src/main/jte/dashboard_jte.jte`: Replace FinViz `<img>` tags (lines 53, 84, 117)
- `src/main/jte/position-form_jte.jte`: Update modal chart (line 146)
- New: `src/main/resources/static/js/tradingview-integration.js` (helper functions)

### Current State Analysis
- **ApexCharts**: Used for SrcProfit-specific data (daily premium, NAV breakdown) - KEEP for custom metrics
- **FinViz**: Static images for market context - REPLACE with TradingView
- **TradingView**: Currently used as external links only - EMBED as interactive widgets

**Recommended Hybrid Approach**:
- **TradingView widgets**: Market data visualization (real-time stock/ETF charts)
- **ApexCharts**: Portfolio analytics (custom calculations, P&L curves, Greeks)
- **Result**: Best UX without sacrificing SrcProfit-specific insights

---

## Constraints & Limitations

### Free Tier Restrictions
- TradingView branding required (watermark/logo)
- Limited to public market data
- Attribution links cannot be removed

### Technical Limitations
- **Cannot overlay custom data**: Cannot plot SrcProfit P&L zones or position markers directly on TradingView charts
- **No options chain**: Widget shows stock prices only, not options Greeks
- **Symbol format dependency**: Must maintain ticker → TradingView symbol mapping
- **JavaScript required**: Widget won't load without JavaScript enabled

### Performance Considerations
- Each widget loads independently (potential page weight increase)
- Recommend lazy loading for off-screen widgets
- Limit concurrent widgets to 3-5 per page for optimal performance

### Licensing
- **Free for non-commercial use** with attribution
- **Commercial use**: Review TradingView embedding terms if SrcProfit becomes paid service

---

## Success Criteria

Research phase completion:
- [x] Analyzed TradingView Advanced Chart widget documentation
- [x] Identified integration requirements and complexity level
- [x] Mapped current chart usage in SrcProfit (FinViz, ApexCharts, TradingView links)
- [x] Documented 3-phase implementation approach
- [x] Assessed constraints and limitations
- [x] Recommended hybrid visualization strategy

Implementation criteria (Phase 1):
- [ ] TradingView script loads in all pages
- [ ] Dashboard cards display interactive TradingView charts (QQQ, GDX, IBIT)
- [ ] Charts respond to theme changes (light/dark mode)
- [ ] Charts are responsive on mobile
- [ ] FinViz images completely removed
- [ ] No performance regression vs previous FinViz approach

---

## Acceptance Tests

### Phase 1 Acceptance
```javascript
// Dashboard should render TradingView widgets
test('dashboard displays tradingview chart for QQQ', () => {
  cy.visit('/dashboard');
  cy.get('#tradingview_qqq').should('be.visible');
  cy.get('iframe[src*="tradingview"]').should('exist');
});

// Charts should update theme with page theme
test('tradingview chart responds to dark mode toggle', () => {
  cy.visit('/dashboard');
  cy.get('[data-theme="dark"]').should('exist');
  // Verify TradingView widget loaded with dark theme
});

// Mobile responsiveness
test('tradingview charts are responsive on mobile', () => {
  cy.viewport('iphone-x');
  cy.visit('/dashboard');
  cy.get('#tradingview_qqq').should('have.css', 'width', '100%');
});
```

### Phase 2 Acceptance
```javascript
// Position calculator shows chart for selected symbol
test('position calculator displays chart for selected ticker', () => {
  cy.visit('/positions');
  cy.get('input[name="symbol"]').type('AAPL');
  cy.get('#tradingview_symbol').should('contain', 'AAPL');
});

// Chart updates dynamically
test('chart updates when symbol changes', () => {
  cy.get('input[name="symbol"]').clear().type('MSFT');
  cy.get('#tradingview_symbol').should('contain', 'MSFT');
});
```

---

## Related Issues

- Related: None yet
- Blocked by: None
- Blocks: None yet

---

## Notes

### References
- [TradingView Advanced Chart Widget Docs](https://www.tradingview.com/widget-docs/widgets/charts/advanced-chart/)
- Current FinViz chart URLs: `https://charts2-node.finviz.com/chart.ashx?...`
- TradingView external links currently used: dashboard (lines 52, 83, 116), position-form (lines 123, 304), earnings (line 60)

### Future Enhancements (Post-Phase 3)
- Explore TradingView Charting Library (paid tier) for custom data overlay (position markers, P&L zones)
- Integrate with broker APIs to sync entry/exit prices on charts
- Add volatility indicators and IV rank visualization
- Consider TradingView Premium features for advanced technical analysis

### Questions for Implementation
1. Should we remove TradingView external navigation links once embedded charts are live?
2. Do we want earnings dates marked on charts? (Requires earnings calendar integration)
3. Should users be able to customize indicators per symbol in dashboard cards?
4. Performance baseline: How many concurrent widgets can we support without lag?
5. Mobile experience: Should we hide some widgets on small screens?

### Session History
- Research conducted: 2025-11-07
- Status: Research phase completed
- Next action: Create ISSUE-010 for Phase 1 implementation when ready
