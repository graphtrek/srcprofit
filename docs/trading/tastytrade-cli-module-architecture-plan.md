# TastyTrade-CLI Module Architecture Plan

**Session 157** - Clean Architecture refactoring strategy for all modules

**Philosophy**: Port proven functionality with Clean Architecture, defer web UI features

---

## Module Inventory & Analysis

| Module | LOC | Commands | Priority | Port Strategy |
|--------|-----|----------|----------|---------------|
| **option.py** | 1,068 | 4 | CRITICAL | Port with Clean Arch (ISSUE-007) |
| **portfolio.py** | 612 | 4 | CRITICAL | Partially ported (margin needed) |
| **order.py** | 246 | 2 | CRITICAL | Port with Clean Arch (ISSUE-006) |
| **trade.py** | 327 | 3 | HIGH | Port with Clean Arch (Future) |
| **plot.py** | 199 | 1 | DEFER | Web UI (charting library) |
| **utils.py** | 205 | N/A | REUSE | Already ported (RenewableSession, etc.) |
| **watchlist.py** | 15 | 1 | DEFER | Not implemented in tt-cli |

---

## Module Deep Dive

### 1. option.py (1,068 LOC) - ISSUE-007

**What it does**: TastyTrade's core options trading methodology

**Commands**:
1. `call` - Buy call spreads by delta (16-30 delta typical)
2. `put` - Buy put spreads by delta
3. `strangle` - Sell strangles by delta (core TastyTrade strategy!)
4. `chain` - Display options chain with Greeks

**Key Features**:
- **Delta-based strike selection** (mechanical, probability-based)
- **Spread width calculation** (risk-defined trades)
- **Greeks display** (delta, gamma, theta, vega)
- **DXLink streaming** (real-time Greeks)
- **Order preview + execution** (suggestion mode ready!)

**Architecture (Clean)**:
```
CLI Layer: src/cli/options.py
  ├─ Commands: call, put, strangle, chain
  └─ Rich tables, interactive selection

Domain Layer: src/domain/options_strategy.py (NEW)
  ├─ DeltaStrikePicker (choose by delta)
  ├─ SpreadBuilder (calculate width, legs)
  ├─ GreeksAnalyzer (aggregate multi-leg Greeks)
  └─ StrategyValidator (risk checks)

Adapter Layer: src/brokers/tastytrade_adapter.py
  ├─ get_option_chain()
  ├─ stream_greeks() (already exists!)
  └─ place_option_order()
```

**Why Critical**: This IS the TastyTrade methodology (mechanical options trading)

**Future Vision**: Your wheel strategy uses this!
```python
# Future (not now):
wheel = WheelStrategy(delta_target=0.30)
# Uses: option.strangle() → DeltaStrikePicker → place_option_order()
```

---

### 2. portfolio.py (612 LOC) - ISSUE-008 (partial)

**What it does**: Portfolio display + margin analysis

**Commands**:
1. `show` - Current positions ✅ **ALREADY PORTED** (contrarian sync-positions)
2. `history` - Previous positions ✅ **ALREADY PORTED** (contrarian position-history)
3. `margin` - Margin usage by position ⚠️ **NEEDED** (ISSUE-008)
4. `balance` - Account balances ✅ **ALREADY PORTED** (contrarian balance)

**What We Need**:
- **ONLY `margin` command** (100 LOC estimate)
  - Per-position buying power breakdown
  - VIX-adjusted warnings
  - Portfolio heat tracking

**Architecture (Clean)**:
```
CLI Layer: src/cli/portfolio.py (extend existing)
  └─ margin command

Domain Layer: src/domain/margin_analyzer.py (NEW)
  ├─ get_position_bp_effect()
  ├─ calculate_portfolio_heat()
  └─ get_vix_adjusted_warnings()

Adapter Layer: src/brokers/tastytrade_adapter.py
  └─ get_margin_requirements() (NEW)
```

**Why Partial**: We already have 3/4 commands!

---

### 3. order.py (246 LOC) - ISSUE-006

**What it does**: Order management

**Commands**:
1. `live` - View/modify/cancel live orders
2. `history` - Order history with filters

**Architecture**: See `/tmp/order-management-architecture.md` (already designed)

**Status**: Ready to implement (this session!)

---

### 4. trade.py (327 LOC) - FUTURE

**What it does**: Simple trading (stocks, crypto, futures)

**Commands**:
1. `stock` - Buy/sell stocks/ETFs
2. `crypto` - Buy cryptocurrency
3. `future` - Buy/sell futures

**Architecture (Clean)**:
```
CLI Layer: src/cli/trade.py
  ├─ stock, crypto, future commands
  └─ Interactive price selection

Domain Layer: src/domain/simple_trader.py (NEW)
  ├─ get_market_price()
  ├─ calculate_order_value()
  └─ validate_buying_power()

Adapter Layer: src/brokers/tastytrade_adapter.py
  └─ place_simple_order()
```

**Why Lower Priority**: Options trading is the focus, simple trades are easy

---

### 5. plot.py (199 LOC) - PARTIAL PORT (Historical Data Method)

**What it does**: Candlestick charts + **Historical price fetching**

**Command**: `candles` - Display price chart

**Key Discovery** (Session 157 - User insight!):
**Optimal historical price fetching pattern**:
```python
# Lines 107-118: DXLink Streamer for historical candles
async with DXLinkStreamer(sesh) as streamer:
    await streamer.subscribe_candle([symbol], width.value, start_time)
    async for candle in streamer.listen(Candle):
        if candle.close:
            # Collect: open, high, low, close, timestamp
            candles.append(...)
        if candle.time == ts:  # Stop at start_time
            break
candles.sort()  # Sort chronologically
```

**Why This Matters**:
- ✅ **DXLink streaming** (not REST API) - More efficient for historical data
- ✅ **Batched delivery** - Streamer sends chunks, not one-by-one
- ✅ **Sorted output** - Chronological order guaranteed
- ✅ **Multiple timeframes** - 1m, 5m, 30m, 1h, 1d, 1mo, 1y
- ✅ **NYSE-aware** - `NYSE.valid_days()` skips weekends/holidays

**What We Port**:
- ✅ Historical candle fetching logic (domain layer)
- ✅ DXLink streaming pattern (adapter layer)
- ❌ gnuplot visualization (web UI will use plotly/d3.js)

**Architecture (Clean)**:
```
Domain Layer: src/domain/market_data_fetcher.py (NEW)
  ├─ get_historical_candles() (OHLCV data)
  ├─ calculate_indicators() (SMA, RSI, etc.)
  └─ detect_patterns() (support/resistance, breakouts)

Adapter Layer: src/brokers/tastytrade_adapter.py
  └─ stream_candles() (DXLink wrapper)

Storage Layer: src/storage/repositories.py
  └─ cache_candles() (avoid re-fetching)

CLI Layer: Defer to Web UI
  - Web UI will use plotly for charts
  - CLI will have tabular historical data display
```

**Why Port This**:
- **Backtesting**: Historical candles needed for strategy validation
- **Pattern recognition**: Support/resistance, breakouts, IV trends
- **Entry timing**: Historical IV rank, price action for wheel strategy
- **Performance**: Streaming is faster than REST for bulk historical data

**Future Use (Wheel Strategy)**:
```python
# Future (not now):
def should_sell_put(symbol: str) -> bool:
    """Check if IV rank is elevated (50+) before selling puts."""
    candles = market_data.get_historical_candles(
        symbol,
        width="1d",
        lookback_days=365
    )
    iv_rank = calculate_iv_rank(candles)  # Needs historical IV data
    return iv_rank > 50  # TastyTrade: sell premium when IV is high
```

**Decision**: Port historical data fetching (valuable!), defer charting to web UI

---

### 6. watchlist.py (15 LOC) - DEFER

**What it does**: Nothing! (not implemented in tastytrade-cli)

**Status**: Placeholder only

**Decision**: Skip, not needed

---

### 7. utils.py (205 LOC) - ALREADY REUSED

**What it does**: Shared utilities

**Key Components**:
- `RenewableSession` ✅ **PORTED** (Session 145)
- `listen_events()` ✅ **PORTED** (Session 146-147)
- `conditional_color()` - Rich formatting
- `round_to_tick_size()` - Price rounding
- Helper functions

**Status**: Already integrated into contrarian

---

## Clean Architecture Pattern (Standard for All Modules)

```
┌─────────────────────────────────────────────────────────────┐
│                      CLI Layer                               │
│  src/cli/{module}.py                                        │
│  - Typer commands                                           │
│  - Rich tables                                              │
│  - User prompts                                             │
│  - Input validation                                         │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                  Domain Layer                                │
│  src/domain/{module}_manager.py                             │
│  - Business logic                                           │
│  - Strategy patterns                                        │
│  - Risk validation                                          │
│  - Calculations                                             │
│  - NO SDK dependencies                                      │
└────────────────┬────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                 Broker Adapter Layer                        │
│  src/brokers/tastytrade_adapter.py                         │
│  - SDK wrapper                                              │
│  - RenewableSession                                         │
│  - Greeks enrichment                                        │
│  - Market data fetching                                     │
└─────────────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────┐
│                 Storage Layer (Optional)                    │
│  src/storage/repositories.py                               │
│  - PostgreSQL                                               │
│  - Caching                                                  │
│  - Audit trail                                              │
└─────────────────────────────────────────────────────────────┘
```

**Benefits**:
- ✅ **Testability**: Domain layer fully mockable
- ✅ **Extensibility**: Add features without touching SDK code
- ✅ **Reusability**: Domain logic works with any broker
- ✅ **Clarity**: Each layer has single responsibility

---

## Porting Roadmap (Milestone 2)

### Session 157 (Current) - Order Management
- **ISSUE-006**: Port order.py
- **Files**: order_manager.py, tastytrade_adapter.py (extend), cli/orders.py
- **Estimated**: 4-6 hours
- **Value**: Foundation for all trading automation

### Sessions 158-159 - Options Trading
- **ISSUE-007**: Port option.py
- **Files**: options_strategy.py, cli/options.py
- **Estimated**: 8-12 hours (biggest module!)
- **Value**: Delta-based strike selection (core TastyTrade methodology)

### Session 160 - Margin Analysis
- **ISSUE-008**: Port portfolio.py margin command
- **Files**: margin_analyzer.py, cli/portfolio.py (extend)
- **Estimated**: 2-3 hours
- **Value**: Risk management, portfolio heat tracking

### Sessions 161-162 - Testing & Validation
- Integration tests for all modules
- Real account validation
- Suggestion mode workflow
- **Value**: Confidence in trading automation

### Future (Milestone 3+)
- **trade.py**: Simple stock/crypto/futures trading (stocks, crypto, futures)
- **plot.py**: Port historical candle fetching (backtesting, IV rank, pattern recognition)
- **watchlist.py**: Skip (not implemented in tastytrade-cli)

---

## What Makes This Different from tastytrade-cli

| Aspect | tastytrade-cli | contrarian (our port) |
|--------|----------------|----------------------|
| **Architecture** | Monolith (CLI + SDK mixed) | Clean Architecture (3-4 layers) |
| **Testability** | Hard (CLI + SDK entangled) | Easy (domain layer mocked) |
| **Extensibility** | Limited (add features = change CLI) | High (add features = new domain logic) |
| **Storage** | None (ephemeral) | PostgreSQL (positions, orders, audit) |
| **Reusability** | TastyTrade only | Multi-broker ready (IBKR, etc.) |
| **Automation** | Manual CLI tool | API-first (suggestion mode ready) |
| **Future** | Portfolio tool | Trading automation platform |

---

## Strategy Extensibility Examples

### Example 1: The Wheel Strategy (Your Vision)

**Using our Clean Architecture**:
```python
# Future (not now):
class WheelStrategy:
    def __init__(
        self,
        delta_target: float = 0.30,
        iv_rank_min: int = 50,
        position_size_pct: float = 0.02
    ):
        self.options_manager = OptionsStrategy()  # From ISSUE-007
        self.margin_analyzer = MarginAnalyzer()   # From ISSUE-008
        self.order_manager = OrderManager()       # From ISSUE-006

    def find_put_opportunities(self) -> list[PutSuggestion]:
        """Find high-IV stocks, suggest 30-delta puts."""
        # Uses: OptionsStrategy.get_chain_by_delta()
        # Uses: MarginAnalyzer.calculate_position_size()
        pass

    def find_call_opportunities(self) -> list[CallSuggestion]:
        """For assigned positions, suggest covered calls."""
        # Uses: OptionsStrategy.get_chain_by_delta()
        # Uses: existing position data
        pass

    def execute_suggestion(self, suggestion: Suggestion) -> Order:
        """Human reviews, then executes."""
        # Uses: OrderManager.place_order()
        # Uses: audit trail
        pass
```

**This Works Because**:
- ✅ `OptionsStrategy` (ISSUE-007) handles delta-based strike selection
- ✅ `MarginAnalyzer` (ISSUE-008) validates position sizing
- ✅ `OrderManager` (ISSUE-006) executes trades
- ✅ All domain logic is reusable (not tied to CLI)

### Example 2: Iron Condor Strategy

```python
# Future (not now):
class IronCondorStrategy:
    def __init__(self, delta_short: float = 0.16, wing_width: int = 5):
        self.options_manager = OptionsStrategy()

    def build_iron_condor(self, symbol: str) -> IronCondorSuggestion:
        """
        Sell 16-delta call, buy 11-delta call (5 wide)
        Sell 16-delta put, buy 11-delta put (5 wide)
        """
        # Uses: OptionsStrategy.get_chain_by_delta()
        # Uses: SpreadBuilder.calculate_width()
        pass
```

---

## Success Criteria (Milestone 2 Complete)

✅ **Feature Parity**: All critical tastytrade-cli features work
✅ **Clean Architecture**: 3-layer separation for all modules
✅ **Testability**: Domain layer fully tested, mocked
✅ **Extensibility**: Can build wheel strategy on top
✅ **Real Account Validated**: Works with live account
✅ **Suggestion Mode Ready**: Review → Approve → Execute workflow

---

## What We Defer (Web UI)

❌ **plot.py** - Charting (web UI has better libraries)
❌ **watchlist.py** - Not implemented in tastytrade-cli

**Why**: Focus on trading automation foundation first, UI enhancements later

---

## Key Decisions

### Decision 1: Port Options Trading Exactly (ISSUE-007)
**Rationale**: Delta-based positioning is the core TastyTrade methodology. Their code handles ALL edge cases (expiration selection, Greeks aggregation, spread width, etc.). Don't "improve" production-tested logic.

### Decision 2: Clean Architecture for Everything
**Rationale**: Monolithic CLI code blocks extensibility. Domain layer separation enables testing, reuse, and strategy composition (wheel = put strategy + call strategy).

### Decision 3: Defer Web UI Features
**Rationale**: CLI is fast for automation, web UI is for visualization. Build foundation first (order management, options trading), add visualization later.

### Decision 4: Portfolio Partial Port (margin only)
**Rationale**: We already have position display, history, balance. Only margin analysis is missing for risk management.

---

## Next Steps (Immediate)

**This Session (157)**:
1. Port order.py with Clean Architecture (ISSUE-006)
2. Create OrderManager (domain layer)
3. Extend TastyTradeAdapter (broker layer)
4. Create CLI commands (UI layer)
5. Integration tests + real account validation

**Sessions 158-159**:
- Port option.py (biggest module, core methodology)
- Create OptionsStrategy domain layer
- Delta-based strike selection
- Spread building logic

**Session 160**:
- Port portfolio.py margin command
- Create MarginAnalyzer domain layer
- VIX-adjusted warnings

---

**Ready to build the future!** 🚀

**User's Vision**: "if our data is correct and if our app is stable and precise"
- ✅ Correct data (RenewableSession, Greeks enrichment working)
- ✅ Stable app (615 tests, Ground Truth TDD, V2 foundation solid)
- ✅ Precise calculations (EXACT money match requirement)
- 🎯 **Missing**: Trading automation infrastructure ← **This roadmap solves it**

After Milestone 2: Wheel strategy, iron condors, suggestion mode - all become trivial to implement!
