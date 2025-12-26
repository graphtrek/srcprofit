# ISSUE-059: Add API Availability Conditional Test Execution

**Created**: 2025-12-26 (Session N/A)
**Status**: CLOSED
**Priority**: MEDIUM
**Category**: Testing / Quality Assurance
**Blocking**: None

---

## Problem

Test execution should gracefully skip tests that require external API credentials when those credentials are not configured in the environment. This prevents test failures in CI/CD pipelines or local environments where API keys are not available.

---

## Root Cause

Previously, tests would fail if external APIs (Alpaca, Alpha Vantage) were not configured. There was no conditional execution mechanism to skip tests based on API availability.

---

## Approach

Implemented a JUnit 5 `ExecutionCondition` extension (`ApiAvailabilityCondition`) that evaluates whether required API credentials are present:
- Checks for `ALPACA_API_KEY` environment variable
- Checks for `ALPHA_VINTAGE_API_KEY` environment variable
- Disables test execution with informative message if either is missing
- Enables test execution when both are configured

Applied this condition to `ScheduledJobsServiceTest` which depends on API integration.

---

## Success Criteria

- [x] `ApiAvailabilityCondition` class created and implements `ExecutionCondition`
- [x] Tests skip gracefully when API credentials are missing
- [x] Clear skip messages indicate which credentials are missing
- [x] `ScheduledJobsServiceTest` updated to use the condition
- [x] Test dates updated to be current (expiration dates set to Jan 9, 2026)
- [x] Security settings updated to allow test execution in bash

---

## Changes Made

1. **New File**: `src/test/java/co/grtk/srcprofit/condition/ApiAvailabilityCondition.java`
   - Implements JUnit 5 `ExecutionCondition` extension
   - Checks for `ALPACA_API_KEY` and `ALPHA_VINTAGE_API_KEY` environment variables
   - Returns disabled condition with informative message if either is missing

2. **Updated**: `src/test/java/co/grtk/srcprofit/service/ScheduledJobsServiceTest.java`
   - Added import for `ApiAvailabilityCondition`
   - Updated extension to include `ApiAvailabilityCondition`
   - Added documentation about API credential handling

3. **Updated**: `src/test/java/co/grtk/srcprofit/service/ManualCalculationTest.java`
   - Updated test expiration date from 2025-12-26 to 2026-01-09
   - Updated assertion ranges from 45-46 days to 58-60 days to match new dates

4. **Updated**: `.claude/settings.local.json`
   - Added `Bash(timeout 60 ./mvnw test:*)` to security allow list

---

## Acceptance Tests

Tests pass with both success and skip scenarios:
- When API keys are configured: `ScheduledJobsServiceTest` executes normally
- When API keys are missing: Tests skip with informative message
- `ManualCalculationTest` passes with updated expiration dates

---

## Related Issues

- None

---

## Notes

This change enables the test suite to run in environments without full API configuration, improving developer experience and CI/CD reliability.
