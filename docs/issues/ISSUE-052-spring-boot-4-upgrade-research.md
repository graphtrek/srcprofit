# ISSUE-052: Spring Boot 4.0 Upgrade Research

**Created**: 2025-12-18
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Infrastructure / Technical Debt
**Blocking**: None

---

## Problem

SrcProfit currently runs on Spring Boot 3.5.6 with Java 24. Spring Boot 4.0 introduces significant breaking changes including:
- Jakarta EE 11 baseline with Servlet 6.1 compliance
- Spring Framework 7.x requirement
- Major modularization of starter POMs
- Testing framework changes (@MockBean deprecated)
- Removal of several features (Undertow, Pulsar Reactive, embedded jar scripts)

We need comprehensive research to understand the upgrade impact and create an actionable migration plan before attempting the upgrade.

---

## Root Cause

Framework evolution driving architectural improvements:
- **Jakarta EE 11**: Next generation enterprise Java standard
- **Spring Framework 7.x**: Major framework upgrade with breaking changes
- **Modularization**: Moving from monolithic starters to granular technology-specific modules
- **Deprecation cleanup**: Removal of features deprecated in Spring Boot 3.x lifecycle

---

## Approach

### Phase 1: Requirements Analysis
Document Spring Boot 4.0 baseline requirements:
- Java 17+ (SrcProfit uses Java 24 ✅)
- Jakarta EE 11 with Servlet 6.1
- Spring Framework 7.x
- GraalVM native-image v25+ (if using native compilation)
- Kotlin v2.2+ (not applicable to SrcProfit)

### Phase 2: Breaking Changes Identification

#### 2.1 Starter POM Modularization
Spring Boot 4.0 breaks apart monolithic starters into granular modules:

**Migration Paths**:
- `spring-boot-starter-web` → `spring-boot-starter-webmvc` (for MVC)
- New explicit starters: `spring-boot-starter-restclient`, `spring-boot-starter-webclient`
- **Interim solution**: `spring-boot-starter-classic` for gradual migration

**SrcProfit Impact**:
- Current: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator`
- Action: Map to 4.0 equivalents or use classic starter initially

#### 2.2 Testing Framework Changes
- `@MockBean` / `@SpyBean` → deprecated, use `@MockitoBean` / `@MockitoSpyBean`
- `@SpringBootTest` no longer auto-enables MockMVC, WebClient, TestRestTemplate
- Required new annotations: `@AutoConfigureMockMvc`, `@AutoConfigureWebTestClient`, `@AutoConfigureTestRestTemplate`
- `MockitoTestExecutionListener` removed (deprecated in 3.4)
- Consider `RestTestClient` as `TestRestTemplate` replacement

**SrcProfit Impact**: Review all test files for deprecated annotations

#### 2.3 Removed Features
- **Undertow**: Eliminated (Servlet 6.1 incompatibility) - SrcProfit uses default Tomcat ✅
- **Pulsar Reactive**: Auto-configuration removed - Not used ✅
- **Embedded jar launch scripts**: Discontinued - Review Docker build process
- **Spring Session Hazelcast/MongoDB**: Moved to vendor leadership - Not used ✅
- **Spock integration**: Removed (Groovy 5 incompatibility) - Not used ✅

#### 2.4 Framework Upgrades
- **Jackson 3**: New group ID `tools.jackson` (from `com.fasterxml.jackson`)
- **Elasticsearch**: Rest5Client replaces deprecated RestClient
- MongoDB properties reorganized: `spring.data.mongodb` → `spring.mongodb`
- Spring Session properties: `spring.session.redis` → `spring.session.data.redis`

**SrcProfit Impact**:
- Jackson XML 2.19.1 → Check Jackson 3 compatibility
- No Elasticsearch or MongoDB usage ✅

### Phase 3: SrcProfit-Specific Impact Analysis

#### Current State Assessment (from codebase analysis)

**GREEN FLAGS (Low Risk)**:
1. ✅ Already using `jakarta.persistence` (not `javax.persistence`)
2. ✅ Using `RestClient` (not deprecated `RestTemplate`)
3. ✅ Constructor injection pattern (not `@Autowired` field injection)
4. ✅ Java 24 (exceeds Java 17+ minimum requirement)
5. ✅ Modern Spring Boot patterns throughout
6. ✅ Using `JpaRepository` with custom `@Query` methods
7. ✅ PostgreSQL driver (well-maintained, Spring Boot 4.0 compatible)
8. ✅ Virtual threads enabled (perfect for Spring Boot 4.0+)
9. ✅ Graceful shutdown configured
10. ✅ Spring Boot Actuator for observability

**YELLOW FLAGS (Review Needed)**:
1. ⚠️ **Hibernate Annotations** (`@CreationTimestamp`, `@UpdateTimestamp`)
   - Still supported in Spring Boot 4.0
   - Consider JPA standard alternatives for future-proofing

2. ⚠️ **JTE 3.2.1** (Java Template Engine)
   - File: `pom.xml` - dependency `jte-spring-boot-starter-3:3.2.1`
   - Action: Verify Spring Boot 4.0 compatibility with JTE maintainers
   - Templates: `index_jte`, `dashboard_jte`, `ibkr-login`

3. ⚠️ **Jackson XML 2.19.1**
   - File: `pom.xml` - dependency `jackson-dataformat-xml:2.19.1`
   - Action: Verify Jackson 3 migration path and group ID change

4. ⚠️ **@Modifying on custom repository methods**
   - Files: `OptionSnapshotRepository.java:deleteByExpirationDateBefore()`
   - Action: Ensure `@Transactional` properly handled in Spring Boot 4.0

5. ⚠️ **Custom RestClient beans**
   - File: `RestClientConfig.java` - multiple beans with custom SSL handling
   - Action: Review Spring Boot 4.0 RestClient API for breaking changes
   - Custom method: `disableSSLCertificateValidation()`

**RED FLAGS**: None identified

**Overall Risk Assessment**: **LOW** - Codebase is exceptionally well-prepared for Spring Boot 4.0

### Phase 4: Critical Integration Testing Plan

Priority testing areas after upgrade:

#### 4.1 JPA/Hibernate (HIGH Priority)
**Files to test**:
- `src/main/java/co/grtk/srcprofit/repository/OptionRepository.java`
- `src/main/java/co/grtk/srcprofit/repository/OpenPositionRepository.java`
- `src/main/java/co/grtk/srcprofit/repository/OptionSnapshotRepository.java`
- All entity files in `src/main/java/co/grtk/srcprofit/entity/`

**Test cases**:
- All custom `@Query` methods with `JOIN FETCH`
- Complex `WHERE` conditions and `COUNT` aggregations
- `@Modifying` queries with `@Transactional`
- Entity relationships (`@ManyToOne`, `@OneToMany`)
- Hibernate-specific annotations (`@CreationTimestamp`, `@UpdateTimestamp`)

#### 4.2 RestClient Configuration (MEDIUM Priority)
**Files to test**:
- `src/main/java/co/grtk/srcprofit/config/RestClientConfig.java`

**Test cases**:
- SSL certificate handling (custom `disableSSLCertificateValidation`)
- Multiple bean definitions (`alpacaRestClient`, `ibkrRestClient`, etc.)
- Custom headers and base URLs
- REST endpoint calls in `AlpacaRestController`, `IbkrRestController`

#### 4.3 Scheduled Tasks (MEDIUM Priority)
**Files to test**:
- `src/main/java/co/grtk/srcprofit/service/ScheduledJobsService.java` (8 scheduled methods)

**Test cases**:
- Task timing with `@Scheduled(fixedDelay, initialDelay, timeUnit)`
- Thread behavior with virtual threads enabled
- Concurrent execution patterns

#### 4.4 Jackson Serialization (MEDIUM Priority)
**Files to test**:
- `src/main/java/co/grtk/srcprofit/config/ObjectMapperConfig.java`

**Test cases**:
- `ZonedDateTime` serialization/deserialization
- `JsonIdentityInfo` for circular reference handling (used in entities)
- XML format support (`jackson-dataformat-xml`)

#### 4.5 JTE Template Rendering (LOW Priority)
**Files to test**:
- `src/main/java/co/grtk/srcprofit/controller/HomeController.java`
- Templates: `index_jte`, `dashboard_jte`, `ibkr-login`

**Test cases**:
- Template compilation
- Rendering performance
- Model attribute binding

### Phase 5: Migration Path Definition

#### Option A: Classic Starter (Recommended for Initial Upgrade)
1. Update `pom.xml`: `<version>3.5.6</version>` → `<version>4.0.x</version>`
2. Add `spring-boot-starter-classic` to maintain current behavior
3. Run full test suite
4. Validate all integration points (JPA, REST, scheduling, templates)
5. Test Docker build with Spring Boot 4.0

**Pros**: Minimal immediate changes, faster initial upgrade
**Cons**: Defers modularization work to future

#### Option B: Granular Starters (Full Migration)
1. Update `pom.xml` parent version
2. Replace `spring-boot-starter-web` with `spring-boot-starter-webmvc`
3. Add explicit starters: `spring-boot-starter-restclient`
4. Update test dependencies with new annotations
5. Migrate Jackson to version 3 (group ID change)
6. Run full test suite
7. Validate all integration points

**Pros**: Full Spring Boot 4.0 compliance from day one
**Cons**: More upfront work, potential for transitive dependency conflicts

#### Recommended Approach: Hybrid
1. Start with **Option A** (classic starter) to validate core functionality
2. Incremental migration to granular starters over 2-3 PRs
3. Each PR focuses on one area: web → REST clients → data access → testing

---

## Success Criteria

- [x] Complete dependency mapping (3.5.6 → 4.0) *(documented above)*
- [x] Breaking changes documented with mitigation strategies *(see Phase 2)*
- [x] Test plan created for critical integration points *(see Phase 4)*
- [x] Migration phases defined with risk assessment *(see Phase 5)*
- [ ] Create follow-up implementation issue (ISSUE-053)
- [ ] Verify JTE Spring Boot 4.0 compatibility
- [ ] Verify Jackson 3 migration path
- [ ] Document Docker build changes (if needed)

---

## Acceptance Tests

```bash
# Pre-upgrade validation
./mvnw clean test  # All tests pass on Spring Boot 3.5.6

# Post-upgrade validation (Spring Boot 4.0)
./mvnw clean test  # All tests still pass

# Integration tests
# 1. JPA queries execute correctly
# 2. REST endpoints respond properly
# 3. Scheduled tasks run on schedule
# 4. Templates render without errors
# 5. Jackson serialization works with ZonedDateTime
# 6. Docker build succeeds
# 7. Application starts and passes health checks
```

**Specific validation queries**:
```sql
-- Test JPA repository after upgrade
SELECT COUNT(*) FROM open_position WHERE position_effect = 'OPEN';
SELECT * FROM option_entity WHERE expiration_date > CURRENT_DATE;
```

---

## Related Issues

- Blocks: ISSUE-053 (Spring Boot 4.0 Upgrade Implementation) - *to be created*
- Related: ISSUE-006 (Docker Database Schema Initialization) - Docker build validation

---

## Notes

### Reference Documentation
- **Migration Guide**: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- **Spring Boot 4.0 Release Notes**: https://github.com/spring-projects/spring-boot/releases/tag/v4.0.0
- **Spring Framework 7.0 Upgrade**: https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-7.x

### Key Configuration Files
- `/Users/Imre/IdeaProjects/other/srcprofit/pom.xml` - Maven dependencies
- `/Users/Imre/IdeaProjects/other/srcprofit/src/main/resources/application.yaml` - Spring configuration
- `/Users/Imre/IdeaProjects/other/srcprofit/Dockerfile` - Container build

### Estimated Effort
**Research**: 3-4 hours (COMPLETE with this issue)
**Implementation**: 4-6 hours (phased approach)
- Phase 1 (classic starter): 2 hours
- Phase 2 (granular migration): 2-3 hours
- Phase 3 (validation & cleanup): 1-2 hours

### Dependencies to Monitor
```xml
<!-- Current versions to check for 4.0 compatibility -->
<jte.version>3.2.1</jte.version>
<jackson-dataformat-xml.version>2.19.1</jackson-dataformat-xml.version>
<commons-math3.version>3.6.1</commons-math3.version>
<postgresql.version>42.7.4</postgresql.version>
```

### Next Steps
1. Monitor Spring Boot 4.0 GA release date
2. Create ISSUE-053 when ready to implement
3. Check JTE project for Spring Boot 4.0 compatibility announcement
4. Review Jackson 3 migration guide when available
5. Consider upgrading to latest Spring Boot 3.5.x before 4.0 jump
