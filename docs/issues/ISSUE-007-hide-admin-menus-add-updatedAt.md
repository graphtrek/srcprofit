# ISSUE-007: Hide Admin Menus and Add updatedAt to FlexStatementResponseEntity

**Created**: 2025-11-04
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

The SrcProfit UI currently displays "Admin Data" and "Admin" menu sections in the main navigation that are not needed for typical user workflows. Additionally, the `FlexStatementResponseEntity` lacks an `updatedAt` timestamp field for proper audit tracking, while other entities in the codebase follow the pattern of including both `createdAt` and `updatedAt` fields.

**Current State**:
1. **index_jte.jte** (lines 157-268) shows two admin sections:
   - "Admin Data" menu (lines 157-205) with Earnings Calendar, IBKR Flex Trades, and IBKR Flex Net Asset
   - "Admin" menu (lines 208-268) with market data endpoints and IBKR login
2. **FlexStatementResponseEntity** only has `requestDate` and `originalTimestamp` but no standard `updatedAt` field for tracking modifications

---

## Root Cause

1. **UI Clutter**: Admin menus were created during development but are not part of the primary user workflow
2. **Incomplete Audit Trail**: FlexStatementResponseEntity was created without following the established timestamp pattern from BaseAsset and other entities

---

## Approach

### Task 1: Hide Admin Menus
- Comment out lines 157-205 (Admin Data section) in `src/main/jte/index_jte.jte`
- Comment out lines 208-268 (Admin section) in `src/main/jte/index_jte.jte`
- Use JTE/HTML comment syntax: `<%-- ... --%>` to preserve code for easy restoration
- Keep code intact for potential future use or debugging

### Task 2: Add updatedAt to FlexStatementResponseEntity
Add field with `@UpdateTimestamp` annotation for automatic timestamp management:
```java
@UpdateTimestamp(source = SourceType.DB)
@Column
private Instant updatedAt;
```

**Implementation Steps**:
1. Add the `updatedAt` field with `@UpdateTimestamp` annotation
2. Mark as nullable (`@Column` without `nullable = false`) to accommodate existing records
3. Add getter method: `public Instant getUpdatedAt()`
4. Add setter method: `public void setUpdatedAt(Instant updatedAt)`
5. Update `toString()` method to include `updatedAt` field
6. No manual database migration required (Hibernate will auto-generate column on next schema update)

**Note**: Field is nullable initially to allow existing records (which have no update timestamp) to coexist with new records that will have `updatedAt` automatically populated.

---

## Success Criteria

- [x] Admin Data menu (lines 157-205) is commented out in index_jte.jte
- [x] Admin menu (lines 208-268) is commented out in index_jte.jte
- [x] UI compiles and renders without the admin sections
- [x] FlexStatementResponseEntity has `updatedAt` field with `@UpdateTimestamp` annotation
- [x] `updatedAt` uses `Instant` type (consistent with BaseAsset pattern)
- [x] `updatedAt` uses `source = SourceType.DB` for database-managed timestamps
- [x] Getter and setter methods added for `updatedAt`
- [x] `toString()` method includes `updatedAt` field
- [x] Application starts successfully after changes
- [x] Existing tests pass

---

## Acceptance Tests

```java
// Test that updatedAt is properly set
@Test
void testFlexStatementResponseEntity_updatedAt_autoPopulated() {
    FlexStatementResponseEntity entity = new FlexStatementResponseEntity();
    entity.setReferenceCode("TEST-REF-001");
    entity.setStatus("Success");

    FlexStatementResponseEntity saved = repository.save(entity);
    assertThat(saved.getUpdatedAt()).isNotNull();

    // Update entity
    saved.setStatus("Processed");
    FlexStatementResponseEntity updated = repository.save(saved);

    assertThat(updated.getUpdatedAt())
        .isNotNull()
        .isAfter(saved.getUpdatedAt());
}
```

**UI Verification**:
1. Start application: `./mvnw spring-boot:run`
2. Navigate to index page
3. Verify "Admin Data" section is not visible
4. Verify "Admin" section is not visible
5. Verify main navigation still functions correctly

---

## Related Issues

- Related: ISSUE-006 (Database schema initialization - updatedAt column will be auto-created)

---

## Notes

**File Locations**:
- UI Template: `src/main/jte/index_jte.jte`
- Entity: `src/main/java/co/grtk/srcprofit/entity/FlexStatementResponseEntity.java`
- Pattern Reference: `src/main/java/co/grtk/srcprofit/entity/BaseAsset.java` (lines 43-47)

**Why Comment Instead of Delete**:
- Preserves admin functionality for debugging
- Easy to restore if needed in future
- Maintains git history of full implementation

**Estimated Effort**: 1-2 hours
**Risk Level**: Low (non-breaking changes, additive for entity)
