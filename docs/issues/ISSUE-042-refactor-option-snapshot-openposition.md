# ISSUE-042: Refactor OptionSnapshotService to Use OpenPositionEntity

## Status
OPEN

## Description

Refactor `OptionSnapshotService.refreshOptionSnapshots()` to use `OpenPositionEntity` as the authoritative source of truth for option positions instead of the legacy `OptionRepository.findInstrumentsWithOpenPositions()` approach.

### Problem Statement

The current implementation fetches ALL options in a ±10% strike range regardless of whether they're actually held. This results in:
- Unnecessary API calls for out-of-range options
- Database bloat with irrelevant snapshots
- No direct link between held positions and their snapshots

### Solution

- Use `OpenPositionEntity` (IBKR Flex Report data) as the source of truth
- Fetch snapshots for held positions PLUS nearby strikes (±2 strikes for trading context)
- Implement fail-fast validation for data quality
- Remove legacy `OptionRepository` dependency

### Acceptance Criteria

- [x] Snapshots refreshed for exact held positions
- [x] Snapshots refreshed for ±2 nearby strikes
- [x] Fail-fast on null `underlyingInstrument` relationships
- [x] `OptionRepository` dependency removed
- [x] Batch by underlying (fewer API calls)
- [x] All 7 existing tests pass
- [x] 3 new tests added (OCC construction, null validation, nearby strikes)
- [x] Manual validation with real IBKR data

## Technical Details

### Files Modified

1. **OptionSnapshotService.java**
   - Remove `OptionRepository` dependency
   - Add `OpenPositionRepository` dependency
   - Replace `refreshOptionSnapshots()` (lines 84-109)
   - Replace `refreshSnapshotsForInstrument()` with `refreshSnapshotsForUnderlying()` (lines 120-158)
   - Modify `fetchAndSaveSnapshots()` to filter by symbol set (line 170)
   - Add 8 new helper methods (OCC construction, grouping, range calculation)
   - Add 2 new helper classes (StrikeRange, ExpirationRange)

2. **OptionSnapshotServiceTest.java**
   - Update all 7 existing tests to use `OpenPositionEntity`
   - Add 3 new tests (OCC construction, null instrument fail-fast, nearby strikes)
   - Add `createMockOpenPosition()` helper method

### Key Design Decisions

1. **Source of Truth**: `OpenPositionEntity` from IBKR Flex Report (authoritative)
2. **Scope**: Held positions + ±2 nearby strikes (trading context)
3. **Validation**: Fail-fast on null relationships (data quality enforcement)
4. **Batching**: Group by underlying symbol (API efficiency)
5. **Backward Compatibility**: Complete replacement (clean break from legacy)

### Implementation Sequence

1. Add `OpenPositionRepository` dependency
2. Implement helper classes and methods
3. Refactor core `refreshOptionSnapshots()` logic
4. Add filtering logic to `fetchAndSaveSnapshots()`
5. Remove `OptionRepository` dependency
6. Update all tests
7. Run full test suite
8. Manual validation

### Risk Assessment

| Risk | Level | Mitigation |
|------|-------|-----------|
| OCC symbol mismatch | HIGH | Unit tests + validation against real API |
| Missing underlyingInstrument | HIGH | Fail-fast + clear error message |
| Strike increment calculation | HIGH | Industry-standard OCC rules + edge case testing |
| N+1 queries | MEDIUM | Use JOIN FETCH in repository query |
| API rate limiting | MEDIUM | Keep 3-second delay between underlyings |
| Breaking changes | LOW | Internal method - public interface unchanged |

## Implementation Plan

See `/Users/Imre/.claude/plans/curious-watching-dewdrop.md` for detailed implementation plan with code examples.

## Success Metrics

1. **Functionality**: Snapshots correctly refreshed for held + nearby positions
2. **Performance**: Fewer API calls than before (batched strategy)
3. **Quality**: Fail-fast validation prevents silent errors
4. **Coverage**: All tests pass (7 updated + 3 new)
5. **Validation**: Manual test confirms correct behavior

## Related Issues

- ISSUE-041: Establish JPA relationships between OpenPositionEntity and InstrumentEntity (prerequisite - COMPLETED)
- ISSUE-040: Add Graceful FLEX Report Configuration Handling

## Owner

Claude Code

## Created Date

2025-11-30

## Completed Date

(TBD)

## Notes

- This is a clean break from the legacy `OptionRepository` approach
- OpenPositionEntity provides ground truth via IBKR Flex Report
- Fail-fast validation ensures data quality
- Nearby strikes (±2) provide trading context without excessive API calls
