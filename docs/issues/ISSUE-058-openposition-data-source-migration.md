# ISSUE-058: OpenPosition Data Source Migration & Footer Calculations

**Created**: 2025-12-26 (Session)
**Status**: CLOSED
**Priority**: HIGH
**Category**: Code Quality / Technical Debt
**Blocking**: N/A

---

## Problem

PositionController was retrieving open options data from OptionService (manual CSV imports) instead of OpenPositionService (IBKR Flex Report API snapshots). Additionally, the OpenPositions template footer calculations were summing strike prices and underlying prices without weighting by position quantity, which doesn't reflect the true dollar-weighted totals.

---

## Root Cause

1. **Data Source Inconsistency**: OptionService queries OptionEntity (manually imported trading history), while OpenPositionService queries OpenPositionEntity (IBKR Flex Report snapshots from official APIs). The controller wasn't consistently using the authoritative IBKR data source.

2. **Unweighted Footer Calculations**: The DataTable footer was calculating simple arithmetic sums (sum of strikes, sum of prices) without multiplying by quantity, providing misleading totals that didn't represent actual position values.

---

## Approach

### Part 1: PositionController Migration
Replace three instances in PositionController where `optionService.getOpenOptionsByTicker()` was called with `openPositionService.getOpenOptionsByTickerDto()`:
- `loadPositionData()` - for viewing existing ticker positions
- `fillPositionFormData()` - for displaying position form data
- `loadPositionDataWithVirtual()` - for what-if scenario with virtual positions

For the virtual position handling, migrate the logic from `optionService.getOpenOptionsByTickerWithVirtual()` directly into the controller to avoid dependency on OptionService.

### Part 2: Template Footer Calculations
Update the DataTable footerCallback in openpositions_jte.jte to calculate weighted totals:
- Strike footer: `sum(strike × quantity)` instead of simple sum of strikes
- Price footer: `sum(price × quantity)` instead of simple sum of prices

---

## Success Criteria

- [x] PositionController.loadPositionData() uses openPositionService.getOpenOptionsByTickerDto()
- [x] PositionController.fillPositionFormData() uses openPositionService.getOpenOptionsByTickerDto()
- [x] PositionController.loadPositionDataWithVirtual() uses openPositionService.getOpenOptionsByTickerDto() with inline virtual position handling
- [x] Virtual position logic preserved in loadPositionDataWithVirtual() for ISSUE-028 compliance
- [x] openpositions_jte.jte footer calculates sum(strike × qty) weighted total
- [x] openpositions_jte.jte footer calculates sum(price × qty) weighted total
- [x] PositionController compiles without errors
- [x] No test failures in existing tests

---

## Acceptance Tests

All existing tests continue to pass:
```bash
./mvnw test  # No new test failures
./mvnw clean compile  # Compiles successfully
```

Verification:
- PositionController now retrieves from authoritative IBKR data source
- Footer totals in Open Positions table reflect quantity-weighted calculations
- Virtual position functionality preserved for what-if analysis (ISSUE-028)

---

## Related Issues

- Related to: ISSUE-045 (OpenPosition controller migration)
- Related to: ISSUE-028 (Virtual position what-if calculator)
- Related to: ISSUE-057 (Options datatable summary footer)

---

## Notes

- OptionService.getOpenOptionsByTicker() and getOpenOptionsByTickerWithVirtual() remain in codebase but are no longer used by PositionController
- The migration uses OpenPositionEntity (IBKR Flex Report snapshots) as the authoritative data source
- Virtual position handling moved inline to avoid OptionService dependency for this workflow
- Changes maintain backward compatibility - no breaking changes to public APIs

---

## Files Changed

1. `src/main/jte/openpositions_jte.jte` - Updated footer calculation logic (lines 408-423)
2. `src/main/java/co/grtk/srcprofit/controller/PositionController.java` - Migrated 3 method calls to OpenPositionService
   - Line 124: loadPositionData()
   - Line 147: fillPositionFormData()
   - Lines 259-267: loadPositionDataWithVirtual()
   - Added ArrayList import

**Completed**: 2025-12-26
