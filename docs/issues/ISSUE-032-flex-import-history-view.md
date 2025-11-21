# ISSUE-032: Create FLEX Import History View with DataTable

**Created**: 2025-11-20
**Completed**: 2025-11-21
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Feature
**Blocking**: None

---

## Problem

There is currently no user interface to view the history of FLEX statement imports and their processing results. Users cannot see:
- Which FLEX reports have been imported
- Import success/failure status
- Record counts (successful, failed, skipped)
- When imports were last processed

The `flex_statement_response` table contains all this tracking data (added in ISSUE-031), but it's only accessible via database queries.

---

## Root Cause

No controller, DTO, or view has been created to display `FlexStatementResponseEntity` records to users. The infrastructure exists (entity, repository, service), but the presentation layer is missing.

---

## Approach

Create a new "Import History" menu following the existing pattern used in `NetAssetValueController` and `net_asset_values_jte.jte`:

1. **Controller Layer**: `FlexImportHistoryController`
   - Single GET endpoint at `/flexImportHistory`
   - Fetch all records via `FlexStatementResponseRepository.findAll()`
   - Map to DTOs for presentation

2. **DTO Layer**: `FlexImportHistoryDto`
   - Map entity fields: `referenceCode`, `reportType`, `status`, `updatedAt`
   - Map count fields: `csvRecordsCount`, `csvFailedRecordsCount`, `csvSkippedRecordsCount`, `dataFixRecordsCount`
   - Handle nullable values (default counts to 0 for display)
   - Format `updatedAt` as readable string

3. **View Layer**: `flex_import_history_jte.jte`
   - DataTables implementation with 8 columns
   - Default sort by `updatedAt` DESC (newest first)
   - Basic table only (no status badges, filters, or expandable rows)

---

## Success Criteria

- [x] `FlexImportHistoryController.java` created with `/flexImportHistory` endpoint
- [x] `FlexImportHistoryDto.java` created with all required fields
- [x] `flex_import_history_jte.jte` created with DataTable displaying:
  - `reference_code` - IBKR FLEX API unique identifier
  - `report_type` - TRADES or NAV
  - `status` - Success/Fail from FLEX API
  - `updated_at` - Last update timestamp
  - `csv_records_count` - Successful import count
  - `csv_failed_records_count` - Failed record count
  - `csv_skipped_records_count` - Skipped record count (non-OPT assets)
  - `data_fix_records_count` - Data fix cleanup count (TRADES only)
- [x] Table sorted by `updatedAt` descending (most recent first)
- [x] All existing tests pass (204 tests - 7 new tests for FlexImportHistoryController)
- [x] Page accessible and functional (user tested)

---

## Acceptance Tests

```java
@Test
void testFlexImportHistoryEndpoint() {
    // Given: FlexStatementResponse records exist
    FlexStatementResponseEntity entity1 = new FlexStatementResponseEntity();
    entity1.setReferenceCode("12345");
    entity1.setReportType("TRADES");
    entity1.setStatus("Success");
    entity1.setCsvRecordsCount(100);
    entity1.setCsvFailedRecordsCount(5);
    entity1.setCsvSkippedRecordsCount(2);
    entity1.setDataFixRecordsCount(3);
    flexStatementResponseRepository.save(entity1);

    // When: GET /flexImportHistory
    ResponseEntity<String> response = restTemplate.getForEntity(
        "/flexImportHistory",
        String.class
    );

    // Then: Page contains expected data
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("12345");
    assertThat(response.getBody()).contains("TRADES");
    assertThat(response.getBody()).contains("Success");
    assertThat(response.getBody()).contains("100"); // csv_records_count
    assertThat(response.getBody()).contains("5");   // failed
    assertThat(response.getBody()).contains("2");   // skipped
    assertThat(response.getBody()).contains("3");   // data_fix
}
```

---

## Related Issues

- Depends on: ISSUE-031 (FlexStatementResponse tracking fields - CLOSED)
- Related: ISSUE-030 (Resilient CSV import - CLOSED)
- Related: ISSUE-005 (FLEX import monitoring fields - CLOSED)

---

## Notes

### Design Decisions

**Scope: View Only**
- This issue focuses ONLY on creating the view and displaying existing data
- No navigation menu updates (can be added later if needed)
- No re-trigger failed imports functionality
- No error detail drill-down
- Basic DataTable only (no status badges or advanced filtering)

**Column Selection: All Count Fields**
- Display all 4 count fields as separate columns for transparency
- `csv_records_count` - successful imports
- `csv_failed_records_count` - failed during import (ISSUE-030)
- `csv_skipped_records_count` - skipped (non-OPT assets like STK)
- `data_fix_records_count` - TRADES only cleanup operation

**Nullable Handling**
- `csvFailedRecordsCount`, `csvSkippedRecordsCount`, `dataFixRecordsCount` can be NULL
- DTO should default NULL values to 0 for display
- `updatedAt` can be NULL for old records (added in ISSUE-031)

### Reference Implementation Patterns

**Controller Pattern**: See `NetAssetValueController.java`
```java
@GetMapping("/netAssetValues")
public String getNetAssetValues(Model model) {
    List<NetAssetValueDto> dtos = service.getAllAsDto();
    model.addAttribute("netAssetValues", dtos);
    return "net_asset_values_jte";
}
```

**JTE Template Pattern**: See `net_asset_values_jte.jte`
```html
<table id="datatable" class="table table-striped">
  <thead>
    <tr><th>Headers...</th></tr>
  </thead>
  <tbody>
    @for(var item : itemList)
      <tr><td>${item.getField()}</td></tr>
    @endfor
  </tbody>
</table>

<script>
const table = new DataTable('#datatable', {
  perPageSelect: [5, 10, 15, ["All", -1]],
  pageLength: -1,
  order: [[3, 'desc']] // column 3 descending
});
</script>
```

### Database Context

**Entity**: `FlexStatementResponseEntity.java` (`src/main/java/co/grtk/srcprofit/entity/`)
- Primary key: `id` (Long)
- Unique constraint: `referenceCode`
- Indexes: `referenceCode`, `requestDate`, `reportType`

**Repository**: `FlexStatementResponseRepository.java`
- Available methods: `findAll()`, `findByReferenceCode()`, `findByReportType()`

**Service**: `FlexReportsService.java`
- Populates entity during import workflow
- Updates count fields after CSV processing

### Future Enhancements (Out of Scope)

- Add status color badges (green=success, red=failures, yellow=partial)
- Add reportType filter dropdown (TRADES vs NAV)
- Add expandable rows showing detailed error messages
- Add "Re-import" button for failed records
- Add date range filtering on `requestDate` or `updatedAt`
- Add navigation menu item (if this becomes a primary feature)

### Testing Notes

- ✅ All 204 tests pass (7 new tests added for FlexImportHistoryController)
- ✅ New test coverage includes:
  - Controller annotation verification
  - Endpoint returns correct view name
  - Model attributes populated correctly
  - Empty list handling
  - Service method invocation
  - Endpoint path validation
- ✅ Manual testing completed:
  1. ✅ Application runs successfully
  2. ✅ Page accessible at `http://localhost:8080/flexImportHistory`
  3. ✅ All 8 columns display correctly
  4. ✅ DataTable sorting works (click column headers)
  5. ✅ DataTable pagination works
  6. ✅ NULL values handled correctly (old records without updated_at)
