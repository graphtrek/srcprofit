# Claude Project Initialization Template

**Version**: 1.6
**Created**: 2025-10-08
**Last Updated**: 2025-10-20 (Session 180 - Code review workflow improvements)
**Purpose**: Comprehensive initial prompt for starting new development projects with Claude
**Source Project**: Contrarian Trading Portfolio System (174+ sessions, 1000+ commits)

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Foundation Protocols](#foundation-protocols-new---session-122)
3. [Project Setup Foundation](#project-setup-foundation)
4. [TDD Workflow & Quality Gates](#tdd-workflow--quality-gates)
5. [Architecture Principles](#architecture-principles)
6. [Development Process](#development-process)
7. [Documentation Standards](#documentation-standards)
8. [Collaboration Style](#collaboration-style)

---

## Quick Start

Copy this template and customize for your project:

```markdown
# New Project - Initial Context for Claude

## Project Overview
- **Name**: [Your Project Name]
- **Tech Stack**: [Languages, frameworks, databases]
- **Domain**: [Business domain/purpose]
- **Team**: [Solo/Team size]

## Adopt These Practices
I want to use the proven TDD workflow from the Contrarian project:
- Trunk-based development with smart sync
- Ground Truth TDD methodology
- 3-tier quality gates
- Clean architecture
- Proactive subagent usage

See: /path/to/CLAUDE_PROJECT_INIT_TEMPLATE.md
```

---

## Foundation Protocols (NEW - Session 122)

### Why These Protocols Exist

After 122 sessions, we identified critical quality degradation patterns:
1. **False "done" claims** - "Implementation complete" but 5/8 tests failing
2. **RTFM failures** - Guessing API behavior instead of reading docs (100+ times!)
3. **Vague handoffs** - "Continue working on X" (next session wastes 30min understanding)
4. **Optimistic completion** - "90% complete" but user testing not done
5. **Context waste** - Poor planning leads to hitting 200k token limit mid-task

**Solution**: Two foundation protocols that MUST be followed:

### 1. Session State Transfer Protocol

**File**: `docs/SESSION_STATE_TRANSFER_PROTOCOL.md`

**Purpose**: Zero-degradation context handoffs between sessions

**Core Principle**: Quality degradation between sessions is UNACCEPTABLE.

**Key Requirements**:

1. **Definition of DONE** (strict):
   ```
   DONE = Code Written
        + Tests Pass
        + User Tested (if user-facing)
        + UX Validated (if UI/output)
        + Bugs Fixed
        + Code Committed
   ```

2. **Test Execution Rules** (agent states rule, user can override):
   - Agent: "According to test execution rules, I will run tests now."
   - Agent: "According to test execution rules, this requires user testing."
   - User can always override either direction

3. **Session Handoff Checklist**:
   - [ ] All claimed "completed" work meets Definition of Done
   - [ ] Completion percentages reflect REALITY (not optimism)
   - [ ] Uncommitted code explicitly listed
   - [ ] Test failures explicitly listed (if any)
   - [ ] Next steps are actionable (not vague)
   - [ ] Blockers explicit (with workarounds if known)
   - [ ] Dependencies clear (what needs to happen first)
   - [ ] User testing queue has EXACT commands to run

4. **Context Efficiency Strategies**:
   - Start with /start-session (7k tokens), not /full-context (35k)
   - Batch similar work (update 3 agents at once, not one per session)
   - Read files in parallel (3x faster)
   - Test strategically (don't re-run if code unchanged)
   - Strategic restart when >150k tokens and major tasks remain

5. **Session Commands** (2-command model, Session 169):
   - `/start-session`: Auto-detects normal vs emergency resume (no decision needed)
   - `/end-session`: Auto-detects normal vs emergency exit (uncommitted code, low context)
   - Always creates SESSION_XX_COMPLETE.md (metadata flag for emergency state)
   - No separate emergency commands needed (auto-detection eliminates user decision fatigue)

**Common Handoff Failures to Avoid**:
- ❌ "95% complete" (but 5 tests failing) → Reality: NOT complete
- ❌ "Continue working on feature X" → Reality: Where to start?
- ❌ "Feature ready" (but depends on unreleased API) → Reality: Blocked!
- ❌ "CLI command complete" (never shown to user) → Reality: Needs testing!
- ❌ Load /full-context for bug fix → Reality: Wasted 30k tokens!

### 2. Definition of Done

**File**: `docs/DEFINITION_OF_DONE.md`

**Purpose**: Clear, enforceable completion criteria for all work

**The Core Formula**:
```
DONE = Code Written
     + Tests Pass
     + User Tested (if user-facing)
     + UX Validated (if UI/output)
     + Bugs Fixed
     + Code Committed
```

**Completion Checklist** (all required):
- [ ] Code written and follows project standards
- [ ] Unit tests written and passing
- [ ] Integration tests passing (if applicable)
- [ ] No regression (existing tests still pass)
- [ ] User tested (if user-facing feature)
- [ ] UX validated (if output/UI changes)
- [ ] Documentation updated (if needed)
- [ ] Code committed and pushed
- [ ] No known bugs

**Percentage Completion Guide** (honest assessment):

If Definition of Done has 8 criteria and you've met 5:
- Calculation: 5/8 = 62.5% complete (round to 60%)
- **Wrong**: "Implementation complete, just needs testing" (implies 90%+)
- **Right**: "60% complete - code working, tests passing, needs user validation"

**Common Ranges**:
- 0-25%: Started, significant work remains
- 25-50%: Substantial progress, major components incomplete
- 50-75%: Core work done, testing/validation incomplete
- 75-90%: Nearly done, minor issues or final validation pending
- 90-99%: All criteria met except one or two final items
- 100%: ALL Definition of Done criteria met

**Rule**: Never claim >90% if user testing not done

**When User Must Test** (agent states, user can override):
- CLI command output (formatting, readability)
- Interactive workflows (multi-step processes)
- Real portfolio data validation
- Edge cases requiring domain knowledge
- UX/usability evaluation
- Ground truth comparison (e.g., TastyTrade CSV)

**When Automated Tests Sufficient**:
- Internal functions (no user visibility)
- Data transformations (with known inputs/outputs)
- Algorithm correctness (unit testable)
- Regression prevention (existing tests)
- Code quality (lint, format, type checks)

### 3. RTFM Enforcement (NEW - Session 122)

**Problem**: 100+ times told "check docs first" - kept guessing anyway!

**Solution**: MANDATORY API documentation check before ANY implementation

**Process** (NON-NEGOTIABLE):
```
Step 0: RTFM (Read The F***ing Manual)
↓
Step 1: Use WebFetch to read official API docs
↓
Step 2: Document what you learned
↓
Step 3: Implement based on docs (not assumptions)
```

**Common RTFM Failures** (Session 122 patterns):
- ❌ Guessing pagination works like "other APIs" → Reality: TastyTrade uses `page-offset`, not `item-offset`
- ❌ Assuming option symbols use standard format → Reality: Futures options need 3-part format
- ❌ Hardcoding multiplier = 100 for all options → Reality: Futures options vary (/GC=100, /ES=50, /NG=10000)
- ❌ Using TastyTrade format for DXLink → Reality: Different symbol formats per API

**RTFM Rules**:
1. ALWAYS read docs BEFORE implementing
2. NEVER guess API endpoint names
3. NEVER assume response formats
4. NEVER hardcode without checking docs
5. NEVER copy patterns from other APIs

### How to Integrate These Protocols

**Step 1**: Copy protocol files to your project:
```bash
# From contrarian project
cp docs/SESSION_STATE_TRANSFER_PROTOCOL.md your-project/docs/
cp docs/DEFINITION_OF_DONE.md your-project/docs/
```

**Step 2**: Update /end-session command:
- Add "Required Reading" section referencing protocols
- Add protocol verification checklist before creating summary
- Add protocol enforcement section with quality indicators

**Step 3**: Create/Update Agents (if using Claude Code agents):
- **code-reviewer**: Add RTFM check + ground truth validation + Definition of Done verification
- **api-integration-specialist**: NEW agent for API-first thinking + RTFM enforcement
- **python-pro/java-pro/etc**: Add API documentation CHECK section
- **trading-specialist** (if financial): Add position risk analysis + futures multiplier checks

**Step 4**: Reference in CLAUDE.md:
```markdown
## Quality Protocols (Session 122+)

MANDATORY reading before any session handoff:
- `docs/SESSION_STATE_TRANSFER_PROTOCOL.md` - Zero-degradation handoffs
- `docs/DEFINITION_OF_DONE.md` - Completion criteria

These protocols prevent:
- False "done" claims (saves 30min+ per session)
- API guessing failures (saves hours of debugging)
- Vague handoffs (next session starts immediately)
- Context waste (strategic planning)
```

### Benefits After 122 Sessions

**Measured Improvements**:
- ✅ Session handoffs: 100% actionable (vs 60% vague before)
- ✅ False "done" claims: ~0% (vs 30% before)
- ✅ RTFM compliance: Will improve with enforcement
- ✅ Context efficiency: Strategic restarts when needed
- ✅ User testing: Clear distinction from automated tests

**Time Savings**:
- 30 min/session on "what did previous session mean?"
- 1-2 hours/session on re-implementing after false "done"
- 2-3 hours/session on API debugging after guessing wrong
- **Total**: ~4-5 hours/session savings = 80%+ efficiency gain!

---

## Project Setup Foundation

### 1. Context Management System

**Create these files** (enables Claude to maintain memory across sessions):

```
your-project/
├── CLAUDE.md                           # Pointer file (same on all branches)
├── docs/
│   ├── CLAUDE_CONTEXT.md               # Session state & current focus
│   └── KNOWLEDGE_BASE_INDEX.md         # Central catalog
```

**CLAUDE.md** - The pointer (minimal, never changes):
```markdown
# Claude Agent Pointer

## Memory Location
- **Primary branch**: `claude-coder-work`
- **Context file**: `docs/CLAUDE_CONTEXT.md`
- **Knowledge base**: `docs/KNOWLEDGE_BASE_INDEX.md`

## Critical Lessons
- Always push immediately after commit
- Tests block at every gate
- Review BEFORE PR
- Trunk-based development

[Include your mandatory workflow section here]
```

**CLAUDE_CONTEXT.md** - Session state (updates frequently):
```markdown
# Claude Context

## Current Session State
**Session**: [Number]
**Status**: [In Progress/Complete]
**Focus**: [What we're working on]

## Recent Achievements
- [List recent work]

## Next Steps
- [What's next]

## Critical Reminders
- [Project-specific reminders]
```

**KNOWLEDGE_BASE_INDEX.md** - Central catalog (grows over time):
```markdown
# Knowledge Base Index

## Documentation
- [List all important docs with purpose]

## Decisions
- [Link to ADRs]

## References
- [External resources]
```

### 2. Git Workflow (Trunk-Based + Smart Sync)

**Branch Model** - Two branches only:
```bash
main                    # Production-ready (protected)
claude-coder-work       # All development happens here
```

**Why no feature branches?**
- Solo work: Feature branches create cleanup mess
- Trunk-based: Fast iteration, no merge hell
- Exception: Only for external contributors

**Simple Sync** - Trunk-based workflow (Session 43 best practice):
```bash
# After PR merge (NO SQUASH):
git pull origin main
git push origin claude-coder-work
```

**Merge Strategy for Trunk-Based**:
```bash
# CORRECT (trunk-based, no feature branches):
gh pr merge --merge      # Preserve commit history

# WRONG (feature branch pattern):
gh pr merge --squash     # Loses granular history, causes divergence
```

**Why no squash?**:
- ✅ Preserves granular commit history (already clean!)
- ✅ No divergence after merge (simple `git pull`)
- ✅ No force pushes or complex sync logic
- ✅ Easier debugging with `git bisect`
- ✅ True trunk-based development

**When to squash?**:
- Only for messy feature branches from external contributors
- NOT for your trunk branch (`claude-coder-work`)

**Setup**:
```bash
# One-time setup
git config pull.rebase true

# Create work branch
git checkout -b claude-coder-work
```

### 3. Makefile Commands (Quality Automation)

**Create `Makefile`** with these core commands:

```makefile
.PHONY: setup test review pr sync status

setup:
    # Initial setup (venv, deps, hooks, git config)
    python3 -m venv venv
    ./venv/bin/pip install -r requirements.txt
    ./venv/bin/pre-commit install
    git config pull.rebase true

test:
    # Full test suite with coverage
    pytest tests/unit/ --cov=src --cov-fail-under=80

test-quick:
    # Quick tests for TDD (<30s)
    pytest tests/unit/ -q --tb=line --timeout=30

review:
    # Full code review (REQUIRED before PR)
    # 1. Tests + coverage
    # 2. Type checking (mypy)
    # 3. Complexity (radon)
    # 4. Security (bandit, safety)
    # 5. Lint (black, isort, flake8)

pr:
    # Create PR (requires review pass)
    make review
    git push -u origin $(git branch --show-current)
    gh pr create --base main --fill

sync:
    # Simple sync after merge (trunk-based = no squash)
    git pull origin main
    git push origin $(git branch --show-current)

status:
    # Show workflow status
    git status --short
    git log --oneline -5
```

**Why Makefile?**
- Single entry point for all workflows
- Consistent commands across projects
- Easy to document and remember

---

## TDD Workflow & Quality Gates

### Ground Truth TDD (Not Traditional TDD)

**The Problem** - Traditional TDD doesn't work for calculations:
```python
# ❌ Traditional TDD
def test_calculate_profit():
    result = calculate_profit(positions)
    assert result == ???  # What should it be?
```

**The Solution** - Ground Truth TDD:
```python
# ✅ Ground Truth TDD
def test_calculate_profit_matches_source_system():
    ground_truth = load_ground_truth("baseline.json")
    result = calculate_profit(positions)
    expected = ground_truth["profit"]
    assert abs(result - expected) < 0.01  # Catches real bugs!
```

**Process**:
1. **Collect**: Get known-correct values from source system
2. **Store**: Save in `tests/fixtures/ground_truth/`
3. **Test**: Write test comparing your calculation to ground truth
4. **Implement**: Make test pass
5. **Validate**: Verify against live system

**When to use**:
- Financial calculations (P/L, margin, etc.)
- Data transformations with known outputs
- API integrations with documented responses
- Any calculation where "correct answer" exists

### 4-Tier Quality Gates - Progressive Validation (Sessions 120, 171-172)

**Design Philosophy**: Fast iteration > Perfect code during development

**Four Progressive Tiers**:
- **TIER 0 (/commit-wip)** - Instant checkpoint (skips all hooks)
- **TIER 1 (/commit)** - Fast TDD loop (tests only, 30s)
- **TIER 2 (/commit-review)** - Human review ready (format + lint + tests + AI review, 3-4min)
- **TIER 3 (/ship)** - Production validation (full CI, 5-10min)

#### TIER 0: Emergency Checkpoints (Instant)

**Slash Command**: `/commit-wip`

```bash
/commit-wip  # Fast checkpoint, skips ALL hooks
             # → Use for end-of-day saves
             # → Use when tests intentionally failing (mid-work)
             # → Clean up before /ship
```

**Use when**:
- End-of-day save (mid-implementation)
- Tests intentionally failing (red phase of TDD)
- Emergency context-saving (low context, need to stop)
- Always clean up before `/ship`

**Why**: Sometimes you need to save state without passing gates

#### TIER 1: Fast TDD Loop (Automatic, <30s)

**Slash Command**: `/commit`

**Tests-Only Strategy** (Session 120 optimization - 40-50% faster):

```yaml
# .pre-commit-config.yaml
repos:
  - repo: local
    hooks:
      - id: pytest-quick
        name: Run quick unit tests (no coverage)
        entry: ./venv/bin/pytest tests/unit/ -q --tb=line --timeout=30
        language: system
        pass_filenames: false
        always_run: true

      - id: block-main-commits
        name: Block commits to main branch
        entry: bash -c 'if [ "$(git branch --show-current)" = "main" ]; then echo "❌ Direct commits to main not allowed"; exit 1; fi'
        language: system
        pass_filenames: false
        always_run: true
```

**Checks**:
- ✅ Quick unit tests (<30s, **no coverage** for speed)
- ✅ Block commits to main

**Removed** (moved to TIER 2/3):
- ❌ black, isort (→ /commit-review)
- ❌ flake8 (→ /commit-review)
- ❌ File checks (→ /ship)

**Why**: Fastest possible TDD feedback loop - tests prove correctness, format/lint later

**Use when**: Rapid TDD iteration (50-100 commits/day)

#### TIER 2: Human Review Ready (Manual, 3-4min)

**Slash Command**: `/commit-review`

```bash
make review
```

**Runs on**: Before human code review (via `/commit-review` slash command)

**Checks**:
1. Auto-format code (black + isort)
2. Lint checks (flake8)
3. Full test suite (with coverage)
4. **AI code review** (code-reviewer agent) - Sessions 171-172
   - Analyzes Python changes for trading accuracy, critical bugs, security
   - Saves review to `docs/code-reviews/SESSION_XXX_COMMIT_YYYYYYY.md`
   - Three verdicts: **APPROVE** / **REQUEST_CHANGES** / **BLOCK**
   - **MANDATORY** (not optional) - auto-runs on Python file changes
   - Adds ~60s to workflow (3-4 min total)
5. **Auto-issue creation** (code-reviewer findings) - **NEW Session 180**
   - If verdict = BLOCK or REQUEST_CHANGES → auto-creates tracking issue
   - Issue file: `docs/issues/ISSUE-XXX-code-review-<commit-hash>.md`
   - Prevents review findings from being forgotten
   - Links to commit hash and full review file
   - **Why**: Session 179 found 2 critical bugs, but fix commit never reviewed (debt!)
6. **Reviewed commit tracking** (git tags) - **NEW Session 180**
   - Tags commit as `reviewed-<hash>` automatically after review
   - Tag message: "Session XXX: VERDICT, created ISSUE-XXX (if any)"
   - Query reviewed commits: `git tag -l "reviewed-*"`
   - Find unreviewed commits: See ISSUE-025 for backlog strategy
   - **Why**: Track which commits were reviewed, prevent review debt accumulation

**AI Code Review Example** (Session 172 - caught 3 blocking bugs):
```
VERDICT: BLOCK

Critical Issues Found: 3

1. INPUT VALIDATION MISSING (order_commands.py:145)
   - User input not validated before int() conversion
   - Crash on invalid input: ValueError
   - Fix: Add try/except with user-friendly error

2. PRICE VALIDATION MISSING (order_commands.py:198)
   - Decimal conversion not validated
   - Crash on invalid price input
   - Fix: Validate Decimal conversion with error handling

3. GCD EDGE CASE (order_commands.py:287)
   - gcd(0, 0) crashes if all quantities are zero
   - Fix: Filter zero quantities before GCD calculation
```

**Why**: AI review catches bugs tests don't (invalid inputs, edge cases, wrong formulas)

**Use when**: Ready to share code with team, before creating PR

**Session 172 Lesson**: Skipping AI review = 3 blocking bugs shipped. Now mandatory.

**Session 180 Lesson**: Review findings get lost without issue tracking. Auto-issue creation prevents review debt from accumulating (17 unreviewed commits found!)

**Code Review Debt Prevention** (Session 180):
- Auto-issue creation ensures all findings are tracked
- Git tags enable queries: "Which commits were reviewed?"
- Systematic backlog clearing (newest→oldest, fresher memory)
- Target: 100% of code commits reviewed

#### TIER 3: Production Validation (Manual, 5-10min)

**Slash Command**: `/ship`

```bash
make ship
```

**Runs on**: Before merging to main (via `/ship` slash command)

**Checks**:
1. All TIER 2 checks (format + lint + tests + AI review)
2. Security scans (bandit, safety, pip-audit)
3. Complexity analysis (radon - must be grade B+)
4. Type checking (mypy)
5. Mutation testing reminder (optional but recommended)
6. Full CI/CD pipeline (if using Dagger/GitHub Actions)

**Why**: Final validation before production

**Use when**: Ready to deploy to production, creating release

---

### Makefile Targets (Sessions 120, 171-172)

**Add these targets** to support the 4-tier workflow:

```makefile
.PHONY: test test-quick review ship

test:
    # Full test suite with coverage (for CI/CD)
    pytest tests/unit/ --cov=src --cov-fail-under=55

test-quick:
    # Quick tests for TDD - NO coverage (20-30% faster)
    pytest tests/unit/ -q --tb=line --timeout=30

review:
    # TIER 2: Code review preparation
    black src/ tests/
    isort src/ tests/
    flake8 src/ tests/
    pytest tests/unit/ --cov=src --cov-fail-under=55
    @echo "✅ Code ready for review!"

ship:
    # TIER 3: Full production validation
    $(MAKE) review
    bandit -r src/
    safety check
    radon cc src/ -a -nb
    @echo "⚠️  Reminder: Run mutation tests if time permits"
    @echo "✅ Ready to ship!"
```

### TDD Workflow (Red-Green-Refactor) - Session 120 Optimized

**Optimized 3-minute cycle** (40% faster than traditional):

```bash
# 1. RED - Write failing test (30s)
# tests/unit/test_new_feature.py
def test_new_calculation():
    result = calculate_new_feature(input_data)
    assert result == expected  # FAILS

# 2. Run test
make test-quick  # or: pytest tests/unit/test_new_feature.py -x
# ❌ FAILED - as expected

# 3. GREEN - Make it pass (1.5 min)
# src/new_feature.py
def calculate_new_feature(data):
    return correct_implementation()

make test-quick
# ✅ PASSED

# 4. COMMIT - Fast feedback (30s) - NEW optimized workflow
/commit  # Uses slash command (auto-formats commit message)
# → Pre-commit hook runs TESTS ONLY (<30s)
# → Auto-generates commit message from git diff
# → Pushes to remote automatically
# ✅ All checks pass

# 5. REFACTOR - Clean up (optional, do later)
# Format/lint happens at code review time (/commit-review)
```

**When to format/lint**:
- **During TDD**: Skip (use `/commit` for speed)
- **Before code review**: Run `/commit-review` (formats + lints)
- **Before production**: Run `/ship` (full validation)

**Repeat**: 50-100 times per day (now 40% faster!)

---

## Architecture Principles

### Clean Architecture (Hexagonal/Ports & Adapters)

**Structure**:
```
your-project/
├── src/
│   ├── domain/              # Core business logic (pure, no deps)
│   │   ├── models.py        # Domain entities
│   │   ├── services.py      # Business logic
│   │   └── protocols.py     # Interfaces (ports)
│   │
│   ├── adapters/            # External integrations
│   │   ├── database/        # DB adapter
│   │   ├── api/             # External APIs
│   │   └── cache/           # Cache layer
│   │
│   └── ui/                  # User interfaces
│       ├── cli/             # Command-line
│       ├── web/             # Web UI
│       └── api/             # REST API
│
└── tests/
    ├── unit/                # Domain logic (fast)
    ├── integration/         # Adapters (slower)
    └── e2e/                 # Full workflows (slowest)
```

**Key Principles**:
1. **Domain is pure** - No external dependencies
2. **Adapters wrap externals** - DB, APIs, files
3. **UI uses domain** - Never bypass domain logic
4. **Protocol interfaces** - Easy to mock and swap

**Example Protocol**:
```python
from typing import Protocol

class IDataSource(Protocol):
    """Interface for data sources"""
    async def fetch_data(self, query: str) -> List[dict]: ...
    async def save_data(self, data: dict) -> bool: ...

# Multiple implementations
class PostgresDataSource: ...  # Production
class MockDataSource: ...      # Testing
class FileDataSource: ...      # Backup
```

### Technology-Agnostic Core

**Rule**: Domain should work without ANY external system

```python
# ✅ GOOD - Pure domain logic
@dataclass
class Order:
    id: str
    amount: Decimal
    status: str

    def is_complete(self) -> bool:
        return self.status == "completed"

    def calculate_total(self) -> Decimal:
        return self.amount * (1 + self.tax_rate)

# ❌ BAD - Couples to specific database
class Order:
    def save(self):
        db.session.add(self)  # Couples to SQLAlchemy!
```

**Why**:
- Easy to test (no DB needed)
- Easy to change tech stack
- Easy to add new interfaces

### Progressive Implementation Strategy

**Don't build everything at once** - Start minimal, expand when proven necessary

**Example** - Storage layer progression:
```
Phase 1: In-memory only (1 day)
    ↓ (only if persistence needed)
Phase 2: SQLite cache (2 days)
    ↓ (only if scale/features needed)
Phase 3: PostgreSQL (3 days)
    ↓ (only if distributed needed)
Phase 4: Multi-region PostgreSQL (1 week)
```

**Principle**: "Best code is no code"
- Build minimum viable feature
- Measure performance
- Expand only when proven necessary
- Drop features that aren't used

---

## Development Process

### TodoWrite for Task Tracking

**When to use**:
- Breaking down large features
- Tracking multi-step processes
- Coordinating multiple files
- Session handoffs

**Example**:
```markdown
# Feature: Add PostgreSQL Storage

## Phase 1: Foundation ⏳ IN PROGRESS
- [x] Design schema (3 tables)
- [x] Create migration scripts
- [x] Set up Docker Compose
- [ ] Implement connection pooling
- [ ] Add health checks

## Phase 2: Integration
- [ ] Wire up to domain layer
- [ ] Add caching layer
- [ ] Integration tests
```

**Update frequently**:
```bash
# After each significant step
git add TODO.md
git commit -m "docs: Update TODO with connection pooling complete"
```

### Session Documentation

**Create session summaries** for context preservation:

```markdown
# Session N Summary

**Date**: YYYY-MM-DD
**Duration**: N hours
**Status**: [Complete/In Progress]

## What Was Accomplished
- [List achievements]

## Key Decisions
- [Important choices made]

## Bugs Found & Fixed
- [Issues discovered]

## Next Steps
- [What's next]

## Files Changed
- [Major file changes]

## Metrics
- Tests: N passing (+X new)
- Coverage: N%
- Commits: N
```

**When to create**:
- End of major work session (2+ hours)
- Before PR creation
- When switching focus areas
- Before breaks >1 day

### Proactive Subagent Usage

**If you have AI subagents** (specialized Claude instances):

**ALWAYS use when they can help** - don't wait to be asked:

```python
# Before committing financial code (AUTOMATIC in /commit-review - Session 171-172)
Task(
    subagent_type="code-reviewer",
    description="Review P/L calculations",
    prompt="Review this financial calculation code for accuracy and edge cases"
)

# When writing tests
Task(
    subagent_type="test-automator",
    description="Generate test cases",
    prompt="Generate comprehensive test cases for this feature"
)

# When validating data
Task(
    subagent_type="fact-checker",
    description="Validate calculations",
    prompt="Compare our calculations against source system ground truth"
)
```

**High-value use cases**:
- **Code review before commits** - AUTOMATIC in TIER 2 (/commit-review) for Python files
- Test case generation
- Ground truth validation
- Architecture decisions
- Security reviews
- Performance analysis

**Note**: As of Session 171-172, code-reviewer agent runs automatically in `/commit-review` for Python changes. See "4-Tier Quality Gates" section for details.

### Review-Before-PR Workflow

**NEVER create PR without review passing**:

```bash
# ❌ WRONG - Create PR immediately
git commit -m "Add feature"
git push
gh pr create  # Issues found in PR comments (wastes time)

# ✅ RIGHT - Review first, fix issues, then PR
git commit -m "Add feature"
make review   # Finds 3 issues locally
# Fix issues...
git commit -m "fix: Address review issues"
make review   # All pass ✅
make pr       # Clean PR, fast merge
```

**Why**:
- Catches issues locally (faster feedback)
- Clean PR with no CI failures
- Respect reviewers' time
- Faster merge cycle

---

## Documentation Standards

### ADRs (Architecture Decision Records)

**Create ADRs for significant decisions**:

```markdown
# ADR-NNN: [Decision Title]

**Status**: [Proposed/Accepted/Deprecated/Superseded]
**Date**: YYYY-MM-DD
**Decision Makers**: [Who decided]

## Context
What's the situation and problem?

## Decision
What did we decide?

## Consequences
### Positive
- What benefits?

### Negative
- What costs?

### Mitigations
- How to address negatives?

## Alternatives Considered
What else did we consider and why not?
```

**When to create**:
- Choosing tech stack (PostgreSQL vs SQLite)
- Architectural patterns (Clean Architecture)
- Major refactors (CLI/Terminal split)
- API design (REST vs GraphQL)
- Security decisions (Auth approach)

**Store in**: `docs/architecture/ADR-NNN-short-title.md`

### Session Summaries

**Template** (see earlier section for full template)

**Benefits**:
- Claude can resume work quickly
- Team members understand recent changes
- Historical record of decisions
- Debugging aid (when did we change X?)

### Testing Strategy Documentation

**Create `docs/TESTING_STRATEGY.md`**:

```markdown
# Testing Strategy

## Test Pyramid
- Unit Tests: >80% coverage (fast)
- Integration Tests: Critical paths
- E2E Tests: Key workflows

## Ground Truth Approach
[How you collect/validate ground truth]

## TDD Workflow
[Your red-green-refactor process]

## CI/CD Strategy
[Quality gates and automation]
```

### README for Setup

**Good README** makes onboarding easy:

```markdown
# Project Name

Brief description.

## Quick Start

```bash
# Setup
make setup

# Run tests
make test

# Development
make test-quick  # Fast TDD
make review      # Before PR
make pr          # Create PR
make sync        # After merge
```

## Architecture
See: `docs/architecture/ADR-001-clean-architecture.md`

## Testing
See: `docs/TESTING_STRATEGY.md`

## Contributing
1. Work on `claude-coder-work` branch
2. TDD workflow (red-green-refactor)
3. `make review` before PR
4. `make pr` to create PR
5. `make sync` after merge
```

---

## Issue Tracking System

**Location**: `docs/issues/`
**Index**: `docs/issues/README.md` (auto-generated)
**Update Script**: `scripts/update_issue_index.py`

### Automated Issue Management

All issues follow a standardized metadata structure:

```markdown
# ISSUE-XXX: Title

**Created**: YYYY-MM-DD (Session XXX)
**Completed**: YYYY-MM-DD (Session YYY) [if closed]
**Status**: OPEN | PARTIAL | CLOSED
**Priority**: CRITICAL | HIGH | MEDIUM | LOW
**Category**: Feature | Bug | Infrastructure | Testing | Documentation
**Estimated**: X hours/sessions
**Actual**: Y hours [if completed]
**Related**: ISSUE-###, ISSUE-### [if applicable]
**Blocks**: ISSUE-### [if applicable]
**Blocked By**: ISSUE-### [if applicable]
```

### Auto-Generated README

The issue index (`docs/issues/README.md`) is automatically generated by `scripts/update_issue_index.py`:

**Features**:
- Scans all ISSUE-*.md files in docs/issues/
- Extracts metadata (Status, Priority, Category, etc.)
- Groups issues by priority (CRITICAL → LOW)
- Shows statistics (open/closed/partial counts)
- Categorizes by type (Feature, Bug, Infrastructure, etc.)
- Handles emoji status variations (✅ CLOSED → CLOSED)
- Fast execution (<1 second)

**Workflow Integration**:
- Runs automatically during `/end-session` (Step 5)
- Keeps README current without manual updates
- Zero maintenance overhead

**Manual Update**:
```bash
python scripts/update_issue_index.py
```

**Statistics** (as of Session 174):
- Total: 21 issues
- Closed: 6 (29%)
- Partial: 3 (14%)
- Open: 12 (57%)

---

## Collaboration Style

### Expectations for Claude

**Be proactive**:
- Suggest improvements to code/architecture
- Point out potential issues before they become bugs
- Recommend testing for edge cases
- Question unclear requirements

**Be precise**:
- Use exact technical terms
- Provide specific examples
- Show code snippets, not just descriptions
- Include line numbers when referencing code

**Be pragmatic**:
- Follow "best code is no code"
- Don't over-engineer
- Build minimally, expand when needed
- Measure before optimizing

**Be educational**:
- Explain the "why" behind decisions
- Share best practices
- Document trade-offs
- Help me learn patterns

### When Claude Should Ask

**Always ask when**:
- Requirements are ambiguous
- Multiple valid approaches exist
- Trade-offs between solutions
- Significant time investment (>1 hour)
- Breaking changes to existing code
- Security/safety concerns

**Never assume**:
- Architecture patterns (ask what I prefer)
- Code style (unless .editorconfig/linter defines)
- Testing approach (ask about coverage expectations)
- Deployment environment (ask about constraints)

### Communication Style

**Good** - Clear, concise, actionable:
```
I found 3 issues in the P/L calculation:
1. Missing decimal precision (line 42) - causes rounding errors
2. Not handling negative positions (line 67) - crashes on shorts
3. Timezone assumption (line 91) - breaks for non-US markets

Recommendation: Fix #1 and #2 now (critical), defer #3 (rare case)
```

**Bad** - Vague, unclear impact:
```
There might be some issues with the calculation. It could be better.
Maybe we should refactor it?
```

---

## Appendix: Project-Specific Customizations

### For Financial/Trading Projects

**Additional practices** from Contrarian project:
- Collect ground truth from broker APIs
- Validate all calculations against broker UI
- Use `Decimal` for all monetary values (never `float`)
- Document data sources in comments
- Create data validation scripts

**Example validation**:
```python
def test_profit_matches_broker():
    """Validate our P/L calculation against TastyTrade"""
    ground_truth = load_csv("broker_export.csv")
    our_result = calculate_profit(positions)
    broker_result = ground_truth["total_pl"]

    # Financial calculations must be exact (no tolerance)
    assert our_result == broker_result, (
        f"P/L mismatch: expected {broker_result}, got {our_result}"
    )
```

### For Web Applications

**Additional practices**:
- Frontend testing (unit + integration + E2E)
- API contract testing (OpenAPI specs)
- Performance budgets (Lighthouse scores)
- Accessibility requirements (WCAG AA)

### For ML/Data Science Projects

**Additional practices**:
- Experiment tracking (MLflow, Weights & Biases)
- Data versioning (DVC)
- Model versioning (registry)
- Reproducibility (seed everything)
- Validation sets (never test on training data)

### For CLI Tools

**Additional practices**:
- Help text for all commands
- Shell completion scripts
- Exit codes (0 = success, >0 = error)
- Pipeline-friendly output (JSON/CSV)
- Progress indicators for long operations

---

## Example Initial Prompt

Use this template when starting a new project with Claude:

```markdown
# New Project: [Name]

## Project Overview
- **Purpose**: [What does it do?]
- **Tech Stack**: [Languages, frameworks]
- **Domain**: [Business area]
- **Duration**: [Expected timeline]
- **Team**: [Solo / N people]

## Development Approach
I want to use the proven workflow from the Contrarian trading project:

1. **TDD**: Ground Truth TDD with 3-tier quality gates
2. **Architecture**: Clean Architecture (domain-adapter-UI)
3. **Git**: Trunk-based development with smart sync
4. **Process**: TodoWrite + Session summaries + ADRs
5. **Quality**: make setup/test/review/pr/sync workflow

Full reference: docs/CLAUDE_PROJECT_INIT_TEMPLATE.md

## Initial Setup Tasks
1. Create context management files (CLAUDE.md, CLAUDE_CONTEXT.md)
2. Set up git workflow (claude-coder-work branch, Makefile)
3. Configure quality gates (pre-commit, CI/CD)
4. Create initial architecture (domain/adapters/ui)
5. Set up testing infrastructure (pytest, fixtures)

## First Feature
[Describe the first feature to implement]

## Ground Truth Sources
[Where to get known-correct values for testing]

## Success Criteria
[How do we know it's working?]

## Questions
[Any clarifications needed before starting?]
```

---

## Summary Checklist

**Before starting development with Claude**, verify:

- [ ] Context management system (CLAUDE.md, CLAUDE_CONTEXT.md, KNOWLEDGE_BASE_INDEX.md)
- [ ] Git workflow (two branches, smart sync, Makefile)
- [ ] Quality gates (pre-commit hooks, make review, CI/CD)
- [ ] Testing strategy (Ground Truth TDD, 3-tier pyramid)
- [ ] Architecture pattern (Clean Architecture or chosen pattern)
- [ ] Documentation approach (ADRs, session summaries)
- [ ] Collaboration expectations (when Claude should ask, communication style)

**During development**, maintain:

- [ ] Update CLAUDE_CONTEXT.md regularly
- [ ] Create ADRs for significant decisions
- [ ] Write session summaries for long sessions
- [ ] Use TodoWrite for complex tasks
- [ ] Run `make review` before every PR
- [ ] Push immediately after commit (team visibility)
- [ ] Use `make sync` after PR merge (simple pull for trunk-based workflow)

---

## Lessons from 174+ Sessions (Contrarian Project)

**Critical insights**:

1. **Ground Truth TDD prevents bugs** - Traditional TDD missed 3 critical bugs that ground truth caught
2. **Merge commits eliminate conflicts** - No squash for trunk-based = simple `git pull` after merge (Session 43)
3. **Fast TDD feedback is essential** - <30s pre-commit enables 50-100 commits/day
4. **Start minimal, expand when needed** - 3 tables > 7 tables (simplicity wins)
5. **Review before PR saves time** - Fix issues locally, not in PR comments
6. **Document decisions (ADRs)** - Future you will thank present you
7. **Session summaries preserve context** - Claude resumes work instantly
8. **Trunk-based > feature branches** - No merge mess for solo work
9. **Best code is no code** - Drop unused features, build minimum viable

**Biggest wins**:
- 679 tests passing, 0 false positives (Ground Truth TDD works!)
- Zero merge conflicts over 174+ sessions (merge commits + trunk-based works!)
- <6s test runtime enables rapid TDD (fast feedback works!)
- 100% P/L calculation accuracy vs broker (validation works!)
- Automated issue tracking (21 issues, zero manual maintenance!)
- Simple workflow after Session 43 (no squash = no divergence!)

---

## Version History

- **1.6** (2025-10-20): Session 180 Code Review Workflow Improvements
  - NEW: Auto-issue creation from code review findings (Step 4.6)
    - If verdict = BLOCK or REQUEST_CHANGES → auto-creates ISSUE-XXX
    - Issue file: docs/issues/ISSUE-XXX-code-review-<hash>.md
    - Prevents review findings from being forgotten (Session 179: found 17 unreviewed commits!)
  - NEW: Reviewed commit tracking with git tags (Step 4.7)
    - Tags commits as `reviewed-<hash>` automatically
    - Tag message includes: Session number, verdict, issue created (if any)
    - Query: `git tag -l "reviewed-*"` to find reviewed commits
    - Enables systematic code review debt clearing
  - NEW: ISSUE-025 and ISSUE-026 created
    - ISSUE-025: Code review debt backlog (17 unreviewed commits, Sessions 172-180)
    - ISSUE-026: Remove obsolete/dead code (cleanup strategy)
  - Impact: Zero review findings lost, 100% commit review tracking, prevents review debt
- **1.5** (2025-10-19): Sessions 171-174 AI Code Review + Issue Tracking
  - NEW: 4-tier quality gates (added TIER 0: /commit-wip for emergency checkpoints)
  - NEW: AI code review integration in TIER 2 (Session 171-172)
    - code-reviewer agent runs automatically on Python changes
    - Saves reviews to docs/code-reviews/SESSION_XXX_COMMIT_YYYYYYY.md
    - Three verdicts: APPROVE / REQUEST_CHANGES / BLOCK
    - MANDATORY (not optional) - Session 172 lesson: caught 3 blocking bugs
    - Adds ~60s to workflow (TIER 2: 2-3min → 3-4min)
  - NEW: Automated issue tracking (Session 174)
    - scripts/update_issue_index.py (auto-generate docs/issues/README.md)
    - Standardized metadata across all 21 issue files
    - Integration into /end-session workflow (Step 5)
    - Statistics: 6 closed (29%), 3 partial (14%), 12 open (57%)
  - Impact: AI review prevents bugs tests miss, zero issue tracking overhead
- **1.4** (2025-10-19): Session 169 Workflow Simplification
  - Simplified session commands: 4 → 2 (deleted /emergency-checkpoint, /resume-emergency)
  - Auto-detection: /end-session detects emergency state (uncommitted code, low context)
  - Auto-resume: /start-session detects WIP sessions (shows blocker automatically)
  - Unified naming: Always SESSION_XX_COMPLETE.md (metadata flag for emergency)
  - Impact: 50% command reduction, 33% line reduction (555 lines), zero user decision fatigue
- **1.3** (2025-01-16): Session 122 Foundation Protocols + Agent Updates
  - NEW: Session State Transfer Protocol (zero-degradation handoffs)
  - NEW: Definition of Done (clear completion criteria)
  - NEW: RTFM Enforcement (API documentation mandatory)
  - Updated: code-reviewer agent (RTFM + ground truth validation)
  - NEW: api-integration-specialist agent (API-first thinking)
  - Updated: trading-specialist agent (position risk analysis)
  - Updated: python-pro agent (futures/options expertise)
  - Impact: 80%+ efficiency gain per session (4-5 hours saved)
- **1.2** (2025-01-16): Session 120 CI/CD optimizations - Progressive quality gates
  - Tests-only pre-commit hook (40-50% faster commits)
  - 3-tier workflow: /commit → /commit-review → /ship (later expanded to 4-tier in v1.5)
  - New Makefile targets: test-quick, review, ship
  - Session workflow optimizations (parallel reads, auto-infer)
  - Daily savings: 9-10 min/day, 3-4 hours/month
- **1.1** (2025-10-10): Updated merge strategy - no squash for trunk-based (Session 43)
- **1.0** (2025-10-08): Initial template from Contrarian project (21 sessions)

---

**This template is tech-stack agnostic** - Adapt for Python, JavaScript, Go, Rust, etc.

**This template is domain agnostic** - Works for web apps, CLI tools, data pipelines, APIs, etc.

**This template is proven** - Battle-tested over 174+ sessions, 2000+ commits, multiple complex features.

Use it as a starting point, customize for your needs, and improve as you go.
