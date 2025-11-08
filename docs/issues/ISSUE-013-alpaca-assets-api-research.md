# ISSUE-013: Alpaca Assets API Research & Documentation

**Created**: 2025-11-08 (Session TBD)
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Documentation
**Blocking**: Future Alpaca asset validation integration work

---

## Problem

SrcProfit needs to validate assets before placing trades and determine asset tradability constraints (margin, shorting, fractional). The Alpaca `GET /v2/assets/{symbol_or_asset_id}` endpoint provides this capability, but the API details needed to be researched and documented for implementation reference.

---

## Root Cause

No comprehensive internal documentation exists for the Alpaca Assets API endpoint. This research fills that gap.

---

## Approach

Research the official Alpaca API documentation at https://docs.alpaca.markets/reference/get-v2-assets-symbol_or_asset_id and compile comprehensive reference material including:
- Complete endpoint structure and authentication
- Request/response formats with examples
- All field descriptions and data types
- Use cases and best practices
- Rate limits and error handling
- Crypto symbol formatting requirements

---

## Success Criteria

- [x] Complete API endpoint structure documented
- [x] All request parameters and response fields documented
- [x] Example requests and responses provided
- [x] Authentication and rate limits documented
- [x] Use cases and best practices identified
- [x] Important notes and limitations documented
- [x] Integration considerations for SrcProfit identified

---

## API Endpoint Reference

### Endpoint Details

**HTTP Method:** GET

**Base URLs:**
- Paper Trading: `https://paper-api.alpaca.markets/v2/assets/{symbol_or_asset_id}`
- Live Trading: `https://api.alpaca.markets/v2/assets/{symbol_or_asset_id}`

**Path Parameters:**
- `symbol_or_asset_id` (required, string): Ticker symbol (e.g., "AAPL", "BTCUSD") or UUID asset ID

### Authentication

**Type:** API Key via HTTP headers
- `APCA-API-KEY-ID`: Your API key ID
- `APCA-API-SECRET-KEY`: Your API secret key

**Rate Limits:**
- 200 requests per minute per API key
- 10 requests per second (burst limit)
- HTTP 429 (Too Many Requests) when exceeded

### Request Examples

```http
# By ticker symbol
GET https://paper-api.alpaca.markets/v2/assets/AAPL
APCA-API-KEY-ID: {YOUR_API_KEY_ID}
APCA-API-SECRET-KEY: {YOUR_API_SECRET_KEY}

# By asset UUID
GET https://paper-api.alpaca.markets/v2/assets/904837e3-3b76-47ec-b432-046db621571b
APCA-API-KEY-ID: {YOUR_API_KEY_ID}
APCA-API-SECRET-KEY: {YOUR_API_SECRET_KEY}

# Crypto (old symbology format)
GET https://paper-api.alpaca.markets/v2/assets/BTCUSD
APCA-API-KEY-ID: {YOUR_API_KEY_ID}
APCA-API-SECRET-KEY: {YOUR_API_SECRET_KEY}
```

### Response Structure

**Success Response (200 OK):**

```json
{
  "id": "904837e3-3b76-47ec-b432-046db621571b",
  "class": "us_equity",
  "exchange": "NASDAQ",
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "status": "active",
  "tradable": true,
  "marginable": true,
  "shortable": true,
  "easy_to_borrow": true,
  "fractionable": true,
  "maintenance_margin_requirement": 30
}
```

**Error Response (404 Not Found):**

```json
{
  "code": 40410000,
  "message": "asset not found"
}
```

### Response Fields

| Field | Type | Description | Notes |
|-------|------|-------------|-------|
| `id` | string (UUID) | Unique asset identifier | Use as primary key in database (symbols can change) |
| `class` | string | Asset class: "us_equity" or "crypto" | Determines which fields are available |
| `exchange` | string | Trading venue | AMEX, ARCA, BATS, NYSE, NASDAQ, NYSEARCA, OTC |
| `symbol` | string | Asset ticker symbol | Can change; use UUID as primary key |
| `name` | string | Official asset name | Display-friendly identifier |
| `status` | string | "active" or "inactive" | Check before trading |
| `tradable` | boolean | Is asset tradable via Alpaca? | **Must be true** before placing orders |
| `marginable` | boolean | Eligible for margin trading | Check for margin orders |
| `shortable` | boolean | Eligible for short selling | Check for short orders |
| `easy_to_borrow` | boolean | Current short availability | **Best indicator** for short availability; changes intraday |
| `fractionable` | boolean | Supports fractional shares | Check for fractional orders |
| `min_order_size` | string | Minimum order quantity | Crypto only; validate before crypto orders |
| `min_trade_increment` | string | Trade quantity increment | Crypto only |
| `price_increment` | string | Price increment | Crypto only |
| `maintenance_margin_requirement` | integer | Margin requirement % | Equities only; use for position sizing |

---

## Use Cases for SrcProfit

### Primary Integration Points

1. **Pre-Trade Validation**
   - Verify asset exists before order placement
   - Check `tradable=true` before attempting any trade
   - Validate symbol format (especially for crypto)

2. **Trading Constraint Checking**
   - Check `marginable` for margin order eligibility
   - Check `shortable` and `easy_to_borrow` for short selling
   - Check `fractionable` for fractional order support
   - Retrieve `maintenance_margin_requirement` for position sizing

3. **Asset Discovery**
   - Resolve ticker symbols to asset UUIDs
   - Display asset names and exchanges in UI
   - Show tradability status to users

4. **Short Selling Strategy**
   - `easy_to_borrow=true` is best indicator for current availability
   - Short availability changes throughout trading day
   - Refresh before attempting short orders

5. **Crypto Asset Handling**
   - Convert between symbol formats (validate old symbology: "BTCUSD" not "BTC-USD")
   - Retrieve min_order_size and min_trade_increment for crypto orders
   - Handle URL-encoded coin pairs (e.g., "BTC%2FUSDT")

---

## Best Practices

### Caching Strategy

- **Cache indefinitely**: Symbol, name, exchange, class, id, marginable, shortable, fractionable, maintenance_margin_requirement
- **Refresh regularly**: tradable, status, easy_to_borrow (can change throughout day)
- **Consider**: Store asset UUID in database as primary key, not symbol

### Error Handling

- Implement retry logic for 429 (rate limit) responses
- Validate symbol format before API calls (especially crypto)
- Handle 404 responses for invalid symbols gracefully
- Check for null/missing fields (some are asset-class specific)

### Performance

- Use list endpoint (`GET /v2/assets`) for bulk lookups
- Batch asset validations to reduce API calls
- Cache static metadata to respect rate limits
- Validate assets before order submission, not after

### Data Integrity

- Use asset UUID as primary key in SrcProfit database
- Periodically refresh cached metadata to catch status changes
- Cross-reference with Market Data API for price information
- Track asset_id with positions for future lookups

---

## Critical Implementation Notes

### Crypto Symbol Formatting (⚠️ CRITICAL)

- **MUST use old symbology**: "BTCUSD" not "BTC-USD"
- **Coin pairs require URL encoding**: "BTC%2FUSDT" for BTC/USDT
- Failure to follow format results in 404 errors

### Asset Status Validation

- `tradable=false` means asset cannot be traded via Alpaca
- Always validate before order placement
- An asset can exist but not be tradable

### Short Selling Considerations

- `easy_to_borrow=true` is best current availability indicator
- Short availability can change dynamically intraday
- Refresh before attempting short orders
- Different from `shortable` field (which is static permission)

### Field Availability by Asset Class

**Crypto-specific fields:**
- min_order_size
- min_trade_increment
- price_increment

**Equities-specific fields:**
- maintenance_margin_requirement

Always check `class` field to determine available fields.

---

## Acceptance Tests

```python
def test_alpaca_assets_api_documentation():
    """Verify API research is complete and documented"""
    # Endpoint structure documented
    assert "GET /v2/assets/{symbol_or_asset_id}" in api_docs

    # Response fields documented
    assert "tradable" in response_fields
    assert "easy_to_borrow" in response_fields
    assert "marginable" in response_fields

    # Use cases identified
    assert "Pre-Trade Validation" in use_cases
    assert "Crypto Symbol Formatting" in critical_notes

    # Rate limits documented
    assert "200 requests per minute" in rate_limits
    assert "429 Too Many Requests" in error_handling

def test_srcprofit_integration_strategy():
    """Verify integration approach for SrcProfit"""
    # Caching strategy identified
    assert "cache indefinitely" in strategy
    assert "refresh regularly" in strategy

    # Best practices documented
    assert "Use UUID as primary key" in recommendations
    assert "Check tradable=true before orders" in requirements
```

---

## Related Issues

- Related: ISSUE-009 (TradingView Chart Integration) - may need asset data
- Related: ISSUE-010 (Dashboard Phase 1A) - may display asset information
- Related: ISSUE-011 (Position Calculator) - may use margin requirements

---

## Notes

**Research Source**: https://docs.alpaca.markets/reference/get-v2-assets-symbol_or_asset_id

**Key Reference Documents**:
- Alpaca API Reference: Assets endpoint
- TastyTrade Methodology: Position sizing and margin management
- SrcProfit Architecture: Market data integration patterns

**Implementation Ready**: This ticket serves as complete reference material for future Alpaca asset validation integration work.

**Session Completed**: 2025-11-08
