# ISSUE-048: Persist tradeDate, daysBetween, and roi in OpenPositionEntity during saveCSV

## Status: CLOSED
## Priority: Medium
## Type: Performance Optimization
## Created: 2025-12-07
## Completed: 2025-12-07

---

## Summary

Move the calculation of `tradeDate`, `daysBetween`, and `roi` fields from `convertToOpenPositionViewDto()` to `saveCSV()` in OpenPositionService. These values are immutable once a position is created, so they should be calculated once at import time and persisted, rather than recalculated on every page load.

---

## Problem

Currently, every call to `convertToOpenPositionViewDto()` (OpenPositionService.java:635-702) performs:

1. **Database query** to OptionEntity to find tradeDate (lines 646-665)
2. **Calculation** of daysBetween using PositionCalculationHelper (line 668)
3. **Calculation** of roi using PositionCalculationHelper (lines 671-675)

This happens **on every page render**, creating:
- N+1 query problem (1 query per position to lookup OptionEntity)
- Redundant calculations for values that never change
- Slower page load times as position count grows

---

## Solution

### 1. Add Fields to OpenPositionEntity

Add three new persisted columns to `OpenPositionEntity.java`:

```java
@Column
private LocalDate tradeDate;

@Column
private Integer daysBetween;

@Column
private Integer roi;
```

### 2. Calculate in saveCSV()

In `OpenPositionService.saveCSV()`, after setting CSV fields and before saving, calculate and persist these fields.

### 3. Simplify convertToOpenPositionViewDto()

Replace calculation logic with direct reads from persisted fields.

---

## Files to Modify

- `OpenPositionEntity.java` - Add fields
- `OpenPositionService.java:saveCSV()` - Add calculation logic
- `OpenPositionService.java:convertToOpenPositionViewDto()` - Read from entity

---

## Benefits

1. **Performance**: Eliminates N+1 query problem on page load
2. **Consistency**: Follows OptionEntity pattern already in codebase
3. **Correctness**: Values are truly immutable
