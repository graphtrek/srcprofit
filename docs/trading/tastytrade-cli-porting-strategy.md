# TastyTrade-CLI Porting Strategy - Complete Port, No Shortcuts

**Created**: Session 143 (2025-10-17)
**Lesson**: "You give up fast when trying to get the data from the broker"

---

## The Problem

Session 141 failed because we didn't complete the port:
- Looked at tastytrade-cli code ✅
- Understood the pattern ✅
- Wrote "similar" code ❌ **WRONG APPROACH**
- It failed silently ❌
- We moved on ❌ **GAVE UP**

---

## The Solution

**LINE-BY-LINE PORT** when needed. Don't write "similar" code, port their EXACT implementation.

---

## Porting Methodology

### Step 1: Study Reference Implementation COMPLETELY

**Before writing ANY code**, read and understand:

```bash
# Clone tastytrade-cli (if not done)
cd /tmp
git clone https://github.com/tastyware/tastytrade-cli
cd tastytrade-cli

# Study these files IN ORDER:
1. ttcli/utils.py               # RenewableSession, listen_events, helpers
2. ttcli/tastytrade_client.py   # Main client wrapper
3. ttcli/portfolio.py           # Position handling (lines 86-112)
4. ttcli/option.py              # Option trading patterns
5. ttcli/trade.py               # Trade execution patterns
```

**Document**:
- Every class they use
- Every helper function
- Every error they handle
- Every retry pattern
- Every assumption they make

### Step 2: Port Infrastructure First

**Priority 1: RenewableSession** (utils.py)
```python
# Don't write our own session management
# Port their RenewableSession class EXACTLY

# Read: tastytrade-cli/ttcli/utils.py lines 30-80
# Copy structure, adapt to our codebase

class RenewableSession:
    """
    Port from tastytrade-cli/ttcli/utils.py

    Handles:
    - Token refresh
    - Automatic renewal
    - Session validation
    """
    # Port their implementation line-by-line
    pass
```

**Priority 2: listen_events** (utils.py)
```python
# Don't write our own event listening
# Port their listen_events function EXACTLY

# Read: tastytrade-cli/ttcli/utils.py lines 100-150
# This is how they batch Greeks fetching

async def listen_events(streamer, symbols):
    """
    Port from tastytrade-cli/ttcli/utils.py

    Handles:
    - Batch subscription
    - Event collection
    - Exit conditions
    """
    # Port their implementation line-by-line
    pass
```

### Step 3: Port Adapter Methods ONE AT A TIME

**For EACH method**:

1. **Read their implementation completely**
   ```bash
   # Example: Porting position enrichment
   cat tastytrade-cli/ttcli/portfolio.py | sed -n '86,112p'
   ```

2. **Understand their approach**
   - What SDK methods do they call?
   - What parameters do they pass?
   - What do they do with responses?
   - What errors do they handle?

3. **Port their code (don't write "similar" code)**
   ```python
   # WRONG (what we did in Session 141):
   def our_implementation(self):
       # Our approach based on understanding their pattern
       result = self.sdk.method()  # Similar but not identical
       return result

   # RIGHT (what we should do):
   def port_from_tastytrade_cli(self):
       """
       Ported from tastytrade-cli/ttcli/portfolio.py lines 86-112

       Changes:
       - Renamed variables to match our convention
       - Adapted to our class structure
       - Added type hints

       Otherwise IDENTICAL to their implementation.
       """
       # Their code, our variable names
       pass
   ```

4. **Test against their results**
   ```python
   def test_port_equivalence():
       # Run their implementation
       their_result = run_tastytrade_cli_code()

       # Run our port
       our_result = our_ported_implementation()

       # Must be identical
       assert our_result == their_result
   ```

### Step 4: When Stuck, ASK FOR HELP

**Don't give up**. Try these in order:

1. **Re-read their code** - Maybe you missed something
2. **Compare line-by-line** - Find the difference
3. **Use tastytrade-specialist subagent** - Expert help
4. **Ask user** - They can run tastytrade-cli for comparison
5. **Debug systematically** - Print their values vs ours

**Never** move on with "close enough". Either it works EXACTLY like theirs, or document the blocker.

---

## Porting Checklist

For EACH feature being ported:

- [ ] Read reference implementation COMPLETELY
- [ ] Document their approach
- [ ] Port their code (not "similar" code)
- [ ] Test for equivalence
- [ ] Validate with Ground Truth
- [ ] If stuck, escalate (don't give up)

---

## Examples

### Example 1: Position Enrichment (Session 141 fix)

**Reference**: `tastytrade-cli/ttcli/portfolio.py` lines 86-112

**Their approach**:
```python
# tastytrade-cli code (simplified)
def enrich_positions(positions):
    # Group by instrument type
    by_type = defaultdict(list)
    for p in positions:
        by_type[p['instrument-type']].append(p['symbol'])

    # Fetch instruments per type
    enriched = {}
    if by_type['Equity Option']:
        opts = Option.get(session, by_type['Equity Option'])
        for opt in opts:
            enriched[opt.symbol] = opt.streamer_symbol

    # Similar for FutureOption, Equity, etc.

    # Enrich positions
    for p in positions:
        p['streamer-symbol'] = enriched.get(p['symbol'])

    return positions
```

**Our port** (Session 144):
```python
# Ported from tastytrade-cli/ttcli/portfolio.py lines 86-112
def enrich_streamer_symbols(self, positions):
    """
    Port from tastytrade-cli - EXACT same logic

    Changes from their code:
    - Class method instead of function
    - Type hints added
    - Variable names match our style

    Otherwise IDENTICAL to tastytrade-cli implementation.
    """
    # Their code, our formatting
    by_type = defaultdict(list)
    for position in positions:
        instrument_type = position.get('instrument-type')
        symbol = position.get('symbol')
        by_type[instrument_type].append(symbol)

    enriched = {}

    # Equity Options
    if by_type['Equity Option']:
        from tastytrade.instruments import Option
        options = Option.get_options(self.session, by_type['Equity Option'])
        for opt in options:
            enriched[opt.symbol] = opt.streamer_symbol

    # Future Options
    if by_type['Future Option']:
        from tastytrade.instruments import FutureOption
        fut_opts = FutureOption.get_future_options(self.session, by_type['Future Option'])
        for fo in fut_opts:
            enriched[fo.symbol] = fo.streamer_symbol

    # Continue for all instrument types...

    # Enrich original positions
    for position in positions:
        symbol = position.get('symbol')
        position['streamer-symbol'] = enriched.get(symbol)

    return positions
```

**Validation**:
```python
def test_enrichment_matches_tastytrade_cli():
    # Get positions
    positions = adapter.get_positions()

    # Our enrichment
    enriched = adapter.enrich_streamer_symbols(positions)
    enriched_count = sum(1 for p in enriched if p.get('streamer-symbol'))

    # Their enrichment (run tastytrade-cli)
    their_count = run_tastytrade_cli_enrichment()

    # Must match exactly
    assert enriched_count == their_count
    assert enriched_count == len(positions)  # 100% coverage
```

---

## Key Lessons

### Lesson 1: "Similar" ≠ Correct
Writing code that's "similar" to theirs doesn't work. Port their EXACT logic.

### Lesson 2: Don't Give Up
If it doesn't work, debug systematically. Compare line-by-line. Ask for help.

### Lesson 3: They Solved This Already
tastytrade-cli is production code with 2,727 LOC. They've handled all the edge cases. Trust their implementation.

### Lesson 4: Test for Equivalence
Your port should produce IDENTICAL results to their code. If it doesn't, it's wrong.

### Lesson 5: Document Differences
If you MUST change something, document WHY and test that it still works.

---

## Session 144 Execution Plan

### Phase 1: Study (30 min)
- Read `tastytrade-cli/ttcli/utils.py` completely
- Read `tastytrade-cli/ttcli/portfolio.py` lines 86-112
- Document every pattern they use

### Phase 2: Port RenewableSession (45 min)
- Copy their class structure
- Adapt to our codebase
- Test token renewal works

### Phase 3: Port Enrichment (60 min)
- Copy their position enrichment logic
- Port for all 5 instrument types
- Test with real positions

### Phase 4: Validation (45 min)
- Run their code (tastytrade-cli)
- Run our code
- Compare results
- Must be IDENTICAL

### Total: 3 hours
**Success criteria**: 501/501 positions enriched (100% coverage)

---

## Bottom Line

**Port working code completely.**
**Don't write "similar" code.**
**Don't give up when stuck.**
**Test for exact equivalence.**

Session 141 failed because we didn't follow these rules. Session 144 will succeed because we will.

---

**Next**: Session 144 - Execute this strategy, complete the port
