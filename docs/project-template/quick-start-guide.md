# Quick Start Guide - New Project with Claude

**One-page reference** for starting development with Claude using proven TDD workflow.

**Full details**: See `CLAUDE_PROJECT_INIT_TEMPLATE.md`

---

## 5-Minute Setup

```bash
# 1. Create project structure
mkdir your-project && cd your-project
git init
git checkout -b claude-coder-work

# 2. Create context files
touch CLAUDE.md docs/CLAUDE_CONTEXT.md docs/KNOWLEDGE_BASE_INDEX.md

# 3. Create Makefile (copy from template)
touch Makefile

# 4. Set up Python
python3 -m venv venv
./venv/bin/pip install pytest black isort flake8 pre-commit

# 5. Configure git
git config pull.rebase true

# 6. Install hooks
./venv/bin/pre-commit install

# 7. Initial commit
git add .
git commit -m "Initial setup"
git push -u origin claude-coder-work
```

---

## Essential Files

```
your-project/
├── CLAUDE.md                    # Memory pointer (keep minimal)
├── docs/
│   ├── CLAUDE_CONTEXT.md        # Session state
│   └── KNOWLEDGE_BASE_INDEX.md  # Central catalog
├── Makefile                     # Workflow automation
├── .pre-commit-config.yaml      # Git hooks
├── .github/workflows/ci.yml     # CI/CD
└── src/
    ├── domain/                  # Core logic (pure)
    ├── adapters/                # External systems
    └── ui/                      # Interfaces
```

---

## Daily Workflow

### TDD Cycle (5 min)

```bash
# 1. RED - Write failing test
vim tests/unit/test_feature.py

# 2. GREEN - Make it pass
vim src/feature.py

# 3. REFACTOR - Clean up

# 4. COMMIT
git add .
git commit -m "feat: Add feature"
# Pre-commit runs (<30s)

# 5. PUSH
git push
# CI runs in background
```

### Before Creating PR

```bash
# Full review (1-2 min)
make review

# Fix any issues found
# [make changes]

# Commit fixes
git commit -m "fix: Address review issues"

# Create PR
make pr
```

### After PR Merges

```bash
# Smart sync (automatic)
make sync

# Handles both:
# - Normal work (rebase)
# - Squash merge (reset)
```

---

## Makefile Commands

```bash
make setup         # Initial setup (one time)
make test          # Full test suite
make test-quick    # Quick tests (<30s)
make lint          # Check code quality
make format        # Auto-format code
make review        # Full review (required before PR)
make pr            # Create pull request
make sync          # Sync after merge
make status        # Show git status
```

---

## Testing Strategy

### Ground Truth TDD

```python
# ✅ GOOD - Test against known values
def test_calculation_matches_source():
    ground_truth = load_ground_truth("baseline.json")
    result = calculate(input_data)
    expected = ground_truth["expected_result"]
    assert result == expected

# ❌ BAD - No oracle
def test_calculation():
    result = calculate(input_data)
    assert result == ???  # What should it be?
```

### 3-Tier Quality Gates

1. **Pre-commit** (<30s): Format + Lint + Quick tests
2. **Pre-push** (1-2min): Full tests + Security + Complexity
3. **CI/CD** (2-5min): All checks + Integration tests

---

## Architecture Pattern

### Clean Architecture

```
UI Layer        → Calls
  ↓
Domain Layer    → Pure business logic (no dependencies)
  ↓
Adapter Layer   → Wraps external systems
```

**Example**:
```python
# Domain (pure, no dependencies)
@dataclass
class Order:
    id: str
    amount: Decimal

    def calculate_total(self) -> Decimal:
        return self.amount * 1.1

# Adapter (wraps database)
class PostgresOrderRepo:
    def save(self, order: Order):
        # DB-specific code
        pass

# UI (uses domain)
def create_order_endpoint(data: dict):
    order = Order(**data)
    total = order.calculate_total()  # Domain logic
    repo.save(order)                  # Adapter
    return {"total": total}
```

---

## Documentation

### When to Create

**ADR** (Architecture Decision Record):
- Choosing tech stack
- Major refactors
- API design
- Security decisions

**Session Summary**:
- After 2+ hour work session
- Before PR creation
- When switching focus

**TodoWrite**:
- Breaking down large features
- Tracking multi-step work
- Session handoffs

---

## Git Branches

**Two branches only**:
```
main                 # Production (protected, requires PR)
claude-coder-work    # All development
```

**No feature branches** for solo work (creates merge mess)

**Smart sync** handles squash merges automatically:
- <10 commits ahead → Rebase
- >10 commits ahead → Reset (after squash)

---

## Coverage Targets

| Module | Target | Why |
|--------|--------|-----|
| Domain | >90% | Core logic is critical |
| Adapters | >70% | Focus on transformations |
| UI | >60% | Focus on workflows |
| **Overall** | **>80%** | **Minimum for PR merge** |

---

## Initial Prompt Template

Copy this when starting a new project:

```markdown
# New Project: [Name]

## Tech Stack
- Language: [Python/JavaScript/etc]
- Framework: [FastAPI/React/etc]
- Database: [PostgreSQL/etc]

## Workflow
Use proven TDD workflow from Contrarian project:
- Ground Truth TDD with 3-tier quality gates
- Clean Architecture (domain/adapters/ui)
- Trunk-based git with smart sync
- See: docs/CLAUDE_PROJECT_INIT_TEMPLATE.md

## First Task
[What to build first]

## Ground Truth
[Where to get known-correct values]

## Questions
[Any clarifications?]
```

---

## Common Commands

```bash
# Setup new project
make setup

# TDD workflow
make test-quick        # Fast feedback (<30s)
make test              # Full suite

# Before PR
make review            # Required! Catches issues locally

# Create PR
make pr                # Runs review first, then creates PR

# After merge
make sync              # Smart sync (auto-detects squash)

# Check status
make status
```

---

## Critical Reminders

### DO
- Run `make review` before every PR
- Push immediately after commit
- Update CLAUDE_CONTEXT.md regularly
- Create ADRs for big decisions
- Use Ground Truth TDD for calculations
- Use `make sync` after PR merge

### DON'T
- Create feature branches (solo work)
- Skip review before PR
- Commit directly to main
- Use `float` for money (use `Decimal`)
- Assume requirements (ask when unclear)
- Optimize before measuring

---

## Troubleshooting

### Tests fail in CI but pass locally
```bash
# Ensure same environment
docker-compose up -d  # If using Docker
make test             # Should match CI
```

### Merge conflicts after PR
```bash
# Use smart sync
make sync
# Automatically handles squash merges
```

### Pre-commit hook fails
```bash
# Auto-fix formatting
make format

# Run quick tests
make test-quick

# Commit again
git commit -m "..."
```

### Coverage below threshold
```bash
# Check what's not covered
make test
open htmlcov/index.html

# Add tests for uncovered code
```

---

## Resources

- **Full Template**: `docs/CLAUDE_PROJECT_INIT_TEMPLATE.md`
- **Testing Strategy**: `docs/TESTING_STRATEGY.md`
- **Quality Gates**: `docs/workflow/quality-gates.md`
- **Git Workflow**: `docs/GIT_WORKFLOW_FIX.md`
- **Architecture**: `docs/architecture/ADR-001-clean-architecture.md`

---

## Success Metrics (From Contrarian Project)

After 21 sessions using this workflow:
- ✅ 245 tests passing, 0 false positives
- ✅ 100% calculation accuracy vs source system
- ✅ Zero merge conflicts
- ✅ <6s test runtime (enables rapid TDD)
- ✅ 86% overall code coverage
- ✅ 800+ commits, clean git history

**This workflow works.** Use it, adapt it, improve it.

---

**Last Updated**: 2025-10-08
**Version**: 1.0
**Source**: Contrarian Trading Portfolio System
