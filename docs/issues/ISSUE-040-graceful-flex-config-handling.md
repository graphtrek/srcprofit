# ISSUE-040: Graceful FLEX Report Configuration Handling

**Created**: 2025-11-30
**Completed**: 2025-11-30
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Enhancement
**Blocking**: None

---

## Problem

Three FLEX report import methods use `environment.getRequiredProperty()` which throws exceptions if configurations are missing:
1. `importFlexTrades()` - IBKR_FLEX_TRADES_ID
2. `importFlexNetAssetValue()` - IBKR_FLEX_NET_ASSET_VALUE_ID
3. `importFlexOpenPositions()` - ✅ Already fixed

Missing configurations should be gracefully skipped with warning logs, not crash the scheduler.

---

## Solution

Apply the established pattern from `importFlexOpenPositions()` to both methods:
- Replace `getRequiredProperty()` with `getProperty()`
- Check for null/empty and skip gracefully
- Log warning with property name and report type
- Return "SKIPPED/0" status

---

## Implementation

**Files Modified**: 2
1. FlexReportsService.java - Add config checks to 2 methods
2. ScheduledJobsService.java - Update JavaDoc

**Return Format**:
- Success: "{records}/{additional}" (e.g., "42/3/0" for trades)
- Skipped: "SKIPPED/0" (when config missing)

---

## Related Issues

- ISSUE-039: IBKR Flex Open Positions Import (pattern reference)
