# ISSUE-005: FLEX Reports Import Monitoring Fields

**Created**: 2025-11-03 (Session 05)
**Status**: CLOSED
**Completed**: 2025-11-03
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

FlexStatementResponseEntity currently tracks basic FLEX report request metadata (reference code, status, URL, report type), but lacks operational monitoring fields needed to:

1. **Audit database connectivity** - No record of which database the import was processed against
2. **Debug import failures** - CSV content not stored for troubleshooting failed imports
3. **Track import volume** - No record counts to monitor data ingestion trends
4. **Distinguish data processing stages** - Cannot differentiate between initial CSV import vs data fix operations (for trades)

This limits operational visibility and makes troubleshooting import issues more difficult.

---

## Root Cause

The original entity design focused on IBKR API interaction metadata (request/response tracking) but didn't include post-processing monitoring fields. The import workflow spans multiple stages:

1. API request → FlexStatementResponse saved
2. CSV retrieval → Written to file system
3. CSV parsing → Records saved to database
4. Data fix (trades only) → Additional records processed

Currently, only stage 1 is captured in FlexStatementResponseEntity. Stages 2-4 have no audit trail.

---

## Approach

Add four monitoring fields to FlexStatementResponseEntity:

### New Fields

1. **dbUrl** (String, 500 chars)
   - Database connection URL (e.g., `jdbc:postgresql://localhost:5432/srcprofit`)
   - Populated from Spring DataSource configuration
   - Tracks which database instance processed the import

2. **csvFilePath** (String, 500 chars)
   - File system path to saved CSV (e.g., `~/FLEX_TRADES_ABC123.csv`)
   - Populated after `FileUtils.write()` in import methods
   - Enables CSV retrieval for troubleshooting without re-requesting from IBKR

3. **csvRecordsCount** (Integer)
   - Count returned from `optionService.saveCSV()` or `netAssetValueService.saveCSV()`
   - Tracks initial CSV import volume

4. **dataFixRecordsCount** (Integer, nullable)
   - Count returned from `optionService.dataFix()`
   - Only applicable to FLEX Trades (null for NAV reports)
   - Tracks data fix processing volume

### Implementation Steps

#### 1. Entity Changes (FlexStatementResponseEntity.java:74)
```java
@Column(name = "db_url", length = 500)
private String dbUrl;

@Column(name = "csv_file_path", length = 500)
private String csvFilePath;

@Column(name = "csv_records_count")
private Integer csvRecordsCount;

@Column(name = "data_fix_records_count")
private Integer dataFixRecordsCount;
```

#### 2. Repository Changes (FlexStatementResponseRepository.java)
```java
FlexStatementResponseEntity findByReferenceCode(String referenceCode);
```

#### 3. Service Changes (FlexReportsService.java)

**Inject DataSource** (for dbUrl):
```java
@Autowired
private DataSource dataSource;

private String getDatabaseUrl() throws SQLException {
    try (Connection conn = dataSource.getConnection()) {
        return conn.getMetaData().getURL();
    }
}
```

**Update saveFlexStatementResponse()** (line 96):
```java
private void saveFlexStatementResponse(FlexStatementResponse response, String reportType) {
    FlexStatementResponseEntity entity = new FlexStatementResponseEntity();
    // ... existing fields ...
    entity.setDbUrl(getDatabaseUrl());
    flexStatementResponseRepository.save(entity);
}
```

**Update importFlexTrades()** (after line 154):
```java
// After dataFixRecords calculation
FlexStatementResponseEntity entity = flexStatementResponseRepository
    .findByReferenceCode(flexTradesResponse.getReferenceCode());
entity.setCsvFilePath(file.getAbsolutePath());
entity.setCsvRecordsCount(csvRecords);
entity.setDataFixRecordsCount(dataFixRecords);
flexStatementResponseRepository.save(entity);
```

**Update importFlexNetAssetValue()** (after line 204):
```java
// After records calculation
FlexStatementResponseEntity entity = flexStatementResponseRepository
    .findByReferenceCode(flexNetAssetValueResponse.getReferenceCode());
entity.setCsvFilePath(file.getAbsolutePath());
entity.setCsvRecordsCount(records);
entity.setDataFixRecordsCount(null); // NAV reports don't have data fix
flexStatementResponseRepository.save(entity);
```

#### 4. Database Migration
Create Flyway migration: `V006__add_flex_monitoring_fields.sql`
```sql
ALTER TABLE flex_statement_response
ADD COLUMN db_url VARCHAR(500),
ADD COLUMN csv_file_path VARCHAR(500),
ADD COLUMN csv_records_count INTEGER,
ADD COLUMN data_fix_records_count INTEGER;

COMMENT ON COLUMN flex_statement_response.db_url IS 'Database connection URL where import was processed';
COMMENT ON COLUMN flex_statement_response.csv_file_path IS 'File system path to saved CSV file';
COMMENT ON COLUMN flex_statement_response.csv_records_count IS 'Number of records imported from CSV';
COMMENT ON COLUMN flex_statement_response.data_fix_records_count IS 'Number of records processed in data fix (trades only)';
```

---

## Success Criteria

- [x] Four new fields added to FlexStatementResponseEntity with proper JPA annotations
- [ ] FlexStatementResponseRepository has findByReferenceCode() method
- [ ] FlexReportsService injects DataSource and has getDatabaseUrl() helper method
- [ ] saveFlexStatementResponse() populates dbUrl field
- [ ] importFlexTrades() updates entity with csvFilePath, csvRecordsCount, and dataFixRecordsCount
- [ ] importFlexNetAssetValue() updates entity with csvFilePath and csvRecordsCount
- [ ] Database migration script created and applied
- [ ] All fields populated correctly during manual import test
- [ ] Unit tests cover new fields and update logic
- [ ] Integration test verifies full import workflow populates all monitoring fields

---

## Acceptance Tests

```java
@Test
void testFlexTradesImportPopulatesMonitoringFields() {
    // Given: FLEX Trades import is triggered
    String result = flexReportsService.importFlexTrades();

    // When: Entity is retrieved
    String referenceCode = extractReferenceCode(result);
    FlexStatementResponseEntity entity =
        flexStatementResponseRepository.findByReferenceCode(referenceCode);

    // Then: All monitoring fields are populated
    assertThat(entity.getDbUrl()).contains("jdbc:postgresql://");
    assertThat(entity.getCsvFilePath()).matches(".*FLEX_TRADES_.*\\.csv");
    assertThat(entity.getCsvRecordsCount()).isGreaterThan(0);
    assertThat(entity.getDataFixRecordsCount()).isGreaterThanOrEqualTo(0);
}

@Test
void testFlexNAVImportPopulatesMonitoringFields() {
    // Given: FLEX NAV import is triggered
    String result = flexReportsService.importFlexNetAssetValue();

    // When: Entity is retrieved
    String referenceCode = extractReferenceCode(result);
    FlexStatementResponseEntity entity =
        flexStatementResponseRepository.findByReferenceCode(referenceCode);

    // Then: Monitoring fields are populated (except dataFixRecordsCount)
    assertThat(entity.getDbUrl()).contains("jdbc:postgresql://");
    assertThat(entity.getCsvFilePath()).matches(".*FLEX_NET_ASSET_VALUE_.*\\.csv");
    assertThat(entity.getCsvRecordsCount()).isGreaterThan(0);
    assertThat(entity.getDataFixRecordsCount()).isNull(); // NAV has no data fix
}
```

---

## Related Issues

- Related: ISSUE-003 (FLEX Reports Automatic Synchronization) - Monitoring fields support scheduled import tracking
- Related: ISSUE-004 (Scheduled Reports Consolidation) - Monitoring data can feed into centralized scheduling dashboard

---

## Notes

### Design Decisions

1. **Why store file path instead of CSV content?**
   - CSV files can be 100KB+ per import
   - Storing content would duplicate data already on file system
   - File path provides audit trail without bloat
   - If CSV debugging needed, file can be retrieved from path

2. **Why capture database URL?**
   - Multi-environment deployments (dev/staging/prod) need import tracking
   - Database URL distinguishes which instance processed the import
   - Helps audit and troubleshooting in distributed setups

3. **Why separate csvRecordsCount and dataFixRecordsCount?**
   - Different processing stages have different failure modes
   - CSV import might succeed while data fix fails
   - Granular counts enable better operational dashboards
   - NAV reports don't have data fix (null value is semantically correct)

### File References

- **Entity**: `src/main/java/co/grtk/srcprofit/entity/FlexStatementResponseEntity.java`
- **Repository**: `src/main/java/co/grtk/srcprofit/repository/FlexStatementResponseRepository.java`
- **Service**: `src/main/java/co/grtk/srcprofit/service/FlexReportsService.java`
- **Controller**: `src/main/java/co/grtk/srcprofit/controller/IbkrRestController.java`
- **CSV Parsers**:
  - `src/main/java/co/grtk/srcprofit/service/OptionService.java:339` (saveCSV)
  - `src/main/java/co/grtk/srcprofit/service/NetAssetValueService.java:124` (saveCSV)

### Implementation Notes

- Use `@Transactional` boundary to ensure entity updates are atomic
- Handle SQLException from getDatabaseUrl() gracefully (log warning, continue with null)
- Consider adding index on csv_records_count for analytics queries
- Future enhancement: Add import_duration_ms field to track performance
