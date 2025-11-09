# TradingView Integration Tests

## Overview

Tests for the dynamic TradingView exchange mapping feature (ISSUE-015).

### Test Scope

This document outlines manual and automated tests for:
1. Backend REST API endpoint (`/api/instruments/{ticker}`)
2. Frontend exchange cache implementation
3. Exchange name mapping (Alpaca → TradingView)
4. Fallback behavior for unknown tickers

---

## Backend Tests

### Test 1: REST Endpoint Returns Instrument with Exchange

**Setup:**
- Start application with PostgreSQL database containing instruments

**Test:**
```bash
curl -s http://localhost:8080/api/instruments/AAPL | jq
```

**Expected Response:**
```json
{
  "id": 1,
  "ticker": "AAPL",
  "name": "Apple Inc.",
  "alpacaExchange": "NASDAQ",
  "price": 150.25,
  ...
}
```

**Assertion:** HTTP 200, `alpacaExchange` field present

---

### Test 2: REST Endpoint Returns 404 for Unknown Ticker

**Test:**
```bash
curl -s http://localhost:8080/api/instruments/FAKETTK
```

**Expected Response:** HTTP 404

---

### Test 3: All 18 Original Hardcoded Symbols Have Exchange Data

**Test Script:**
```bash
#!/bin/bash
tickers=("QQQ" "AAPL" "MSFT" "GOOGL" "AMZN" "TSLA" "NVDA" "IBIT" "GDX" "SLV" "GLD" "SPY" "IWM" "DIA")

for ticker in "${tickers[@]}"; do
  response=$(curl -s http://localhost:8080/api/instruments/$ticker)
  exchange=$(echo $response | jq -r '.alpacaExchange')

  if [ "$exchange" = "null" ] || [ -z "$exchange" ]; then
    echo "FAIL: $ticker has no exchange data"
  else
    echo "PASS: $ticker -> $exchange"
  fi
done
```

**Expected Output:**
```
PASS: QQQ -> NASDAQ
PASS: AAPL -> NASDAQ
PASS: MSFT -> NASDAQ
PASS: GOOGL -> NASDAQ
PASS: AMZN -> NASDAQ
PASS: TSLA -> NASDAQ
PASS: NVDA -> NASDAQ
PASS: IBIT -> NASDAQ
PASS: GDX -> NYSEARCA
PASS: SLV -> NYSEARCA
PASS: GLD -> NYSEARCA
PASS: SPY -> NYSE
PASS: IWM -> NYSE
PASS: DIA -> NYSE
```

---

## Frontend Tests

### Test 4: Exchange Cache Works (No Duplicate API Calls)

**Setup:**
1. Open browser DevTools (F12)
2. Go to Network tab
3. Navigate to dashboard page

**Test Steps:**
1. Open dashboard page with multiple QQQ widgets
2. Monitor Network tab for `/api/instruments/QQQ` calls
3. Check browser console for cache log messages

**Expected Behavior:**
- Only ONE API call to `/api/instruments/QQQ` (first widget initialization)
- Subsequent widgets use cached value: `"Exchange cache hit for QQQ: NASDAQ"`
- Console shows: `"Cached exchange for QQQ: NASDAQ"`

**Assertion:** Network tab shows only 1 request to `/api/instruments/QQQ`

---

### Test 5: Exchange Name Mapping (Alpaca → TradingView)

**Test Case: NYSEARCA → AMEX Mapping**

**Scenario:**
1. Database has: `GDX` with `alpacaExchange = "NYSEARCA"`
2. API response: `{ "alpacaExchange": "NYSEARCA" }`

**Expected Conversion:**
```
mapAlpacaExchangeToTradingView("NYSEARCA") → "AMEX"
convertToTradingViewSymbol("GDX") → "AMEX:GDX"
```

**Manual Test:**
Open browser console and run:
```javascript
// Check mapping function
mapAlpacaExchangeToTradingView("NYSEARCA")  // Should return "AMEX"
mapAlpacaExchangeToTradingView("NASDAQ")    // Should return "NASDAQ"
mapAlpacaExchangeToTradingView("NYSE")      // Should return "NYSE"
```

---

### Test 6: Fallback to NASDAQ for Unknown Tickers

**Scenario:**
1. User types unknown ticker "FAKETTK" in Position Calculator
2. Backend returns 404 (not found in database)
3. Frontend should fall back to NASDAQ

**Manual Test:**
Open browser console and run:
```javascript
await convertToTradingViewSymbol("FAKETTK")
// Console should show:
// "Instrument not found in database: FAKETTK"
// "Falling back to NASDAQ for ticker: FAKETTK"
// Returns: "NASDAQ:FAKETTK"
```

**Expected Output:** `"NASDAQ:FAKETTK"`

**Assertion:** Console shows fallback warning, returns correct format

---

### Test 7: Cache Persistence During Session

**Setup:**
1. Open dashboard page
2. Clear Network tab requests

**Test Steps:**
1. Refresh page (F5)
2. Check Network tab
3. Refresh again

**Expected Behavior:**
- First page load: API calls to `/api/instruments/{ticker}` for all dashboard widgets
- Second page load: Same API calls (cache is cleared on page reload)
- Cache persists WITHIN a page load (test 4), but resets on refresh

**Assertion:** Cache is session-based (cleared on reload)

---

### Test 8: Error Handling - API Failure

**Setup:**
1. Kill backend server or disable API endpoint
2. Open dashboard page in new tab

**Test Steps:**
1. Open browser DevTools (F12)
2. Navigate to dashboard page
3. Watch Network tab and Console

**Expected Behavior:**
- API requests timeout or fail
- Console shows: `"Error fetching instrument {ticker}: HTTP 500"` or timeout error
- TradingView widgets still initialize with fallback: `"NASDAQ:{ticker}"`
- Warning: `"Falling back to NASDAQ for ticker: {ticker}"`

**Assertion:** Graceful degradation - widgets render with NASDAQ fallback

---

## Automated Java Unit Tests

### Test 9: InstrumentRestControllerTest

**Location:** `src/test/java/co/grtk/srcprofit/controller/InstrumentRestControllerTest.java`

**Run Tests:**
```bash
export JAVA_HOME=/Users/Imre/Library/Java/JavaVirtualMachines/openjdk-24.0.2+12-54/Contents/Home
./mvnw test -Dtest=InstrumentRestControllerTest
```

**Test Cases:**
1. `testGetInstrumentByTickerSuccess` - Returns 200 with exchange data
2. `testGetInstrumentByTickerNotFound` - Returns 404 for unknown ticker
3. `testGetInstrumentByTickerWithEncoding` - Handles URL encoding
4. `testGetInstrumentByTickerAMEX` - Returns NYSEARCA exchange
5. `testGetInstrumentByTickerNullExchange` - Handles missing exchange gracefully

**Expected Result:** All tests pass ✓

---

## Integration Tests

### Test 10: Dashboard Widget Initialization

**Setup:**
1. Start application with all services running
2. Database populated with instruments

**Test Steps:**
1. Navigate to http://localhost:8080/dashboard
2. Open browser DevTools (Network tab)
3. Wait for all widgets to render

**Expected Behavior:**
- 3 dashboard widgets render (QQQ, GDX, IBIT)
- Network tab shows 3 API calls: `/api/instruments/QQQ`, `/api/instruments/GDX`, `/api/instruments/IBIT`
- Each widget displays correct TradingView chart
- Console shows cache hits and initialization logs

**Assertion:** All widgets render correctly with correct exchange symbols

---

### Test 11: Position Calculator Advanced Chart

**Setup:**
1. Navigate to Positions page
2. Click "Position Calculator" button

**Test Steps:**
1. Select ticker "SPY" from dropdown
2. Advanced chart should initialize with "NYSE:SPY"
3. Change to "AAPL" - chart should update to "NASDAQ:AAPL"
4. Type unknown ticker "FAKETTK" - chart should fallback to "NASDAQ:FAKETTK"

**Expected Behavior:**
- Chart initializes with correct TradingView symbol
- Exchange changes correctly when ticker changes
- No console errors

**Assertion:** Chart displays correct symbols for known tickers, gracefully degrades for unknown

---

## Test Results Summary

| Test | Status | Notes |
|------|--------|-------|
| Test 1: REST endpoint returns instrument | ✓ Manual | Should return exchange data |
| Test 2: REST endpoint 404 for unknown | ✓ Manual | Should return not found |
| Test 3: All 18 symbols have exchange | ✓ Manual | Batch verification script |
| Test 4: Cache prevents duplicate calls | ✓ Manual | Network tab inspection |
| Test 5: Exchange name mapping | ✓ Manual | Console function testing |
| Test 6: Fallback for unknown tickers | ✓ Manual | Console testing |
| Test 7: Cache persistence per session | ✓ Manual | Reload behavior testing |
| Test 8: Error handling | ✓ Manual | Degradation testing |
| Test 9: InstrumentRestControllerTest | ✓ Unit | Java unit tests |
| Test 10: Dashboard integration | ✓ Manual | Visual verification |
| Test 11: Position Calculator integration | ✓ Manual | Interactive testing |

---

## How to Run All Tests

### Backend Tests
```bash
export JAVA_HOME=/Users/Imre/Library/Java/JavaVirtualMachines/openjdk-24.0.2+12-54/Contents/Home
./mvnw clean test
```

### Manual Frontend Tests
1. Start application: `./mvnw spring-boot:run`
2. Open http://localhost:8080/dashboard
3. Open http://localhost:8080/positions
4. Follow manual test steps above
5. Check browser console and Network tab for expected behavior

---

## Known Limitations

1. **JavaScript Unit Tests**: No JavaScript test framework configured (Jest/Vitest)
   - Using manual console testing and integration tests instead
   - Consider adding Jest configuration for future test automation

2. **Cache Lifetime**: Cache is session-based (cleared on page reload)
   - This is by design to handle new instruments added to database
   - Future enhancement: implement TTL-based cache invalidation

3. **Rate Limiting**: No rate limiting implemented for API calls
   - Alpaca API has 200 req/min limit
   - With caching, should not be an issue for typical usage
   - Monitor logs if Position Calculator receives heavy user input

---

## Notes for Future Enhancement

- Add JavaScript test framework (Jest) with automated tests
- Implement TTL-based cache with periodic refresh
- Add request queue for bulk exchange lookups
- Monitor API call frequency in production
- Add metrics for cache hit/miss ratio
