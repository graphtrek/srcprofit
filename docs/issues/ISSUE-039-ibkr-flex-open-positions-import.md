# ISSUE-039: IBKR Flex Open Positions Import

**Created**: 2025-11-30
**Completed**: 2025-11-30
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

The IBKR_FLEX_OPEN_POSITIONS_ID environment variable is configured (env.properties line 15: 1317125) but has no corresponding import functionality. There is no way to download and import open positions data from IBKR Flex Reports, which is needed for:

1. **Position Reconciliation**: Validate calculated positions against IBKR's official snapshot
2. **Multi-Asset Portfolio View**: Track all open positions (options, stocks, cash) in one place
3. **Position Monitoring**: Historical snapshots of portfolio positions over time
4. **Risk Analysis**: Complete portfolio view for position sizing and risk management

Without this import, we rely solely on calculated positions from TRADES import, which can drift from actual broker positions due to corporate actions, adjustments, or data gaps.

---

## Root Cause

The IBKR_FLEX_OPEN_POSITIONS_ID configuration was added but the corresponding import workflow was never implemented. The existing TRADES and NAV imports provide the pattern, but no one extended it to handle Open Positions.

---

## Approach

Follow the exact pattern from `importFlexNetAssetValue()` (simple error handling, snapshot data) to implement IBKR Flex Open Positions import.

### 1. Entity Layer - OpenPositionEntity

**File**: `src/main/java/co/grtk/srcprofit/entity/OpenPositionEntity.java` (NEW)

Create entity with:
- Natural key: `conid` (unique per contract)
- Upsert behavior: Update existing position on re-import
- Multi-asset support: OPT, STK, CASH, etc.
- Core fields: account, reportDate, assetClass, symbol, quantity, markPrice, positionValue
- Options-specific: strike, expirationDate, putCall, underlyingSymbol (nullable for non-OPT)
- Financial: costBasisPrice, costBasisMoney, fifoPnlUnrealized
- Identifiers: conid, underlyingConid, currency, side

**Indexes**:
- UNIQUE on `conid` (upsert key)
- INDEX on `symbol`, `assetClass`, `reportDate`

### 2. Repository Layer - OpenPositionRepository

**File**: `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java` (NEW)

Standard JpaRepository with:
- `findByConid(Long conid)` - Upsert lookup
- `findByAssetClass(String assetClass)` - Filter by asset type
- `findBySymbol(String symbol)` - Ticker lookup
- `findByReportDate(LocalDate reportDate)` - Snapshot queries

### 3. Service Layer - OpenPositionService

**File**: `src/main/java/co/grtk/srcprofit/service/OpenPositionService.java` (NEW)

CSV parsing with simple error handling (following NAV pattern):
- `saveCSV(String csv)` - Returns `int` (record count)
- Upsert logic: Check `findByConid()`, update if exists, insert if new
- Parse all asset classes (OPT, STK, CASH) - no filtering
- Null-safe parsing for optional fields
- Transaction rollback on error (no detailed error tracking)

**Reference**: NetAssetValueService.saveCSV() (lines 123-164)

### 4. Orchestration - FlexReportsService

**File**: `src/main/java/co/grtk/srcprofit/service/FlexReportsService.java` (MODIFY)

Add:
- Constructor injection: `OpenPositionService`
- New method: `importFlexOpenPositions()`
- Workflow: SendRequest → wait 15s → GetStatement → save CSV → parse → update metadata
- CSV file: `~/FLEX_OPEN_POSITIONS_{referenceCode}.csv`
- Return format: `"{records}/0"` (like NAV)
- Report type: `"OPEN_POSITIONS"`

**Reference**: importFlexNetAssetValue() (lines 194-234) - EXACT pattern

### 5. Controller Layer - IbkrRestController

**File**: `src/main/java/co/grtk/srcprofit/controller/IbkrRestController.java` (MODIFY)

Add endpoint:
```java
@GetMapping(value = "/ibkrFlexOpenPositionsImport", produces = MediaType.APPLICATION_XML_VALUE)
public String ibkrFlexOpenPositionsImport() {
    return flexReportsService.importFlexOpenPositions();
}
```

### 6. Scheduling - ScheduledJobsService

**File**: `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java` (MODIFY)

Add scheduled job:
- Schedule: `@Scheduled(fixedDelay = 360, initialDelay = 1, timeUnit = TimeUnit.MINUTES)`
- Every 6 hours (matching TRADES and NAV)
- Delegates to: `flexReportsService.importFlexOpenPositions()`

---

## Success Criteria

### Database Schema
- [ ] OpenPositionEntity created with all required fields
- [ ] OpenPositionRepository created with upsert lookup method
- [ ] Table OPEN_POSITION created by Hibernate (auto-DDL)
- [ ] Indexes created correctly (conid unique, symbol, assetClass, reportDate)

### Business Logic
- [ ] OpenPositionService.saveCSV() parses CSV correctly
- [ ] Upsert logic works (insert new, update existing by conid)
- [ ] All asset classes imported (OPT, STK, CASH verified)
- [ ] Null handling for optional fields (strike, expiration, etc.)

### Integration
- [ ] FlexReportsService.importFlexOpenPositions() orchestrates full workflow
- [ ] CSV file saved to ~/FLEX_OPEN_POSITIONS_{referenceCode}.csv
- [ ] FlexStatementResponseEntity metadata persisted (reportType: "OPEN_POSITIONS")
- [ ] Metadata fields populated: csvRecordsCount, csvFilePath, updatedAt

### REST API
- [ ] Endpoint /ibkrFlexOpenPositionsImport responds successfully
- [ ] Manual trigger downloads and imports data
- [ ] Response format matches NAV: "{records}/0"

### Scheduling
- [ ] Scheduled job runs every 6 hours
- [ ] Scheduled job logs start/completion/errors
- [ ] No conflicts with existing TRADES/NAV schedules

### Testing
- [ ] Unit tests: CSV parsing, upsert logic, multi-asset support
- [ ] Integration tests: End-to-end flow with mock IBKR API
- [ ] Manual testing with real IBKR API successful
- [ ] Data accuracy validated against IBKR portal

### Documentation
- [ ] JavaDoc added to all public methods
- [ ] Implementation plan followed from binary-tumbling-tiger.md

---

## Acceptance Tests

### Test 1: CSV Parsing Success
```java
@Test
void testOpenPositionsCsvParsing() {
    String csv = """
        ClientAccountID,Conid,AssetClass,Symbol,Quantity,Report Date,Currency,Mark Price
        U1234567,12345,OPT,SPY,10,2025-11-30,USD,4.50
        U1234567,67890,STK,AAPL,100,2025-11-30,USD,180.25
        """;
    int records = openPositionService.saveCSV(csv);
    assertEquals(2, records);

    OpenPositionEntity opt = openPositionRepository.findByConid(12345L);
    assertEquals("SPY", opt.getSymbol());
    assertEquals("OPT", opt.getAssetClass());
    assertEquals(10, opt.getQuantity());
}
```

### Test 2: Upsert Logic
```java
@Test
void testOpenPositionsUpsert() {
    // First import
    String csv1 = "ClientAccountID,Conid,Symbol,Quantity,Report Date\nU1234567,12345,SPY,10,2025-11-30\n";
    openPositionService.saveCSV(csv1);

    OpenPositionEntity first = openPositionRepository.findByConid(12345L);
    assertEquals(10, first.getQuantity());
    Instant firstCreated = first.getCreatedAt();

    // Second import (update existing)
    String csv2 = "ClientAccountID,Conid,Symbol,Quantity,Report Date\nU1234567,12345,SPY,15,2025-12-01\n";
    openPositionService.saveCSV(csv2);

    OpenPositionEntity updated = openPositionRepository.findByConid(12345L);
    assertEquals(15, updated.getQuantity());
    assertEquals(firstCreated, updated.getCreatedAt()); // Created unchanged
    assertTrue(updated.getUpdatedAt().isAfter(first.getUpdatedAt())); // Updated changed
    assertEquals(1, openPositionRepository.count()); // Still only 1 record
}
```

### Test 3: End-to-End Flow
```java
@Test
void testFlexOpenPositionsImportEndToEnd() {
    // Mock IBKR API
    when(ibkrService.getFlexWebServiceSendRequest(anyString()))
        .thenReturn(new FlexStatementResponse("ABC123", "2025-11-30 10:00:00", "Success", "https://..."));
    when(ibkrService.getFlexWebServiceGetStatement(anyString(), anyString()))
        .thenReturn("ClientAccountID,Conid,Symbol,Quantity,Report Date\nU1234567,12345,SPY,10,2025-11-30\n");

    String result = flexReportsService.importFlexOpenPositions();

    assertEquals("1/0", result);
    assertEquals(1, openPositionRepository.count());

    FlexStatementResponseEntity metadata = flexStatementResponseRepository.findByReferenceCode("ABC123");
    assertEquals("OPEN_POSITIONS", metadata.getReportType());
    assertEquals(1, metadata.getCsvRecordsCount());
    assertTrue(metadata.getCsvFilePath().contains("FLEX_OPEN_POSITIONS_ABC123.csv"));
}
```

---

## Related Issues

- Pattern: ISSUE-003 (Flex Reports Automatic Synchronization) - Uses same infrastructure
- Pattern: ISSUE-005 (Flex Import Monitoring Fields) - Uses same metadata tracking
- Related: ISSUE-033 (P&L Calculation) - Open Positions can validate calculated positions

---

## Notes

### Design Decisions

1. **Simple Error Handling (like NAV)**: Return `int` count, no `CsvImportResult`. Snapshots are simpler than transactional TRADES data.

2. **Upsert on Conid Only**: Natural key is `conid` alone (not conid + reportDate). Latest snapshot wins. Alternative: Add reportDate to unique index if full history needed.

3. **No Data Fix**: Snapshots don't need orphan cleanup (unlike TRADES which removes unpaired OPEN/CLOSED records).

4. **Multi-Asset Support**: Import all asset classes (OPT, STK, CASH) without filtering. Query-time filtering available via `findByAssetClass()`.

### IBKR Flex Report Structure (Assumptions)

**Required CSV Columns**:
- ClientAccountID, Conid, AssetClass, Symbol, Report Date, Quantity, Currency

**Optional CSV Columns**:
- Mark Price, Position Value, Cost Basis Price, Cost Basis Money, FIFO PNL Unrealized, Side
- Options: Strike, Expiry, Put/Call, Underlying Symbol, Underlying Conid
- Identifiers: CUSIP, ISIN, Security ID

**Data Formats**:
- Dates: ISO format (YYYY-MM-DD)
- Numbers: Standard decimal (123.45)
- Encoding: UTF-8

### Implementation Reference

Complete implementation plan: `/Users/Imre/.claude/plans/binary-tumbling-tiger.md`

### Files to Create/Modify

**NEW**:
- src/main/java/co/grtk/srcprofit/entity/OpenPositionEntity.java
- src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java
- src/main/java/co/grtk/srcprofit/service/OpenPositionService.java
- src/test/java/co/grtk/srcprofit/service/OpenPositionServiceTest.java

**MODIFY**:
- src/main/java/co/grtk/srcprofit/service/FlexReportsService.java
- src/main/java/co/grtk/srcprofit/controller/IbkrRestController.java
- src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java
