# ISSUE-014: Implement Alpaca Assets API Integration

**Created**: 2025-11-09 (Session Current)
**Status**: CLOSED
**Completed**: 2025-11-09 (Session Current)
**Priority**: HIGH
**Category**: Feature
**Blocking**: Position validation, short selling constraints, margin requirement calculations

---

## Problem

Currently, SrcProfit only fetches market data (quotes/prices) from Alpaca but lacks critical asset metadata needed for:
- **Pre-trade validation**: Checking if an asset is tradable before placement
- **Position constraints**: Verifying marginable/shortable flags
- **Margin calculations**: Using maintenance_margin_requirement for position sizing
- **Asset discovery**: Resolving symbols to Alpaca asset UUIDs

The `PositionController.getMarketData()` method retrieves prices via `/v2/stocks/snapshots` but does not fetch or validate asset tradability, marginability, or borrowability.

---

## Root Cause

The Alpaca Assets API endpoint (`GET /v2/assets/{symbol}`) is not integrated into the service layer:
1. No `AlpacaAssetDto` to parse the API response
2. `InstrumentEntity` lacks Alpaca-specific metadata fields
3. `AlpacaService` doesn't have a `getAsset()` method
4. `RestClientConfig` only provides data API client (base URL: `https://data.alpaca.markets`), not trading API client (base URL: `https://paper-api.alpaca.markets`)
5. No caching strategy to avoid redundant API calls

---

## Approach

### Phase 1: AlpacaService + Data Layer

1. **Extend InstrumentEntity** (`src/main/java/co/grtk/srcprofit/entity/InstrumentEntity.java`)
   - Add columns: `alpaca_asset_id` (UUID), `alpaca_tradable` (boolean), `alpaca_marginable` (boolean), `alpaca_shortable` (boolean), `alpaca_easy_to_borrow` (boolean), `alpaca_fractionable` (boolean), `alpaca_maintenance_margin_requirement` (BigDecimal), `alpaca_exchange` (String), `alpaca_status` (String), `alpaca_asset_class` (String)
   - Add fields with `@Column` annotations
   - Track when asset metadata was last updated

2. **Create AlpacaAssetDto** (`src/main/java/co/grtk/srcprofit/dto/AlpacaAssetDto.java`)
   - Map API response: `id`, `class`, `exchange`, `symbol`, `name`, `status`, `tradable`, `marginable`, `shortable`, `easy_to_borrow`, `fractionable`, `maintenance_margin_requirement`

3. **Add alpacaTradingRestClient bean** (`src/main/java/co/grtk/srcprofit/config/RestClientConfig.java`)
   - New `@Bean` with base URL: `https://paper-api.alpaca.markets` (or live based on environment)
   - Same authentication headers as data API client
   - Named: `alpacaTradingRestClient`

4. **Implement AlpacaService methods**:
   - `AlpacaService.getAsset(String symbol)`: Call `/v2/assets/{symbol}`, return `AlpacaAssetDto`
   - `AlpacaService.saveAssetMetadata(AlpacaAssetDto, InstrumentEntity)`: Populate InstrumentEntity with Alpaca fields

5. **Create database migration**
   - Add 10 new columns to `INSTRUMENT` table
   - Create index on `alpaca_asset_id` for lookups

### Phase 2: PositionController Integration

1. **Update PositionController.getMarketData()** (currently `getMarketValue()`)
   - Check `InstrumentRepository.findByTicker(ticker)` first (cache hit)
   - If asset not found in DB:
     - Call `AlpacaService.getAsset(ticker)`
     - Save metadata via `AlpacaService.saveAssetMetadata()`
     - Log: "Asset XXX not cached, fetched from Alpaca and saved"
   - If asset exists but metadata is stale (>24h):
     - Refresh via `AlpacaService.getAsset()` (optional, can be deferred)
   - Use `alpaca_tradable` flag to validate before returning market data

2. **Add validation logging**
   - WARN if `tradable=false`
   - WARN if `marginable=false` for margin trades
   - WARN if `shortable=false` for short positions

---

## Success Criteria

- [ ] `AlpacaAssetDto` created with all 11 response fields mapped
- [ ] `InstrumentEntity` extended with 10 new Alpaca-specific columns
- [ ] `alpacaTradingRestClient` bean added to `RestClientConfig` with correct base URL
- [ ] `AlpacaService.getAsset(String symbol)` implemented and tested
- [ ] `AlpacaService.saveAssetMetadata(AlpacaAssetDto, InstrumentEntity)` implemented
- [ ] Database migration created and applied successfully
- [ ] `PositionController.getMarketData()` updated with cache-first logic
- [ ] All existing tests pass
- [ ] New unit tests for `AlpacaService.getAsset()` with mocked REST responses
- [ ] Integration test for `PositionController.getMarketData()` cache behavior
- [ ] No duplicate API calls for same symbol within same request
- [ ] Rate limit compliance verified (200 requests/min, 10/sec)
- [ ] Crypto symbol handling tested (BTCUSD format)

---

## Acceptance Tests

```java
// Phase 1: AlpacaService Asset Retrieval
@Test
void testGetAsset_WithValidSymbol_ReturnsAlpacaAssetDto() {
    // Given: Mock REST response for AAPL asset
    // When: alpacaService.getAsset("AAPL")
    // Then: Returns AlpacaAssetDto with tradable=true, marginable=true, etc.
    assert assetDto.isTradable() == true;
    assert assetDto.getSymbol().equals("AAPL");
}

@Test
void testGetAsset_WithCryptoSymbol_HandlesFormatCorrectly() {
    // Given: Symbol "BTCUSD"
    // When: alpacaService.getAsset("BTCUSD")
    // Then: Successfully retrieves crypto asset (not BTC-USD)
}

@Test
void testSaveAssetMetadata_PersistsAllFields() {
    // Given: AlpacaAssetDto with all fields populated
    // When: alpacaService.saveAssetMetadata(dto, instrumentEntity)
    // Then: InstrumentEntity has all alpaca_* fields set
    assert instrument.getAlpacaTradable() == true;
    assert instrument.getAlpacaMaintenanceMarginRequirement() != null;
}

// Phase 2: PositionController Integration
@Test
void testGetMarketData_FirstCall_FetchesAndCachesAsset() {
    // Given: Asset not in database
    // When: positionController.getMarketData("AAPL")
    // Then: Calls alpacaService.getAsset() once and saves to DB
    verify(alpacaService, times(1)).getAsset("AAPL");
    assertNotNull(instrumentRepository.findByTicker("AAPL"));
}

@Test
void testGetMarketData_SecondCall_UsesCachedAsset() {
    // Given: Asset already in database
    // When: positionController.getMarketData("AAPL") called twice
    // Then: Calls alpacaService.getAsset() only once (cached)
    verify(alpacaService, times(1)).getAsset("AAPL");
}

@Test
void testGetMarketData_WithNonTradableAsset_LogsWarning() {
    // Given: Asset with tradable=false
    // When: positionController.getMarketData("HALT")
    // Then: Logs WARNING level message
}
```

---

## Related Issues

- Blocked by: ISSUE-013 (research completed)
- Related: ISSUE-011 (IBKR position sync), ISSUE-012 (option chain data)

---

## Notes

**API Endpoints**:
- Assets API: `GET /v2/assets/{symbol_or_asset_id}`
- Base URLs:
  - Paper trading: `https://paper-api.alpaca.markets`
  - Live trading: `https://api.alpaca.markets`
- Rate limits: 200 requests/minute, 10 requests/second
- Authentication: Headers `APCA-API-KEY-ID` and `APCA-API-SECRET-KEY`

**Caching Strategy**:
- Phase 2 implementation: Cache-first (check DB before API call)
- Optional future: TTL-based refresh (refresh if >24h old)

**Critical Considerations**:
- Crypto symbols use "BTCUSD" format, not "BTC-USD"
- Asset UUID and symbol are both searchable endpoints
- Maintenance margin requirement is decimal (0.2 = 20%)
- Easy-to-borrow flag indicates stock lending availability

**References**:
- ISSUE-013: Alpaca Assets API Research
- Alpaca API Docs: https://docs.alpaca.markets/api-references/assets-api/
- TastyTrade Learning: Pre-trade position validation
