# TastyTrade-CLI Feature Gap Analysis

**Created**: 2025-10-18 (Session 156)
**Purpose**: Identify features from tastytrade-cli that we haven't ported yet
**Source**: tastytrade-cli (2,727 LOC, 10 modules)

---

## Summary

**What We Have**: ✅ Core infrastructure (RenewableSession, Greeks enrichment, adapter)
**What We're Missing**: Advanced trading features, order management, options strategies

---

## Feature Comparison Matrix

### ✅ Already Ported (Milestone 1 Complete)

| Feature | tastytrade-cli | Contrarian V2 | Status |
|---------|---------------|---------------|--------|
| **RenewableSession** | `utils.py` (205 LOC) | `src/brokers/renewable_session.py` | ✅ **COMPLETE** |
| **Greeks Batch Fetching** | `utils.py:listen_events()` | `src/brokers/greeks_service.py` | ✅ **COMPLETE** |
| **Position Enrichment** | `portfolio.py` (lines 86-112) | `src/brokers/tastytrade_adapter.py` | ✅ **COMPLETE** |
| **Portfolio Display** | `portfolio.py:positions()` | `src/cli/cli_rich.py:live` | ✅ **COMPLETE** |
| **Transaction History** | `portfolio.py:history()` | `src/cli/cli_rich.py:history` | ✅ **COMPLETE** |
| **Balance Display** | `portfolio.py:balance()` | `src/cli/cli_rich.py:status` | ✅ **COMPLETE** |

**Milestone 1 Coverage**: 6/6 core features ✅

---

## ⚠️ Missing Features (Candidates for Milestone 2+)

### 1. Options Trading (`option.py` - 1,068 LOC) ⚠️ **CRITICAL GAP**

**What they have**:
- ✅ Call buying/selling with delta or strike selection
- ✅ Put buying/selling with delta or strike selection
- ✅ Strangles (short/long with delta selection)
- ✅ Iron Condors (--width parameter)
- ✅ Vertical spreads (--width parameter)
- ✅ Options chain display with Greeks

**Commands**:
```bash
# Buy/sell calls with delta
tt option call SPY 10 --delta 30 --dte 45

# Buy/sell puts with strike
tt option put AMD -5 --strike 220 --width 10  # Put spread

# Short strangle
tt option strangle /ES -1 --delta 16 --width 50  # Iron condor

# View options chain
tt option chain SPY --strikes 20 --dte 30
```

**Features**:
- Delta-based strike selection (auto-calculates closest strike to target delta)
- DTE (days to expiration) filtering
- Weeklies vs monthlies filtering
- Automatic expiration selection (default 45 DTE or closest to target)
- Spread width parameter (converts singles to spreads)
- Real-time Greeks display
- Buying power usage warnings
- Dry-run order review before execution

**Priority**: **HIGH** (ISSUE-003 prerequisite)

**Estimated Port Time**: 8-12 hours (Session 157-158)

---

### 2. Order Management (`order.py` - 246 LOC) ⚠️ **CRITICAL GAP**

**What they have**:
- ✅ Live orders display (with ability to modify/cancel)
- ✅ Order history with filtering
- ✅ Order modification (price changes)
- ✅ Order cancellation

**Commands**:
```bash
# View live orders
tt order live --all  # All accounts

# Modify or cancel live orders
tt order live  # Interactive modification

# View order history
tt order history --start 2025-01-01 --symbol SPY --status FILLED
```

**Features**:
- Multi-account support
- Interactive order modification (change price or cancel)
- Ratio spread handling
- Order status filtering (LIVE, RECEIVED, FILLED, etc.)
- Date range filtering
- Symbol filtering

**Priority**: **HIGH** (Needed for trading automation)

**Estimated Port Time**: 4-6 hours (Session 157)

---

### 3. Simple Trading (`trade.py` - 327 LOC)

**What they have**:
- ✅ Stock/ETF trading
- ✅ Cryptocurrency trading
- ✅ Futures trading
- ✅ GTC vs DAY order selection
- ✅ Buying power warnings
- ✅ Fee calculation display

**Commands**:
```bash
# Buy/sell stocks
tt trade stock SPY 100 --gtc

# Buy/sell crypto
tt trade crypto BTC 0.1

# Buy/sell futures
tt trade future /ES 2 --gtc
```

**Features**:
- Quote display (bid/mid/ask)
- Custom limit price entry
- Buying power usage % calculation
- Fee display
- Dry-run validation
- Per-position BP % warnings

**Priority**: **MEDIUM** (Nice to have, but not critical)

**Estimated Port Time**: 3-4 hours (Session 159)

---

### 4. Margin Analysis (`portfolio.py:margin()`) ⚠️ **IMPORTANT**

**What they have**:
- ✅ Margin usage by position
- ✅ Per-position BP % warnings
- ✅ VIX-adjusted BP recommendations

**Commands**:
```bash
# View margin breakdown
tt pf margin
```

**Features**:
- Per-position margin requirements
- Per-position BP % (warns if >5% default)
- Total margin usage
- VIX-level comparison (warns if BP usage doesn't match VIX)
- Configurable thresholds

**Priority**: **HIGH** (Risk management)

**Estimated Port Time**: 2-3 hours (Session 159)

---

### 5. Portfolio Metrics Enhancements (`portfolio.py:positions()`)

**What they have that we DON'T**:
- ✅ Beta-weighted delta calculations
- ✅ IV Rank display
- ✅ Dividend/Earnings indicators (days until)
- ✅ Interactive position closing
- ✅ Multi-leg closing orders
- ✅ Configurable table columns (show/hide Greeks)

**Features**:
- Beta-weighted delta vs SPY
- Delta target warnings (delta neutral portfolio)
- Dividend/earnings countdown (e.g., "D 3", "E 7")
- Position closing workflow with dry-run
- Config-driven display (`.config/tastytrade/ttcli.cfg`)

**Priority**: **MEDIUM** (Enhances existing features)

**Estimated Port Time**: 4-5 hours (Session 160)

---

### 6. Plotting (`plot.py` - 199 LOC)

**What they have**:
- ✅ Candle charts for stocks/ETFs
- ✅ Candle charts for crypto
- ✅ Candle charts for futures
- ✅ Multiple timeframes (1m, 5m, 15m, 30m, 1h, 1d, 1mo, 1y)
- ✅ GNU plot integration

**Commands**:
```bash
# Plot stock candles
tt plot stock SPY --width 30m

# Plot crypto candles
tt plot crypto BTC --width 1h

# Plot futures candles
tt plot future /ES --width 1d
```

**Features**:
- DXLink candle streaming
- GNU plot visualization (terminal charts)
- Automatic timeframe selection
- Trading hours filtering

**Priority**: **LOW** (Nice to have, V3 feature)

**Estimated Port Time**: 3-4 hours (V3 Milestone)

---

### 7. Watchlist (`watchlist.py` - 15 LOC)

**Status**: ⚠️ **NOT IMPLEMENTED** in tastytrade-cli either!

```python
def filter(name: str):
    print_warning("This functionality hasn't been implemented yet!")
```

**Priority**: **LOW** (Not available in tastytrade-cli)

---

## Priority Ranking for Milestone 2

### Tier 1: CRITICAL (Milestone 2 - Sessions 157-159)

1. **Order Management** (`order.py`) - 4-6 hours
   - Live orders display/modification/cancellation
   - Order history with filtering
   - **Why**: Prerequisite for trading automation

2. **Options Trading** (`option.py`) - 8-12 hours
   - Call/put buying/selling
   - Spreads and strangles
   - Delta-based strike selection
   - **Why**: Core TastyTrade methodology, ISSUE-003 blocker

3. **Margin Analysis** (`portfolio.py:margin()`) - 2-3 hours
   - Per-position margin breakdown
   - BP usage warnings
   - **Why**: Risk management essential

**Total Tier 1**: 14-21 hours (~3 sessions)

---

### Tier 2: IMPORTANT (Milestone 2 - Sessions 160-162)

4. **Portfolio Metrics Enhancements** - 4-5 hours
   - Beta-weighted delta
   - IV Rank display
   - Dividend/earnings indicators

5. **Simple Trading** (`trade.py`) - 3-4 hours
   - Stock/crypto/futures trading
   - GTC order support

**Total Tier 2**: 7-9 hours (~2 sessions)

---

### Tier 3: NICE TO HAVE (V3 Milestone)

6. **Plotting** (`plot.py`) - 3-4 hours
   - Candle charts (terminal-based)
   - **Defer to V3**: Web UI will have better charts

7. **Watchlist** - N/A (not implemented in tastytrade-cli)

---

## Milestone 2 Recommendation

**Focus**: Tier 1 features (14-21 hours, ~3 sessions)

### Session 157: Order Management
- Port `order.py` (live orders, history, modify/cancel)
- Add CLI commands: `contrarian orders live`, `contrarian orders history`
- **Deliverable**: Full order lifecycle management

### Session 158-159: Options Trading (Part 1 & 2)
- Port `option.py:call()`, `option.py:put()` (Session 158)
- Port `option.py:strangle()`, `option.py:chain()` (Session 159)
- Add CLI commands: `contrarian option call/put/strangle/chain`
- **Deliverable**: TastyTrade-style options trading

### Session 160: Margin Analysis
- Port `portfolio.py:margin()`
- Add VIX-adjusted BP warnings
- **Deliverable**: Risk management dashboard

**Total**: 3 sessions (14-21 hours)

**After Milestone 2**: We'll have complete trading automation foundation for ISSUE-003

---

## What Makes tastytrade-cli Special

### 1. Delta-Based Strike Selection
- You specify target delta (e.g., --delta 30)
- They fetch Greeks for ALL strikes
- Auto-select closest strike to target delta
- **Use Case**: "Sell a 16-delta strangle" (TastyTrade methodology)

### 2. Spread Width Parameter
- Single `--width` parameter converts singles to spreads
- **Example**: `tt option call SPY 10 --delta 30 --width 5` = call spread
- **Example**: `tt option strangle /ES -1 --delta 16 --width 50` = iron condor

### 3. TastyTrade Methodology Built-In
- Default 45 DTE for options
- Monthlies vs weeklies filtering
- Beta-weighted delta tracking
- VIX-adjusted BP warnings
- Per-position BP % limits

### 4. Config-Driven UX
- `.config/tastytrade/ttcli.cfg` controls everything
- Show/hide table columns
- Set default DTE, BP limits, delta targets
- **Example**: Hide gamma column in positions table

---

## Implementation Strategy

### Phase 1: Study (Session 156, 30 min)
- ✅ Clone tastytrade-cli
- ✅ Analyze all modules
- ✅ Create this gap analysis document

### Phase 2: Port (Sessions 157-160, 14-21 hours)
- Session 157: Order management (4-6 hours)
- Session 158-159: Options trading (8-12 hours)
- Session 160: Margin analysis (2-3 hours)

### Phase 3: Test (Sessions 161-162)
- Session 161: Integration tests, Ground Truth validation
- Session 162: User testing, Milestone 2 review

### Phase 4: Milestone 2 Complete
- **Deliverable**: Suggestion mode trading UI ready
- **Next**: 30-day paper trading validation (Milestone 3)

---

## Key Takeaways

1. **We ported the RIGHT features first** ✅
   - RenewableSession, Greeks, enrichment = foundation
   - Portfolio display, history, balance = analytics

2. **Missing features are TRADING-focused** ⚠️
   - Order management (critical)
   - Options strategies (critical)
   - Margin analysis (important)

3. **Estimated completion**: 3 sessions (Sessions 157-160)
   - Order management: 1 session
   - Options trading: 2 sessions
   - Margin analysis: <1 session

4. **After Milestone 2**: Complete trading automation foundation
   - ISSUE-003 (suggestion mode) ready to implement
   - All TastyTrade methodology features available
   - Ready for 30-day paper trading validation

---

## Next Steps

**Immediate** (Session 156):
1. Review this analysis with user
2. Get approval for Milestone 2 priorities
3. Start ISSUE-006: Port order management

**Session 157**:
- Port `order.py` completely
- Add `contrarian orders live/history` CLI commands
- Test with live account

**Sessions 158-159**:
- Port `option.py` (call, put, strangle, chain)
- Add delta-based strike selection
- Add spread width parameter
- Test with paper trading

**Session 160**:
- Port margin analysis
- Add VIX-adjusted warnings
- Milestone 2 review prep

---

**Total Gap**: ~20 hours of porting work (3-4 sessions)
**Impact**: Complete trading automation foundation
**Risk**: Low (production-tested code, line-by-line port)
**Benefit**: TastyTrade methodology fully integrated
