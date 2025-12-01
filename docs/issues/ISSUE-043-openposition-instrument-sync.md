# ISSUE-043: OpenPositionService saveCSV Should Synchronize Instruments

**Created**: 2025-12-01 (Session TBD)
**Completed**: 2025-12-01
**Status**: CLOSED
**Priority**: HIGH
**Category**: Feature / Data Quality
**Blocking**: None

---

## Problem

`OpenPositionService.saveCSV()` currently only creates/updates `OpenPositionEntity` records when importing IBKR Flex Report CSV data. It does NOT synchronize the corresponding `InstrumentEntity` records, leading to:

1. **Missing instrument records**: Positions are imported but instruments don't exist in the database
2. **Stale instrument metadata**: Instrument name/description from IBKR is more authoritative than external APIs
3. **Broken JPA relationships**: `OpenPositionEntity.instrument` and `OpenPositionEntity.underlyingInstrument` may reference non-existent instruments
4. **Failed snapshot refreshes**: `OptionSnapshotService` fails fast when `underlyingInstrument` is null (ISSUE-042)

### Current Behavior

```java
// OpenPositionService.saveCSV() (lines 110-116)
OpenPositionEntity entity = openPositionRepository.findByConid(conid);
if (entity == null) {
    entity = new OpenPositionEntity();
    entity.setConid(conid);
}
// Updates OpenPositionEntity fields but NEVER touches InstrumentEntity
```

### Expected Behavior

The CSV import should:
1. Check if `InstrumentEntity` exists by `conid`
2. If missing: create new instrument with CSV data
3. If exists: update instrument name/description from CSV (IBKR is ground truth)
4. Link the position to the instrument via JPA relationships

---

## Root Cause

The service was designed to handle only position snapshots, not instrument master data. However:

- **IBKR Flex Report is ground truth** for instrument identification
- **OpenPositionEntity has the data we need**: `conid`, `symbol`, `description`
- **JPA relationships require valid instruments**: Without instruments in DB, relationships are broken

The mapping is:
- `OpenPositionEntity.conid` → `InstrumentEntity.conid` (unique identifier)
- `OpenPositionEntity.symbol` → `InstrumentEntity.ticker` (symbol/ticker)
- `OpenPositionEntity.description` → `InstrumentEntity.name` (full description)

---

## Approach

Add instrument synchronization logic to `OpenPositionService.saveCSV()`:

**Key Logic**:
- **For STOCKS (STK)**: Use `conid` and `symbol` to identify and sync the instrument
- **For OPTIONS (OPT)**: Use `underlyingConid` and `underlyingSymbol` to sync the underlying stock (not the option contract itself)
- **For other asset classes**: Skip instrument sync (CASH, BOND, FOP, etc.)

**Lookup Strategy**:
1. Check by conid (primary key) - handles normal case
2. If not found, check by ticker (unique constraint) - handles conid changes
3. If neither exists, create new instrument
4. If ticker exists but conid differs, update conid (IBKR ground truth)

```java
// Determine which conid/symbol to use for instrument sync
Long instrumentConid = "OPT".equals(assetClass)
        ? parseLongOrNull(csvRecord, "UnderlyingConid")  // Options: underlying
        : conid;                                           // Stocks: own conid
String underlyingSymbol = "OPT".equals(assetClass)
        ? getStringOrNull(csvRecord, "UnderlyingSymbol")  // Options: underlying symbol
        : symbol;                                          // Stocks: own symbol

// Only sync instruments for STK and OPT
if (instrumentConid != null && underlyingSymbol != null) {
    InstrumentEntity instrument = instrumentRepository.findByConid(instrumentConid);
    if (instrument == null) {
        instrument = instrumentRepository.findByTicker(underlyingSymbol);
        if (instrument == null) {
            // Create new
            instrument = new InstrumentEntity();
            instrument.setConid(instrumentConid);
            instrument.setTicker(underlyingSymbol);
        } else {
            // Update conid
            instrument.setConid(instrumentConid);
        }
    }
    // Always update name from CSV
    instrument.setName(description != null ? description : underlyingSymbol);
    instrumentRepository.save(instrument);
}
```

### Key Design Decisions

1. **Asset class awareness**: Different lookup logic for stocks vs options
2. **Options use underlying**: Options sync the underlying stock, not the option contract
3. **Upsert behavior**: Create if new, update if exists (handles conid changes)
4. **IBKR is ground truth**: Always update name/description from CSV
5. **Required fields only**: conid, ticker, name (don't touch price, Alpaca metadata, etc.)
6. **Transaction boundary**: Reuse existing `@Transactional` - both instrument and position save/rollback together
7. **Graceful degradation**: Skip non-tradable asset classes (CASH, BOND, FOP, etc.)

### Edge Cases

| Case | Behavior |
|------|----------|
| Instrument exists, name changed | Update name from CSV |
| Instrument exists, ticker changed | Update ticker from CSV |
| Position for non-existent instrument | Create instrument first, then position |
| CSV has no description | Use symbol as name fallback |
| Duplicate conid in CSV | Last occurrence wins (existing upsert behavior) |
| Instrument has existing price data | Preserve price, only update identification fields |

---

## Success Criteria

- [x] Plan created with code examples
- [x] `InstrumentRepository` dependency added to `OpenPositionService`
- [x] Instrument upsert logic added before position upsert
- [x] Instrument fields mapped correctly (conid→conid, symbol→ticker, description→name)
- [x] Empty description falls back to symbol
- [x] Existing transaction/error handling preserved
- [x] All existing `OpenPositionService` tests pass
- [x] New test: CSV import creates missing instruments
- [x] New test: CSV import updates existing instrument names
- [x] New test: Empty description uses symbol fallback
- [x] Manual validation: Import real IBKR CSV, verify instruments created

---

## Acceptance Tests

### Test 1: CSV Import Creates Missing Instruments

```java
@Test
void testSaveCSV_createsInstrument_whenNotExists() {
    // Given: CSV with position data
    String csv = """
        ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
        U12345,265598,STK,AAPL,APPLE INC,100,2025-12-01,USD
        """;

    // When: Import CSV
    openPositionService.saveCSV(csv);

    // Then: Instrument created
    InstrumentEntity instrument = instrumentRepository.findByConid(265598L);
    assertNotNull(instrument);
    assertEquals("AAPL", instrument.getTicker());
    assertEquals("APPLE INC", instrument.getName());
}
```

### Test 2: CSV Import Updates Existing Instrument Name

```java
@Test
void testSaveCSV_updatesInstrument_whenExists() {
    // Given: Instrument exists with old name
    InstrumentEntity existing = new InstrumentEntity();
    existing.setConid(265598L);
    existing.setTicker("AAPL");
    existing.setName("Old Name");
    existing.setPrice(150.0);
    instrumentRepository.save(existing);

    // When: Import CSV with updated description
    String csv = """
        ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
        U12345,265598,STK,AAPL,APPLE INC - UPDATED,100,2025-12-01,USD
        """;
    openPositionService.saveCSV(csv);

    // Then: Name updated, price preserved
    InstrumentEntity updated = instrumentRepository.findByConid(265598L);
    assertEquals("APPLE INC - UPDATED", updated.getName());
    assertEquals(150.0, updated.getPrice()); // Price not touched
}
```

### Test 3: Empty Description Falls Back to Symbol

```java
@Test
void testSaveCSV_usesSymbolAsFallback_whenDescriptionEmpty() {
    // Given: CSV with empty description
    String csv = """
        ClientAccountID,Conid,AssetClass,Symbol,Description,Quantity,ReportDate,CurrencyPrimary
        U12345,265598,STK,AAPL,,100,2025-12-01,USD
        """;

    // When: Import CSV
    openPositionService.saveCSV(csv);

    // Then: Instrument uses symbol as name
    InstrumentEntity instrument = instrumentRepository.findByConid(265598L);
    assertEquals("AAPL", instrument.getName());
}
```

---

## Implementation Plan

### Phase 1: Add Dependency (5 min)

**File**: `OpenPositionService.java`

```java
@Service
public class OpenPositionService {
    private static final Logger log = LoggerFactory.getLogger(OpenPositionService.class);

    private final OpenPositionRepository openPositionRepository;
    private final InstrumentRepository instrumentRepository; // NEW

    public OpenPositionService(
            OpenPositionRepository openPositionRepository,
            InstrumentRepository instrumentRepository) { // NEW
        this.openPositionRepository = openPositionRepository;
        this.instrumentRepository = instrumentRepository; // NEW
    }
```

### Phase 2: Add Instrument Upsert Logic (15 min)

**File**: `OpenPositionService.java` - Insert after line 108 (after parsing CSV fields)

```java
// Line 109 - NEW: Upsert instrument
InstrumentEntity instrument = instrumentRepository.findByConid(conid);
if (instrument == null) {
    log.debug("Creating new instrument for conid={}, symbol={}", conid, symbol);
    instrument = new InstrumentEntity();
    instrument.setConid(conid);
    instrument.setTicker(symbol);
} else {
    log.debug("Updating existing instrument for conid={}", conid);
}

// Update name from CSV (IBKR is ground truth)
// Use symbol as fallback if description is empty
String name = (description != null && !description.trim().isEmpty())
    ? description.trim()
    : symbol;
instrument.setName(name);

// Note: Don't touch other fields (price, Alpaca metadata, etc.)
instrumentRepository.save(instrument);
log.debug("Saved instrument: conid={}, ticker={}, name={}",
    instrument.getConid(), instrument.getTicker(), instrument.getName());

// EXISTING: Line 110 - Upsert position
OpenPositionEntity entity = openPositionRepository.findByConid(conid);
// ... rest of existing logic
```

### Phase 3: Update JavaDoc (5 min)

**File**: `OpenPositionService.java` - Update class and method JavaDoc

Add to class JavaDoc (line 20-43):
```java
 * Instrument Synchronization:
 * - For each position, upserts corresponding InstrumentEntity
 * - Maps: conid→conid, symbol→ticker, description→name
 * - IBKR Flex Report is ground truth for instrument identification
 * - Preserves existing price/metadata (only updates identification fields)
```

Add to method JavaDoc (line 54-86):
```java
 * Instrument Upsert:
 * - Before position upsert, ensures InstrumentEntity exists
 * - Creates if missing, updates name/ticker if exists
 * - Falls back to symbol if description is empty
 * - Natural key: conid (same as position)
```

### Phase 4: Write Tests (30 min)

**File**: `OpenPositionServiceTest.java`

Add three new test methods (see Acceptance Tests above):
1. `testSaveCSV_createsInstrument_whenNotExists()`
2. `testSaveCSV_updatesInstrument_whenExists()`
3. `testSaveCSV_usesSymbolAsFallback_whenDescriptionEmpty()`

### Phase 5: Run Tests and Validate (10 min)

```bash
./mvnw test -Dtest=OpenPositionServiceTest
```

Expected:
- All existing tests pass (no regression)
- 3 new tests pass

### Phase 6: Manual Validation (10 min)

1. Import real IBKR Flex Report CSV
2. Query database: `SELECT * FROM instrument WHERE conid IN (SELECT DISTINCT conid FROM open_position)`
3. Verify instruments exist for all positions
4. Verify names match IBKR descriptions

---

## Related Issues

- **ISSUE-041**: Establish JPA relationships between OpenPositionEntity and InstrumentEntity (prerequisite - COMPLETED)
- **ISSUE-042**: Refactor OptionSnapshotService to use OpenPositionEntity (prerequisite - COMPLETED)
- **ISSUE-039**: IBKR Flex Open Positions Import (parent feature - COMPLETED)

---

## Notes

### Why This Matters

1. **Data Quality**: Instruments are the master data - positions reference them
2. **IBKR Ground Truth**: Flex Report has authoritative instrument identification
3. **JPA Relationships**: ISSUE-042 requires valid instruments for snapshot refresh
4. **User Experience**: Missing instruments cause confusing errors in UI

### Alternative Approaches Considered

1. **Separate instrument import**: Too complex, requires two CSV imports
2. **Lazy instrument creation**: Causes race conditions, unpredictable failures
3. **Skip instrument sync**: Breaks JPA relationships, fails snapshot refresh

### Performance Impact

- **Minimal**: One additional DB query + save per CSV row (already in transaction)
- **Batching not needed**: OpenPositionService already processes one row at a time
- **Transaction overhead**: None (reuses existing @Transactional boundary)

### Migration Strategy

- **No schema changes**: Uses existing InstrumentEntity fields
- **No data migration**: Historical positions will backfill instruments on next import
- **Backward compatible**: Existing code continues to work

### Implementation Notes

**Instrument Lookup Strategy**:
1. First check by `conid` (primary key) - handles normal case
2. If not found, check by `ticker` (unique constraint) - handles conid changes
3. If neither exists, create new instrument
4. If ticker exists but conid differs, update conid (IBKR is ground truth)

**Why This Approach**:
- Prevents duplicate ticker violation errors when CSV has same ticker, different conid
- IBKR conid is the authoritative identifier for instruments
- Preserves existing instrument data (price, Alpaca metadata) during updates
- Handles edge case where conid mapping changes in IBKR database

**Testing Coverage**:
- 5 comprehensive unit tests covering all scenarios
- 219 total tests pass (no regressions)
- Mockito-based testing for clean isolation
- Edge case coverage: missing instruments, conid changes, empty descriptions, options with underlying

---

## Owner

Claude Code

## Completed Date

TBD
