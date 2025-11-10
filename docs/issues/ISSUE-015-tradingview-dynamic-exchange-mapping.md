# ISSUE-015: Replace Hardcoded TradingView Exchange Mapping with Dynamic Instrument Data

**Created**: 2025-11-09 (Session Current)
**Status**: CLOSED
**Completed**: 2025-11-09 (Session Current)
**Priority**: MEDIUM
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

The TradingView integration uses a hardcoded `exchangeMap` object in JavaScript to map ticker symbols to TradingView exchange-prefixed symbols (e.g., `'AAPL'` → `'NASDAQ:AAPL'`). This approach has several limitations:

1. **Static Maintenance Burden**: Adding new symbols requires code changes and deployment
2. **Incorrect Fallback**: Unknown symbols default to `NASDAQ:${ticker}`, which is incorrect for NYSE, AMEX, and other exchange symbols
3. **Data Duplication**: Exchange information already exists in the database (`InstrumentEntity.alpacaExchange`), populated by the Alpaca Assets API
4. **User Input Handling**: Position Calculator allows user-input tickers that may not exist in the hardcoded map

**Current Implementation**:
- File: `src/main/resources/static/assets/js/tradingview-integration.js` (lines 17-43)
- 18 hardcoded mappings (NASDAQ, NYSE, AMEX symbols)
- Used in 4 locations: dashboard widgets and Position Calculator modal
- Fallback: `NASDAQ:${ticker}` for unmapped symbols

---

## Root Cause

The TradingView integration was implemented before the Alpaca Assets API integration (ISSUE-014) provided dynamic exchange metadata. The hardcoded map was a pragmatic initial solution, but now that exchange data is available in the database, the map should be removed and replaced with dynamic lookups.

---

## Approach

**Phase 1: Backend API** (Expose exchange data)
1. Add `exchange` getter to `InstrumentDto` to expose `alpaca_exchange` field
2. Create/update REST endpoint `GET /api/instruments/{ticker}` to return instrument with exchange
3. Update `InstrumentController` to expose the endpoint
4. Run database query to verify all actively-used instruments have `alpaca_exchange` populated

**Phase 2: Frontend Refactor** (Dynamic lookup with caching)
1. Create `fetchInstrumentExchange(ticker)` async function in `tradingview-integration.js`
2. Implement client-side cache (Map) to store ticker→exchange mappings
3. Update `convertToTradingViewSymbol(ticker)` to:
   - Return async Promise (breaking change - requires refactoring callers)
   - Check cache first
   - Fetch from backend API if not cached
   - Fall back to `NASDAQ:${ticker}` with console warning if API fails
4. Refactor all callers to handle async conversion:
   - `initializeAllTradingViewWidgets()` - pre-load known symbols
   - `updateTradingViewSymbol()` - fetch on-demand
   - `updateAdvancedChartSymbol()` - fetch on-demand

**Phase 3: Testing & Validation**
1. Unit tests for exchange lookup (cache hits, cache misses, fallback)
2. Integration tests for all 18 original hardcoded symbols (verify correct exchange)
3. Test unknown ticker behavior (new user-input ticker not in database)
4. Test API failure fallback behavior

---

## Success Criteria

- [ ] Backend: `InstrumentDto` includes `exchange` field
- [ ] Backend: REST endpoint `GET /api/instruments/{ticker}` returns instrument with exchange
- [ ] Backend: All actively-used instruments have `alpaca_exchange` populated
- [ ] Frontend: Hardcoded `exchangeMap` removed from `tradingview-integration.js`
- [ ] Frontend: `convertToTradingViewSymbol()` refactored to async with backend lookup
- [ ] Frontend: Client-side cache implemented (no duplicate API calls for same ticker)
- [ ] Frontend: Fallback behavior logs warning when API fails or exchange is unknown
- [ ] All TradingView widgets (dashboard + Position Calculator) render correctly
- [ ] All original 18 hardcoded symbols map to correct exchanges
- [ ] Unknown ticker behavior tested (fallback to NASDAQ with warning)
- [ ] Code coverage: New functions have >80% coverage

---

## Acceptance Tests

```javascript
// Test 1: Cache hit (no API call)
async function testExchangeCacheHit() {
  const symbol1 = await convertToTradingViewSymbol('AAPL');
  assert(symbol1 === 'NASDAQ:AAPL');

  // Second call should use cache (no network call)
  const symbol2 = await convertToTradingViewSymbol('AAPL');
  assert(symbol2 === 'NASDAQ:AAPL');
  assert(networkCallCount === 1); // Only 1 API call total
}

// Test 2: Correct exchange for all original symbols
async function testHardcodedSymbolMapping() {
  const mappings = {
    'AAPL': 'NASDAQ:AAPL',
    'MSFT': 'NASDAQ:MSFT',
    'SPY': 'NYSE:SPY',
    'GDX': 'AMEX:GDX',
    'SLV': 'AMEX:SLV',
  };

  for (const [ticker, expected] of Object.entries(mappings)) {
    const result = await convertToTradingViewSymbol(ticker);
    assert(result === expected, `${ticker}: got ${result}, expected ${expected}`);
  }
}

// Test 3: Fallback for unknown ticker
async function testUnknownTickerFallback() {
  const mockApiToFail = true; // Simulate API failure
  const symbol = await convertToTradingViewSymbol('UNKNOWN');
  assert(symbol === 'NASDAQ:UNKNOWN'); // Falls back to NASDAQ
  assert(consoleWarning.includes('API failed'));
}
```

---

## Related Issues

- Blocks: None
- Blocked by: ISSUE-014 (Alpaca Assets API integration) - ✅ COMPLETED
- Related: ISSUE-014 provides the exchange data via `AlpacaService` and `InstrumentEntity`

---

## Notes

### Implementation Considerations

1. **Alpaca Exchange Format vs TradingView Format**
   - Alpaca returns values like "NYSE", "NASDAQ", "AMEX", "NYSEARCA"
   - Verify these match TradingView's expected exchange codes
   - May need mapping layer (e.g., "NYSEARCA" → "AMEX")

2. **Async Conversion Breaking Change**
   - Changing `convertToTradingViewSymbol()` from sync to async will require refactoring all callers
   - Current callers:
     - `initializeAllTradingViewWidgets()` (line 128)
     - `updateTradingViewSymbol()` (line 155)
     - `updateAdvancedChartSymbol()` (line 274)

3. **Data Availability**
   - Need to verify all actively-used instruments have `alpaca_exchange` populated
   - Consider one-time data migration script if gaps exist

4. **Rate Limiting**
   - Alpaca Assets API: 200 requests/min, 10 requests/sec
   - With caching, shouldn't be an issue for typical usage
   - Monitor if Position Calculator (user-input tickers) creates excessive API calls

5. **Cache Invalidation**
   - Cache should persist for entire session (page reload clears it)
   - Consider TTL (time-to-live) if exchange mappings change during trading day

### Files to Modify

| File | Changes |
|------|---------|
| `src/main/java/co/grtk/srcprofit/dto/InstrumentDto.java` | Add `exchange` getter |
| `src/main/java/co/grtk/srcprofit/controller/InstrumentController.java` | Add REST endpoint |
| `src/main/resources/static/assets/js/tradingview-integration.js` | Remove `exchangeMap`, add async lookup + cache |
| Tests | Add unit/integration tests |

### Related Code References

- **Current Implementation**: `src/main/resources/static/assets/js/tradingview-integration.js:17-43`
- **Widget Initialization**: `src/main/resources/static/assets/js/tradingview-integration.js:128, 155, 274`
- **Database Entity**: `src/main/java/co/grtk/srcprofit/entity/InstrumentEntity.java:101` (alpacaExchange field)
- **Service Layer**: `src/main/java/co/grtk/srcprofit/service/AlpacaService.java:95-135`
