# tastytrade-cli option.py Analysis

**Date**: 2025-10-18 (Session 158)
**Source**: `/tmp/tastytrade-cli/ttcli/option.py` (1,068 LOC)
**Purpose**: Understand architecture for Clean Architecture port to contrarian

---

## 📊 High-Level Overview

**File Structure**:
- **Lines 1-45**: Imports (tastytrade SDK, Rich tables, Typer CLI)
- **Lines 47-114**: Expiration choosers (equity + futures)
- **Lines 120-354**: `call` command (naked + spreads)
- **Lines 356-591**: `put` command (naked + spreads)
- **Lines 593-913**: `strangle` command (naked + iron condor)
- **Lines 915-1068**: `chain` command (options chain display)

**Command Count**: 4 commands (call, put, strangle, chain)

---

## 🎯 Core Patterns (Delta-Based Strike Selection)

### Pattern 1: Delta-Based Strike Selection (Lines 184-199)

**Used in**: `call`, `put`, `strangle`

**Algorithm**:
```python
# 1. Subscribe to Greeks for ALL strikes in expiration
dxfeeds = [s.call_streamer_symbol for s in subchain.strikes]
async with DXLinkStreamer(sesh) as streamer:
    await streamer.subscribe(Greeks, dxfeeds)
    async for greek in streamer.listen(Greeks):
        greeks_dict[greek.event_symbol] = greek
        if len(greeks_dict) == len(dxfeeds):
            break  # Got all Greeks

# 2. Find strike with closest delta to target
greeks = list(greeks_dict.values())
selected = min(greeks, key=lambda g: abs(g.delta * 100 - Decimal(delta)))

# 3. Map back to strike price
strike = next(
    s.strike_price
    for s in subchain.strikes
    if s.call_streamer_symbol == selected.event_symbol
)
```

**Key Insights**:
- ✅ **Batch fetch ALL Greeks** (not one-by-one) for efficiency
- ✅ **Delta × 100** (SDK returns 0.30, display shows 30)
- ✅ **Absolute value matching** (`abs(g.delta * 100 - target)`)
- ✅ **Break early** when all Greeks received (performance)

**Put delta quirk** (Line 429):
```python
# Puts have NEGATIVE delta, so ADD instead of SUBTRACT
selected = min(greeks, key=lambda g: abs(g.delta * 100 + Decimal(delta)))
```

---

### Pattern 2: Expiration Selection (Lines 47-114)

**Equity options** (Lines 47-80):
- Filter monthlies UNLESS `--weeklies` flag
- If `--dte` specified: Find closest expiration by days
- Else: Interactive chooser with TastyTrade monthly default

**Futures options** (Lines 83-114):
- Filter by `expiration_type != "Weekly"` (not `is_monthly()`)
- Default to 45 DTE (TastyTrade methodology)
- Show underlying symbol (futures chain nesting)

**Key Insights**:
- ✅ **Monthlies preferred** (TastyTrade methodology: better liquidity)
- ✅ **45 DTE default** for futures (mechanical trading standard)
- ✅ **Closest DTE match** using `min(..., key=lambda exp: abs(...))`

---

### Pattern 3: Spread Building (Lines 202-210, 438-445)

**Call spread** (Lines 202-210):
```python
# Find strike at (selected_strike + width)
spread_strike = next(
    s for s in subchain.strikes
    if s.strike_price == strike + width
)

# Fetch market data for both legs
dxfeeds = [strike_symbol, spread_strike.call]
data = get_market_data_by_type(sesh, options=dxfeeds)

# Calculate spread price (net debit/credit)
bid = data_dict[short_leg].bid - data_dict[long_leg].ask
ask = data_dict[short_leg].ask - data_dict[long_leg].bid
```

**Put spread** (Lines 438-445):
```python
# Find strike at (selected_strike - width)
spread_strike = next(
    s for s in subchain.strikes
    if s.strike_price == strike - width
)
```

**Key Insights**:
- ✅ **Call spreads**: long leg ABOVE short leg (`strike + width`)
- ✅ **Put spreads**: long leg BELOW short leg (`strike - width`)
- ✅ **Net pricing**: short_bid - long_ask (tightest debit), short_ask - long_bid (widest credit)
- ✅ **Error handling**: `StopIteration` if strike doesn't exist

---

### Pattern 4: Iron Condor Building (Lines 702-749)

**Algorithm** (strangle with `--width`):
```python
# 1. Find put spread strikes
put_spread_strike = next(
    s for s in subchain.strikes
    if s.strike_price == put_strike.strike_price - width
)

# 2. Find call spread strikes
call_spread_strike = next(
    s for s in subchain.strikes
    if s.strike_price == call_strike.strike_price + width
)

# 3. Fetch market data for 4 legs
dxfeeds = [
    call_strike.call,
    put_strike.put,
    put_spread_strike.put,
    call_spread_strike.call,
]

# 4. Calculate net credit
bid = (
    data_dict[call_strike.call].bid
    + data_dict[put_strike.put].bid
    - data_dict[put_spread_strike.put].ask
    - data_dict[call_spread_strike.call].ask
)
```

**Key Insights**:
- ✅ **4-leg order** (call credit spread + put credit spread)
- ✅ **Net credit calculation** (short legs bid - long legs ask)
- ✅ **Symmetric wings** (same width on both sides)

---

### Pattern 5: Order Leg Building (Lines 262-282, 498-518, 794-823)

**Naked option**:
```python
option = TastytradeOption.get(sesh, symbol)
legs = [
    option.build_leg(
        Decimal(abs(quantity)),
        OrderAction.SELL_TO_OPEN if quantity < 0 else OrderAction.BUY_TO_OPEN,
    )
]
```

**Vertical spread** (2 legs):
```python
res = TastytradeOption.get(sesh, [short_symbol, long_symbol])
res.sort(key=lambda x: x.strike_price)  # OR reverse=True for puts
legs = [
    res[0].build_leg(abs(q), OrderAction.SELL_TO_OPEN if q < 0 else BUY_TO_OPEN),
    res[1].build_leg(abs(q), OrderAction.BUY_TO_OPEN if q < 0 else SELL_TO_OPEN),
]
```

**Iron condor** (4 legs, lines 794-812):
```python
options = TastytradeOption.get(sesh, [put1, put2, call1, call2])
options.sort(key=lambda o: o.strike_price)  # Ascending order
legs = [
    options[0].build_leg(abs(q), BUY if q < 0 else SELL),   # Lowest put
    options[1].build_leg(abs(q), SELL if q < 0 else BUY),   # Higher put
    options[2].build_leg(abs(q), SELL if q < 0 else BUY),   # Lower call
    options[3].build_leg(abs(q), BUY if q < 0 else SELL),   # Highest call
]
```

**Key Insights**:
- ✅ **Quantity sign convention**: Negative = SELL_TO_OPEN (credit), Positive = BUY_TO_OPEN (debit)
- ✅ **Sorting critical**: Must build legs in correct order (strike price ascending)
- ✅ **Action inversion**: Short leg SELL, long leg BUY (spreads alternate actions)

---

### Pattern 6: Buying Power Validation (Lines 297-351)

**Algorithm**:
```python
# 1. Dry-run order
data = acc.place_order(sesh, order, dry_run=True)

# 2. Calculate BP impact
nl = acc.get_balances(sesh).net_liquidating_value
bp = data.buying_power_effect.change_in_buying_power
percent = abs(bp) / nl * Decimal(100)

# 3. Check config threshold
warn_percent = sesh.config.getfloat(
    "portfolio", "bp-max-percent-per-position", fallback=None
)
if warn_percent and percent > warn_percent:
    print_warning(f"BP usage above target of {warn_percent}%!")

# 4. Show broker warnings
if data.warnings:
    for warning in data.warnings:
        print_warning(warning.message)
```

**Key Insights**:
- ✅ **Always dry-run first** (catches errors before submission)
- ✅ **BP percentage check** (position sizing enforcement)
- ✅ **Broker warnings** (margin calls, maintenance requirements)
- ✅ **User confirmation** (`get_confirmation()` before live order)

---

### Pattern 7: Options Chain Display (Lines 915-1068)

**Algorithm**:
```python
# 1. Get current price (Trade event)
await streamer.subscribe(Trade, [symbol])
trade = await streamer.get_event(Trade)

# 2. Center strikes around current price
mid_index = 0
while subchain.strikes[mid_index].strike_price < trade.price:
    mid_index += 1
half = strikes // 2
all_strikes = subchain.strikes[mid_index - half : mid_index + half]

# 3. Fetch Greeks, Quotes, Summary, Trade in PARALLEL
greeks_task = asyncio.create_task(listen_events(dxfeeds, Greeks, streamer))
quote_task = asyncio.create_task(listen_events(dxfeeds, Quote, streamer))
summary_task = asyncio.create_task(listen_events(dxfeeds, Summary, streamer))
trade_task = asyncio.create_task(listen_events(dxfeeds, Trade, streamer))
await asyncio.gather(*tasks)
```

**Key Insights**:
- ✅ **ATM-centered display** (find strikes around current price)
- ✅ **Parallel data fetching** (`asyncio.gather()` for speed)
- ✅ **Configurable columns** (delta, theta, OI, volume toggles)
- ✅ **`listen_events()` helper** (batch fetch pattern from utils.py)

---

## 🏗️ Architecture Analysis

### Current Architecture (Monolithic CLI)

```
┌─────────────────────────────────────────────┐
│          option.py (1,068 LOC)              │
├─────────────────────────────────────────────┤
│ • CLI parsing (Typer)                       │
│ • Business logic (delta selection)          │
│ • API calls (tastytrade SDK)                │
│ • Display logic (Rich tables)               │
│ • Order building (SDK models)               │
│ • Risk validation (BP checks)               │
└─────────────────────────────────────────────┘
```

**Problems**:
- ❌ **Not testable** (no unit tests possible without mocking CLI)
- ❌ **Not reusable** (can't use delta logic in automation)
- ❌ **Not composable** (can't build wheel strategy from parts)

---

### Target Architecture (Clean Architecture)

```
┌─────────────────────────────────────────────┐
│         UI Layer (contrarian CLI)           │
│  • typer commands (call, put, strangle)     │
│  • Rich table formatting                    │
│  • User input prompts                       │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│    Application Layer (Use Cases)            │
│  • SelectStrikeByDelta                      │
│  • BuildVerticalSpread                      │
│  • BuildIronCondor                          │
│  • ValidatePositionSize                     │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│      Domain Layer (Business Logic)          │
│  • OptionsStrategy                          │
│  • StrikeSelector                           │
│  • SpreadBuilder                            │
│  • GreeksCalculator                         │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│   Adapter Layer (TastyTradeOptionsAdapter)  │
│  • fetch_chain()                            │
│  • fetch_greeks()                           │
│  • place_options_order()                    │
│  • get_market_data()                        │
└─────────────────────────────────────────────┘
```

**Benefits**:
- ✅ **Testable** (domain logic has NO dependencies)
- ✅ **Reusable** (wheel strategy uses same delta logic)
- ✅ **Composable** (strategy = put_strategy + call_strategy)

---

## 📋 Port Checklist

### Phase 1: Domain Models (Session 158, 2h)

**Models to create**:
- [ ] `OptionChain` (expiration, strikes, underlying)
- [ ] `OptionStrike` (call/put symbols, strike price)
- [ ] `OptionGreeks` (delta, theta, gamma, vega, rho)
- [ ] `OptionQuote` (bid, ask, mid, last)
- [ ] `OptionLeg` (symbol, quantity, action, strike, expiration)
- [ ] `SpreadType` (enum: VERTICAL_CALL, VERTICAL_PUT, IRON_CONDOR, STRANGLE)

**Value objects**:
- [ ] `Delta` (validate -99 to +99)
- [ ] `DTE` (days to expiration)
- [ ] `StrikePrice` (Decimal)

### Phase 2: Domain Services (Session 158, 2-3h)

**Services to implement**:
- [ ] `StrikeSelector` (delta-based, strike-based, ATM)
- [ ] `ExpirationSelector` (DTE-based, monthly filter)
- [ ] `SpreadPricing` (net debit/credit calculator)
- [ ] `GreeksAggregator` (sum Greeks across spread legs)

**Key methods**:
```python
class StrikeSelector:
    def select_by_delta(
        self,
        chain: OptionChain,
        target_delta: Delta,
        option_type: OptionType,
    ) -> OptionStrike:
        """Find strike with closest delta to target (lines 184-199)."""
        pass

class SpreadPricing:
    def calculate_vertical_spread_price(
        self,
        short_leg: OptionQuote,
        long_leg: OptionQuote,
    ) -> tuple[Decimal, Decimal]:
        """Return (bid, ask) for vertical spread (lines 217-218)."""
        return (
            short_leg.bid - long_leg.ask,  # Tightest fill
            short_leg.ask - long_leg.bid,  # Widest fill
        )
```

### Phase 3: Adapter Extensions (Session 159, 2h)

**New methods for `TastyTradeAdapter`**:
- [ ] `fetch_option_chain(symbol, dte, weeklies)` → OptionChain
- [ ] `fetch_greeks_batch(symbols)` → dict[str, OptionGreeks]
- [ ] `fetch_market_data_batch(symbols)` → dict[str, OptionQuote]
- [ ] `place_options_order(legs, price, gtc)` → OrderConfirmation

**Pattern to port**: `listen_events()` helper (lines 1013-1032)

### Phase 4: Application Layer (Session 159, 2h)

**Use cases**:
- [ ] `SelectStrikeByDelta` (encapsulates lines 184-199)
- [ ] `BuildVerticalSpread` (encapsulates lines 202-282)
- [ ] `BuildIronCondor` (encapsulates lines 702-823)
- [ ] `ValidatePositionSize` (encapsulates lines 297-351)

### Phase 5: UI Layer (Session 159, 1-2h)

**CLI commands** (reuse existing):
- [ ] `contrarian trade call` (port lines 125-354)
- [ ] `contrarian trade put` (port lines 361-591)
- [ ] `contrarian trade strangle` (port lines 598-913)
- [ ] `contrarian options chain` (port lines 915-1068)

### Phase 6: Testing (Session 159, 2h)

**Test coverage**:
- [ ] Unit tests for domain logic (StrikeSelector, SpreadPricing)
- [ ] Integration tests with mock adapter
- [ ] E2E tests with real broker data
- [ ] Ground Truth validation (compare with tastytrade-cli output)

---

## 🎓 Key Lessons

### Lesson 1: Delta Sign Convention

**Calls**: Positive delta (0 to +100)
**Puts**: Negative delta (0 to -100)

**Code quirk** (lines 193 vs 429):
```python
# Call: subtract target from delta
min(greeks, key=lambda g: abs(g.delta * 100 - Decimal(delta)))

# Put: ADD target to delta (because delta is negative)
min(greeks, key=lambda g: abs(g.delta * 100 + Decimal(delta)))
```

**Why**: If user wants -30 delta put, SDK returns delta=-0.30. We add 30 to get 0, minimizing abs().

### Lesson 2: Spread Direction Matters

**Call spreads**: Long leg ABOVE short leg (`strike + width`)
**Put spreads**: Long leg BELOW short leg (`strike - width`)

**Why**: Max profit = width - premium for credit spreads. Structure must align with directional bias.

### Lesson 3: Leg Ordering Critical

**Always sort by strike price** before building legs (lines 261, 497, 792):
```python
res.sort(key=lambda x: x.strike_price)  # Ascending
```

**Why**: SDK builds order from legs in array order. Wrong order = rejected by broker.

### Lesson 4: Batch API Calls

**Pattern** (lines 1013-1026):
```python
# GOOD: Fetch all data types in parallel
greeks_task = asyncio.create_task(listen_events(...))
quote_task = asyncio.create_task(listen_events(...))
await asyncio.gather(*tasks)

# BAD: Sequential fetches (slow)
greeks = await fetch_greeks()
quotes = await fetch_quotes()
```

**Impact**: 4x faster chain display (parallel vs sequential).

### Lesson 5: Always Dry-Run First

**Every order** goes through dry-run (lines 292, 529, 834):
```python
try:
    data = acc.place_order(sesh, order, dry_run=True)
except TastytradeError as e:
    print_error(str(e))
    return
```

**Why**: Catches insufficient buying power, invalid spreads, maintenance margin violations BEFORE submission.

---

## 📐 Proposed Domain Model

### Core Classes

```python
# Domain models
@dataclass
class OptionChain:
    symbol: str
    expiration: date
    strikes: list[OptionStrike]
    underlying_price: Decimal
    is_monthly: bool
    dte: int

@dataclass
class OptionStrike:
    strike_price: Decimal
    call_symbol: str
    put_symbol: str
    call_greeks: OptionGreeks | None
    put_greeks: OptionGreeks | None

@dataclass
class OptionGreeks:
    delta: Decimal  # -1.0 to +1.0
    theta: Decimal
    gamma: Decimal
    vega: Decimal
    rho: Decimal

@dataclass
class OptionLeg:
    symbol: str
    quantity: int  # Negative = sell, positive = buy
    action: OrderAction
    strike: Decimal
    expiration: date
    option_type: OptionType  # CALL | PUT

# Domain services
class StrikeSelector:
    def select_by_delta(
        self,
        chain: OptionChain,
        target_delta: int,  # -99 to +99
        option_type: OptionType,
    ) -> OptionStrike:
        """Port of lines 184-199 (call) and 420-435 (put)."""
        pass

class SpreadBuilder:
    def build_vertical(
        self,
        chain: OptionChain,
        short_strike: Decimal,
        width: int,
        option_type: OptionType,
        quantity: int,
    ) -> list[OptionLeg]:
        """Port of lines 262-282 (call) and 498-518 (put)."""
        pass

    def build_iron_condor(
        self,
        chain: OptionChain,
        put_strike: Decimal,
        call_strike: Decimal,
        width: int,
        quantity: int,
    ) -> list[OptionLeg]:
        """Port of lines 794-823."""
        pass
```

---

## 🚀 Next Steps

**Immediate (Session 158, remaining time)**:
1. Create domain models (`OptionChain`, `OptionStrike`, `OptionGreeks`)
2. Implement `StrikeSelector.select_by_delta()` (port lines 184-199)
3. Write unit tests for delta selection logic

**Session 159**:
4. Implement `SpreadBuilder` (vertical, iron condor)
5. Extend `TastyTradeAdapter` with options methods
6. Port `listen_events()` helper from utils.py
7. Create application layer use cases

**Session 160**:
8. Port CLI commands (call, put, strangle, chain)
9. Integration tests with real broker
10. Ground Truth validation

---

**Analysis complete!** Ready to design Clean Architecture domain layer.

**Key insight**: Their delta selection algorithm (lines 184-199) is PRODUCTION-TESTED for 2+ years. Port it EXACTLY, don't "improve" it.

**Philosophy**: "Port the proven, improve the architecture, enable the future."
