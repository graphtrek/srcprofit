# ISSUE-004: ScheduledReports Consolidation - Centralized Job Scheduling Architecture

**Created**: 2025-11-03 (Session 4)
**Status**: OPEN
**Priority**: HIGH
**Category**: Code Quality / Technical Debt
**Blocking**: None

---

## Problem

Current architecture has scheduling concerns mixed with business logic across multiple services, leading to:

1. **Thread-Safety Issue** (Critical): FlexReportsService uses non-thread-safe instance variables for retry state management
2. **Separation of Concerns Violation**: Services have both scheduling AND orchestration responsibilities
3. **No Centralized Observability**: Hard to monitor all scheduled jobs, execution frequency, duration, or failure rates
4. **Inconsistent Patterns**: Each service implements scheduling differently

**Affected Services**:
- `FlexReportsService.java:92-127, 152-187` - 2 @Scheduled methods (FLEX trades import, NAV import)
- `MarketDataService.java:33-56` - 1 @Scheduled method (Alpaca market data refresh)

**Thread-Safety Bug** (FlexReportsService.java:49):
```java
// TODO: Consider thread-safe implementation for concurrent scheduled jobs
private FlexStatementResponse flexTradesResponse = null;
private int tradesReferenceCodeCounter = 0;
private FlexStatementResponse flexNetAssetValueResponse = null;
private int netAssetValueReferenceCodeCounter = 0;
```

**Risk**: If Spring's scheduler runs concurrent executions OR manual API endpoints are called during scheduled jobs, state corruption is possible.

---

## Root Cause

**Historical Context** (Session 3):
- Session 3 eliminated circular dependency by removing separate `ScheduledReportsService`
- Moved `@Scheduled` annotations into `FlexReportsService` itself
- This resolved the bean lifecycle issue but left scheduling distributed

**Current State**:
- Scheduling concerns still embedded in business logic services
- No separation between "when to run" (scheduling) and "what to do" (orchestration)
- Instance variable state management is NOT thread-safe

---

## Approach

**Option A: Full Consolidation** (Chosen)

Create centralized `ScheduledJobsService` to own ALL scheduling concerns.

### Architecture

```
ScheduledJobsService (NEW)
├── @Scheduled importFlexTrades() → delegates to FlexReportsOrchestrator
├── @Scheduled importFlexNetAssetValue() → delegates to FlexReportsOrchestrator
└── @Scheduled refreshMarketData() → delegates to MarketDataOrchestrator
```

### Refactoring Steps

1. **Create ScheduledJobsService**
   - Centralize all @Scheduled annotations
   - Delegate to existing orchestrator services
   - Add centralized logging/monitoring hooks

2. **Refactor FlexReportsService → FlexReportsOrchestrator**
   - Remove @Scheduled annotations (lines 92, 152)
   - Remove instance variable state (fix thread-safety issue)
   - Return Result<T> objects instead of managing state
   - Pure orchestration: coordinate IBKR API → CSV parsing → persistence

3. **Refactor MarketDataService → MarketDataOrchestrator**
   - Remove @Scheduled annotation (line 33)
   - Pure orchestration: coordinate Alpaca API → database updates

4. **Thread-Safety Solution**
   - No instance variables for retry state
   - Pass state as method parameters OR use stateless design
   - Consider retry logic at ScheduledJobsService level (consistent across all jobs)

### Benefits

- ✅ Single place for scheduling configuration
- ✅ Fixes thread-safety bug (no shared mutable state)
- ✅ Clean separation of concerns
- ✅ Easier to add monitoring/metrics (single interception point)
- ✅ Easier to test (mock scheduler)
- ✅ Consistent retry/error handling patterns

### Trade-offs

- ⚠️ Additional service layer (more indirection)
- ⚠️ Requires refactoring 2 existing services
- ⚠️ Need to update controller endpoints if they call scheduled methods directly

---

## Success Criteria

### Phase 1: Architecture (Core Implementation)
- [ ] ScheduledJobsService created with @EnableScheduling
- [ ] All @Scheduled methods moved from FlexReportsService to ScheduledJobsService
- [ ] All @Scheduled methods moved from MarketDataService to ScheduledJobsService
- [ ] FlexReportsService renamed to FlexReportsOrchestrator (no @Scheduled annotations)
- [ ] MarketDataService updated (no @Scheduled annotations)

### Phase 2: Thread-Safety (Bug Fix)
- [ ] No instance variables for retry state in any orchestrator service
- [ ] Stateless design OR thread-safe state management (AtomicReference, etc.)
- [ ] Concurrent execution safety verified

### Phase 3: Verification (Testing)
- [ ] Clean compile: `./mvnw clean compile` succeeds
- [ ] All tests pass: `./mvnw test` succeeds
- [ ] Integration test: Spring Boot starts without circular dependency errors
- [ ] Integration test: Scheduled jobs execute on schedule
- [ ] Integration test: Manual API endpoints still work (`/ibkrFlexTradesImport`, etc.)

### Phase 4: Observability (Monitoring)
- [ ] Centralized logging in ScheduledJobsService (job start/end/duration)
- [ ] Error handling logged consistently
- [ ] Consider metrics (execution count, failure rate) - optional for MVP

---

## Acceptance Tests

### Test 1: Scheduled Jobs Execute
```bash
# Start application
./mvnw spring-boot:run

# Verify in logs:
# - "ScheduledJobsService: Starting importFlexTrades()" every 30 minutes
# - "ScheduledJobsService: Starting importFlexNetAssetValue()" every 30 minutes
# - "ScheduledJobsService: Starting refreshMarketData()" every 60 seconds

# Expected: All 3 jobs execute on schedule, no errors
```

### Test 2: Manual API Endpoints Work
```bash
# Test trades import endpoint
curl -X POST http://localhost:8080/ibkrFlexTradesImport
# Expected: Success response, same as before

# Test NAV import endpoint
curl -X POST http://localhost:8080/ibkrFlexNetAssetValueImport
# Expected: Success response, same as before
```

### Test 3: Thread-Safety Verified
```java
// Concurrent execution test (unit test)
@Test
void testConcurrentScheduledJobExecution() {
    ExecutorService executor = Executors.newFixedThreadPool(5);
    List<Future<?>> futures = new ArrayList<>();

    // Simulate 5 concurrent calls to scheduled method
    for (int i = 0; i < 5; i++) {
        futures.add(executor.submit(() ->
            scheduledJobsService.importFlexTrades()
        ));
    }

    // Wait for all to complete
    futures.forEach(f -> assertDoesNotThrow(() -> f.get()));

    // Verify: No state corruption, no exceptions
}
```

### Test 4: No Circular Dependencies
```bash
# Clean build from scratch
./mvnw clean compile

# Expected: SUCCESS, no circular dependency errors
# Expected: No "The dependencies of some of the beans in the application context form a cycle"
```

---

## Related Issues

- **Built upon**: ISSUE-003 (FLEX Reports Automatic Synchronization)
  - Session 3 resolved circular dependency by moving @Scheduled to FlexReportsService
  - This issue completes the refactoring by extracting scheduling to dedicated service

- **Blocks**: Future observability/monitoring features
  - Once centralized, easier to add metrics, alerting, circuit breakers

---

## Implementation Plan

### Files to Create
- `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java` (NEW)
  - @Service, @EnableScheduling
  - 3 @Scheduled methods delegating to orchestrators

### Files to Modify
- `src/main/java/co/grtk/srcprofit/service/FlexReportsService.java`
  - Rename to FlexReportsOrchestrator.java
  - Remove @Scheduled annotations (lines 92, 152)
  - Remove instance variables (lines 49-52)
  - Refactor methods to be stateless

- `src/main/java/co/grtk/srcprofit/service/MarketDataService.java`
  - Remove @Scheduled annotation (line 33)
  - Refactor refreshAlpacaMarketData() to be called by scheduler

- `src/main/java/co/grtk/srcprofit/controller/IbkrRestController.java`
  - Update dependency injection (FlexReportsService → FlexReportsOrchestrator)
  - Verify endpoints still work

- `src/main/java/co/grtk/srcprofit/SrcProfitApplication.java`
  - Move @EnableScheduling to ScheduledJobsService (if needed)
  - Or keep it here (verify Spring Boot best practice)

### Estimated Effort
- **Phase 1 (Architecture)**: 2 hours
- **Phase 2 (Thread-Safety)**: 1 hour
- **Phase 3 (Verification)**: 1 hour
- **Phase 4 (Observability)**: 30 minutes (optional)
- **Total**: 4-5 hours

---

## Notes

### Why This Matters (Business Value)
- **Reliability**: Fixes potential thread-safety bug that could corrupt retry state
- **Maintainability**: Easier to understand what jobs run when (single source of truth)
- **Observability**: Foundation for monitoring production scheduled jobs
- **Scalability**: Easier to add new scheduled jobs in future (just add to ScheduledJobsService)

### Spring Boot Scheduling Best Practices
- Use `@EnableScheduling` on configuration class or main application class
- Prefer `fixedDelay` over `fixedRate` for sequential job execution
- Consider `@Async` for truly concurrent jobs (requires thread pool configuration)
- Use cron expressions for complex schedules (e.g., business hours only)

### Alternative Considered (Option B: Thread-Safety Fix Only)
- Could just fix FlexReportsService with AtomicReference/AtomicInteger
- Faster (30 minutes), but doesn't address architecture issues
- Deferred for future: This option rejected in favor of full consolidation

### Follow-up Work (Future Issues)
- Add Spring Boot Actuator metrics for scheduled job monitoring
- Add circuit breaker for external API failures (Resilience4j)
- Add retry policies with exponential backoff (Spring Retry)
- Consider Quartz scheduler for advanced scheduling needs (DB-backed, clustering)

---

## References

- **Session 3 Context**: `/Users/Imre/IdeaProjects/other/srcprofit/docs/sessions/SESSION_03_COMPLETE.md`
- **Current Implementation**:
  - FlexReportsService: `/Users/Imre/IdeaProjects/other/srcprofit/src/main/java/co/grtk/srcprofit/service/FlexReportsService.java`
  - MarketDataService: `/Users/Imre/IdeaProjects/other/srcprofit/src/main/java/co/grtk/srcprofit/service/MarketDataService.java`
- **Spring Scheduling Docs**: https://docs.spring.io/spring-framework/reference/integration/scheduling.html
