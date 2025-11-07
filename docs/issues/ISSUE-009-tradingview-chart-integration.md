# ISSUE-009: TradingView Chart Integration Research

**Created**: 2025-11-07 (Session N/A)
**Reopened**: 2025-11-07
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
- [TradingView Symbol Overview Widget Docs](https://www.tradingview.com/widget-docs/widgets/charts/symbol-overview/)
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

### Symbol Overview Widget Research (2025-11-07)

**Widget Comparison: Advanced Chart vs Symbol Overview**

| Feature | Advanced Chart | Symbol Overview |
|---------|---------------|-----------------|
| **Scope** | Full-featured charting platform | Single symbol with integrated quote |
| **Best for** | Interactive analysis, multiple timeframes | Quick reference, landing pages |
| **Quote display** | Optional overlay | Built-in, prominent |
| **Complexity** | High (200+ indicators) | Simple (basic moving averages) |
| **Mobile optimization** | Good | Excellent (designed for mobile) |
| **Dashboard use case** | Technical analysis deep-dive | At-a-glance market context |

**Symbol Overview Widget Capabilities**:
- **Quote integration**: Latest price + percent change displayed prominently alongside chart
- **Simple chart**: Area chart by default (vs Advanced Chart's candlestick/line/bar options)
- **Moving averages**: Configurable MA lines (default: 9-period, color: #2962FF)
- **Volume toggle**: Show/hide volume bars
- **Market status**: Real-time market open/closed indicator
- **Symbol logo**: Displays company branding
- **Responsive design**: Optimized for mobile/web landing pages
- **Theme support**: Light/dark mode with customizable colors
- **Date ranges**: Built-in period selectors
- **Autosize**: 100% width/height adaptation

**Integration Details**:
- **Script URL**: `https://s3.tradingview.com/external-embedding/embed-widget-symbol-overview.js`
- **Configuration**: JSON-based settings (similar to Advanced Chart)
- **Theme detection**: Uses `document.documentElement.dataset.theme`
- **Default symbols**: AAPL, GOOGL, MSFT (can be customized)
- **No API key required**: Free tier embedding (same as Advanced Chart)

**Customization Options**:
- Display toggles: Volume, moving averages, date ranges, market status, logo
- Visual settings: Scale position/mode, font family/size, line width/type
- Chart type: Area chart (default), limited vs Advanced Chart options
- Moving average: Length and color customization
- Header font size: Adjustable for different layouts
- Values tracking: Hover-based price display
- Price-and-percent change mode: Toggle between absolute/relative changes

**Limitations vs Advanced Chart**:
- **Single symbol focus**: Cannot overlay multiple symbols or compare
- **Limited indicators**: Only basic moving averages (no Bollinger, MACD, RSI, etc.)
- **Simpler chart types**: Area chart primary, no candlestick/Heikin-Ashi/Renko
- **No drawing tools**: Cannot annotate with trend lines, Fibonacci, etc.
- **Fixed timeframe**: Less granular control over intervals vs Advanced Chart
- **No export**: Users cannot save chart images (Advanced Chart supports this)

**Recommended Use Cases for SrcProfit**:

✅ **USE Symbol Overview for**:
- **Dashboard cards** (QQQ, GDX, IBIT): Quick market context with prominent quote display
- **At-a-glance views**: Users want price + simple trend, not deep analysis
- **Mobile experience**: Optimized for smaller screens
- **Landing/summary pages**: Simple, focused visual

❌ **USE Advanced Chart for**:
- **Position calculator**: Users need technical indicators (Bollinger, ATR) for trade planning
- **Detailed analysis pages**: Full-featured charting with drawing tools
- **Multi-timeframe analysis**: Switching between 1m/5m/1h/D/W views
- **Strategy visualization**: Complex indicator overlays

**Updated Implementation Strategy**:

### Phase 1A: Dashboard Quick Wins (1 hour)
Replace FinViz static images with **Symbol Overview widgets**:
- Dashboard cards (lines 53, 84, 117 in `dashboard_jte.jte`)
- Symbols: QQQ, GDX, IBIT
- Benefits: Real-time quotes + simple trend visualization, mobile-optimized

### Phase 1B: Position Calculator Enhancement (1-2 hours)
Replace modal chart with **Advanced Chart widget**:
- Modal in `position-form_jte.jte` (line 146)
- Benefits: Technical indicators (Bollinger, ATR), multiple timeframes
- Dynamic symbol updates based on ticker input

### Phase 2: Enhanced Visualizations (2-4 hours)
- Add indicator customization to position calculator (user preferences)
- Support for earnings date markers on charts
- Theme consistency with SrcProfit light/dark mode

### Phase 3: Advanced Analytics (8-16 hours)
- Dedicated charts/portfolio page with multi-symbol grid
- Trade log historical analysis (price action at execution)
- Options strategy visualization (entry/exit overlays)

**Widget Pairing Rationale**:
- **Symbol Overview** = "What's happening now?" (market context, dashboard)
- **Advanced Chart** = "What should I do?" (trade planning, analysis)
- **ApexCharts** = "How am I performing?" (portfolio P&L, custom metrics)

This hybrid approach provides best UX: simple widgets for quick context, advanced widgets for decision-making, custom charts for SrcProfit-specific analytics.

---

### Session History
- Research conducted: 2025-11-07 (Advanced Chart widget)
- Additional research: 2025-11-07 (Symbol Overview widget)
- Status: Research phase expanded, issue reopened
- Next action: Implement Phase 1A (Symbol Overview for dashboard) + Phase 1B (Advanced Chart for position calculator)
