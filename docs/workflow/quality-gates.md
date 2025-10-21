# Quality Gates - SrcProfit

**Purpose**: 4-tier progressive validation for fast iteration and production confidence
**Last Updated**: 2025-10-21 (Session 1)

---

## 🎯 Philosophy

**Fast iteration > Perfect code during development**

Progressive quality gates enable rapid TDD while maintaining production standards.

---

## 📊 4-Tier Quality Strategy

| Tier | Command | Checks | Speed | Use Case |
|------|---------|--------|-------|----------|
| **TIER 0** | `/commit-wip` | None (skips all hooks) | Instant | Emergency checkpoint |
| **TIER 1** | `/commit` | Tests only | <30s | Fast TDD iteration |
| **TIER 2** | `/commit-review` | Format + Tests + Coverage | 2-3min | Code review ready |
| **TIER 3** | `/ship` | Full CI + PR + Deploy | 5-10min | Production release |

---

## TIER 0: Emergency Checkpoints

**Command**: `/commit-wip`

**Purpose**: Save state when tests are intentionally failing or end-of-day checkpoint

**Checks**: NONE (bypasses all hooks and validation)

**Use When**:
- End of day save (mid-implementation)
- Tests intentionally failing (red phase of TDD)
- Emergency context save (low context, need to stop)
- Mid-refactoring checkpoint

**Process**:
```bash
git add .
git commit -m "wip: description" --no-verify
git push
```

**Important**: Always clean up with TIER 1+ before creating PR

---

## TIER 1: Fast TDD Loop

**Command**: `/commit`

**Purpose**: Rapid iteration with minimal validation (<30s feedback)

**Checks**:
- ✅ Unit tests (JUnit 5)
- ✅ Block commits to master
- ❌ No formatting
- ❌ No linting
- ❌ No coverage reporting

**Maven Command**:
```bash
./mvnw test -Dtest=*Test -q
```

**Speed**: <30 seconds (enables 50-100 commits/day)

**Use When**:
- Red-Green-Refactor TDD cycle
- Rapid prototyping
- Frequent commits during feature development

**Why Fast**:
- No code formatting (defer to TIER 2)
- No static analysis (defer to TIER 2)
- No coverage calculation (defer to TIER 2)
- Tests prove correctness, style can wait

---

## TIER 2: Code Review Ready

**Command**: `/commit-review`

**Purpose**: Full quality validation before requesting review

**Checks**:
1. **Auto-format Code**:
   ```bash
   # Google Java Format (if configured)
   ./mvnw com.spotify.fmt:fmt-maven-plugin:format
   ```

2. **Static Analysis**:
   ```bash
   # Checkstyle
   ./mvnw checkstyle:check

   # SpotBugs
   ./mvnw spotbugs:check
   ```

3. **Full Test Suite with Coverage**:
   ```bash
   ./mvnw test jacoco:report
   # Verify coverage > threshold
   ```

4. **AI Code Review** (if Python changes in original):
   - For SrcProfit: Manual code review or adapt code-reviewer agent for Java
   - Saves review to `docs/code-reviews/SESSION_XXX_COMMIT_YYYYYYY.md`
   - Verdict: APPROVE / REQUEST_CHANGES / BLOCK

**Speed**: 2-3 minutes

**Use When**:
- Before requesting human code review
- After completing a feature
- End of work session (clean commits)
- Periodic quality checks (every 10-20 commits)

**Coverage Target**:
- Overall: >80%
- Domain logic: >90%
- Controllers: >70%

---

## TIER 3: Production Validation

**Command**: `/ship`

**Purpose**: Full CI/CD validation before production deployment

**Checks**:
1. **All TIER 2 checks**
2. **Security Scans**:
   ```bash
   # OWASP Dependency Check
   ./mvnw dependency-check:check

   # Maven Enforcer (dependency conflicts)
   ./mvnw enforcer:enforce
   ```

3. **Integration Tests**:
   ```bash
   ./mvnw verify -P integration-tests
   ```

4. **Docker Build**:
   ```bash
   docker build -t srcprofit:test .
   ```

5. **GitHub Actions CI**:
   - Existing workflow in `.github/workflows/ci.yml`
   - Builds with Maven
   - Creates Docker image
   - Pushes to ghcr.io

**Speed**: 5-10 minutes

**Use When**:
- Ready to merge to master
- Creating production release
- Before deployment

**Post-Ship**:
```bash
# Create PR
gh pr create --base master --fill

# Merge to master
gh pr merge --merge

# Sync branch
git pull origin master
git push origin claude
```

---

## 🔧 Maven Configuration

### Test Configuration

**Fast Tests (TIER 1)**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <groups>unit</groups>
        <excludedGroups>integration</excludedGroups>
    </configuration>
</plugin>
```

**Coverage (TIER 2)**:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <limits>
                    <limit>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### Quality Plugins

**Checkstyle**:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <configLocation>google_checks.xml</configLocation>
    </configuration>
</plugin>
```

**SpotBugs**:
```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.0.0</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Low</threshold>
    </configuration>
</plugin>
```

---

## 📈 Quality Metrics

### Coverage Targets

| Module | Target | Rationale |
|--------|--------|-----------|
| **Entity** | >95% | Domain model critical |
| **Service** | >90% | Business logic critical |
| **Repository** | >85% | Data access important |
| **Controller** | >70% | Focus on workflows |
| **Config** | >60% | Configuration setup |
| **Overall** | >80% | **Minimum for PR** |

### Complexity Targets

- **Cyclomatic Complexity**: <10 per method
- **Class Size**: <500 lines
- **Method Size**: <50 lines
- **Parameter Count**: <5 per method

---

## 🚨 Gate Failures

### What Blocks Commits?

**TIER 1** (Fast):
- ❌ Failing unit tests
- ❌ Direct commit to master

**TIER 2** (Review):
- ❌ All TIER 1 failures
- ❌ Coverage below threshold
- ❌ Checkstyle violations
- ❌ SpotBugs warnings

**TIER 3** (Production):
- ❌ All TIER 2 failures
- ❌ Integration test failures
- ❌ Security vulnerabilities
- ❌ Docker build failures
- ❌ CI/CD pipeline errors

### How to Fix

**Test Failures**:
```bash
# Run specific test
./mvnw test -Dtest=OptionServiceTest

# Debug mode
./mvnw test -Dtest=OptionServiceTest -X

# Generate coverage report
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

**Format Issues**:
```bash
# Auto-fix formatting
./mvnw fmt:format

# Verify formatting
./mvnw fmt:check
```

**Static Analysis**:
```bash
# Check style
./mvnw checkstyle:check

# Find bugs
./mvnw spotbugs:check

# View reports
open target/site/checkstyle.html
open target/spotbugsXml.html
```

---

## 🔄 TDD Workflow with Quality Gates

### Red-Green-Refactor (3-minute cycle)

```bash
# 1. RED - Write failing test (30s)
# Create test in src/test/java/.../OptionServiceTest.java

# 2. Run test to confirm failure
./mvnw test -Dtest=OptionServiceTest
# ❌ FAILED - as expected

# 3. GREEN - Make it pass (1.5 min)
# Implement in src/main/java/.../OptionService.java

./mvnw test -Dtest=OptionServiceTest
# ✅ PASSED

# 4. COMMIT - Fast feedback (30s)
/commit  # Uses TIER 1 (tests only)
# → Pre-commit runs tests (<30s)
# → Auto-generates commit message
# → Pushes to remote
# ✅ All checks pass

# 5. REFACTOR - Clean up (optional, do later with /commit-review)
```

**When to format/lint**:
- During TDD: Skip (use `/commit` for speed)
- Before code review: Run `/commit-review` (formats + lints)
- Before production: Run `/ship` (full validation)

---

## 📚 Related Documentation

- **Session State Transfer Protocol**: `docs/workflow/session-state-transfer-protocol.md`
- **Definition of Done**: `docs/planning/definition-of-done.md`
- **Testing Strategy**: `docs/workflow/testing-strategy.md`
- **CI/CD**: `.github/workflows/ci.yml`

---

## 🎓 Lessons Learned

**From Contrarian Project (180+ sessions)**:

1. **Fast TDD feedback is essential** - <30s enables 50-100 commits/day
2. **Progressive validation works** - Format/lint can wait until review time
3. **Tests prove correctness** - Style is secondary to working code
4. **Review before PR saves time** - Fix issues locally, not in PR comments
5. **Simple workflow wins** - No squash for trunk-based = simple `git pull`

---

**Version**: 1.0 (Session 1 - Adapted from contrarian)
**Source**: Contrarian Trading Portfolio System (Session 120 optimization)
**Next Review**: After first 10 commits in SrcProfit
