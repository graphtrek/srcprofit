# ISSUE-041: JPA Relationships Between OpenPositionEntity and InstrumentEntity

**Created**: 2025-11-30
**Completed**: 2025-11-30
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Enhancement
**Blocking**: None

---

## Problem

OpenPositionEntity and InstrumentEntity have no JPA relationships, requiring manual lookups by conid/underlyingConid. This leads to:
1. **N+1 Query Problems**: Loading positions + instruments requires separate queries
2. **Manual Joins**: Service layer must manually look up instruments
3. **No Type Safety**: No compile-time relationship validation
4. **Missed Optimizations**: Can't use JOIN FETCH for eager loading

Example current code:
```java
OpenPositionEntity position = repository.findByConid(123L);
Long underlyingConid = position.getUnderlyingConid();
InstrumentEntity instrument = instrumentRepo.findByConid(underlyingConid); // Manual lookup
```

---

## Solution

Establish JPA relationships between OpenPositionEntity and InstrumentEntity:
- **OPTIONS (assetClass="OPT")**: Link via `underlyingConid` field
- **STOCKS (assetClass="STK")**: Link via `conid` field

### Two Separate Relationships Approach

Add TWO ManyToOne relationships to OpenPositionEntity:
1. `underlyingInstrument` - For options, joins via underlyingConid
2. `instrument` - For stocks, joins via conid
3. Helper method `getRelatedInstrument()` - Returns correct relationship based on assetClass

**Rationale**:
- Explicit and clear semantics
- Follows existing codebase patterns
- Type-safe, IDE-friendly
- No database migration needed (tables can be recreated)

---

## Implementation

### Step 1: Update OpenPositionEntity

Add two ManyToOne relationships with `NO_CONSTRAINT` foreign keys (since not all positions have corresponding instruments):

```java
@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "underlyingConid", referencedColumnName = "conid",
            insertable = false, updatable = false, nullable = true,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
private InstrumentEntity underlyingInstrument;

@JsonIgnore
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "conid", referencedColumnName = "conid",
            insertable = false, updatable = false, nullable = true,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
private InstrumentEntity instrument;

public InstrumentEntity getRelatedInstrument() {
    if ("OPT".equals(assetClass)) {
        return underlyingInstrument;
    } else if ("STK".equals(assetClass)) {
        return instrument;
    }
    return null;
}
```

**Key Annotations**:
- `insertable = false, updatable = false` - Columns already managed as primitives
- `referencedColumnName = "conid"` - Join on natural key, not id
- `nullable = true` - Not all positions have corresponding instruments
- `foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT)` - No database constraints (read-only mappings, data integrity handled at application level)
- `@JsonIgnore` - Prevent circular serialization
- `LAZY` fetch - Load only when needed

### Step 2: Add Repository Query Methods

Add to OpenPositionRepository:

```java
@Query("SELECT op FROM OpenPositionEntity op " +
       "LEFT JOIN FETCH op.underlyingInstrument " +
       "WHERE op.assetClass = 'OPT'")
List<OpenPositionEntity> findAllOptionsWithUnderlying();

@Query("SELECT op FROM OpenPositionEntity op " +
       "LEFT JOIN FETCH op.instrument " +
       "WHERE op.assetClass = 'STK'")
List<OpenPositionEntity> findAllStocksWithInstrument();

@Query("SELECT op FROM OpenPositionEntity op " +
       "LEFT JOIN FETCH op.underlyingInstrument i " +
       "WHERE op.assetClass = 'OPT' AND i.ticker = :ticker")
List<OpenPositionEntity> findOptionsByUnderlyingTicker(@Param("ticker") String ticker);

@Query("SELECT op FROM OpenPositionEntity op " +
       "LEFT JOIN FETCH op.instrument " +
       "LEFT JOIN FETCH op.underlyingInstrument")
List<OpenPositionEntity> findAllWithInstruments();
```

### Step 3: Verify InstrumentRepository Method

Ensure InstrumentRepository has:
```java
InstrumentEntity findByConid(Long conid);
```

### Step 4: Optional - Add Inverse Relationships to InstrumentEntity

```java
@JsonIgnore
@OneToMany(mappedBy = "instrument", fetch = FetchType.LAZY)
private List<OpenPositionEntity> directPositions;

@JsonIgnore
@OneToMany(mappedBy = "underlyingInstrument", fetch = FetchType.LAZY)
private List<OpenPositionEntity> underlyingPositions;
```

---

## Success Criteria

- ✅ OpenPositionEntity has both `instrument` and `underlyingInstrument` relationships
- ✅ `getRelatedInstrument()` returns correct instrument based on assetClass
- ✅ Repository queries use JOIN FETCH to avoid N+1 problems
- ✅ Tests verify relationship loading and navigation
- ✅ NULL handling works correctly for missing instruments
- ✅ All existing tests still pass
- ✅ No performance regressions

---

## Design Decisions

### Decision 1: Two Relationships vs. One Computed
**Chosen**: Two separate relationships

**Rationale**: Explicit semantics, follows codebase patterns, type-safe

### Decision 2: Join on conid vs. id
**Chosen**: Join on conid (natural key)

**Rationale**: conid is business identifier, both entities have unique conid

### Decision 3: insertable/updatable flags
**Chosen**: Both false

**Rationale**: Columns already managed as primitives, read-only mapping

### Decision 4: LAZY vs. EAGER fetch
**Chosen**: LAZY

**Rationale**: Follows all existing patterns, better performance, use JOIN FETCH when needed

### Decision 5: @JsonIgnore
**Chosen**: Add to relationships

**Rationale**: Prevents circular references, DTOs should be used for API responses

---

## Files Modified

**MODIFY**:
1. `src/main/java/co/grtk/srcprofit/entity/OpenPositionEntity.java`
2. `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java`
3. `src/main/java/co/grtk/srcprofit/repository/InstrumentRepository.java`

**OPTIONAL**:
4. `src/main/java/co/grtk/srcprofit/entity/InstrumentEntity.java`

---

## Related Issues

- Pattern: Existing relationships in BaseAsset, OptionSnapshotEntity
- Uses: ISSUE-039 OpenPositionEntity structure

---

## Notes

- User confirmed tables can be recreated - no Flyway migration needed
- Relationships are read-only mappings of existing columns
- Service layer should handle null instruments gracefully
- Always use LEFT JOIN (data integrity - not all positions have instruments)
