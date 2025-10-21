# Commit Review - Code Review Ready Commit (TIER 2)

Commit code changes with **full quality validation** (format + lint + tests with coverage) to prepare for code review.

**Purpose**: Auto-format, lint, test with coverage, then commit - ensures code is review-ready

**Use When**: Committing code that's ready for team review (before /ship)

**Progressive Quality Gates**:
- **TIER 0**: `/commit-wip` - No validation (checkpoint, instant)
- **TIER 1**: `/commit` - Tests only (fast iteration, 30s)
- **TIER 2**: `/commit-review` - Format + lint + coverage + code review (human review ready, 3-4 min) ⬅ **YOU ARE HERE**
- **TIER 3**: `/ship` - Full CI + security + PR creation (production ready, 5-10 min)

---

## 🔄 Commit Review Workflow

Follow these steps in order:

### Step 1: Pre-Commit Analysis

**Check current state**:

```bash
# Check branch
git branch --show-current
# Should be: {{CONTEXT_BRANCH}}

# Check status
git status --short

# Check diff (staged + unstaged)
git diff HEAD
```

**Verify**:
- ✅ On `{{CONTEXT_BRANCH}}` branch
- ✅ Has changes to commit
- ✅ Not on `{{MAIN_BRANCH}}` (protected)

**If on {{MAIN_BRANCH}}**:
```
❌ Cannot commit to {{MAIN_BRANCH}}!

{{MAIN_BRANCH}} branch is protected. Please:
1. Switch to {{CONTEXT_BRANCH}}: git checkout {{CONTEXT_BRANCH}}
2. Try /commit-review again
```

**If no changes**:
```
ℹ️ No changes to commit

Working tree is clean. Make some changes first!
```

---

### Step 2: Analyze Changes

**Read the diff and understand**:

Run in parallel:
```bash
# Get staged + unstaged changes
git diff HEAD

# Get list of changed files
git status --short

# Get recent commit messages (for style consistency)
git log --oneline -10
```

**Analyze**:
1. **What changed?** - Read the diff to understand the changes
2. **What type?** - Determine conventional commit type:
   - `feat:` - New feature or enhancement
   - `fix:` - Bug fix
   - `refactor:` - Code refactoring (no behavior change)
   - `test:` - Test additions/changes
   - `docs:` - Documentation only
   - `perf:` - Performance improvement
   - `style:` - Formatting/style changes
   - `chore:` - Build/tooling changes
3. **Scope** - What area? (e.g., `cli`, `fifo`, `adapter`, `cache`)
4. **Summary** - What's the user impact? (1 line, <50 chars)

---

### Step 3: Generate Commit Message

**Format (Conventional Commits)**:
```
<type>(<scope>): <summary>

<body>

<footer>
```

**Rules**:
1. **Summary** - Imperative mood, lowercase, no period, <50 chars
   - ✅ "fix sign handling in FIFO calculator"
   - ❌ "Fixed sign handling." (past tense + period)

2. **Body** (optional) - Explain "why" not "what", wrap at 72 chars
   - Use if changes are complex or need context
   - Bullet points OK

3. **Footer** (always include Claude Code attribution):
   ```
   🤖 Generated with [Claude Code](https://claude.com/claude-code)

   Co-Authored-By: Claude <noreply@anthropic.com>
   ```

**Match project style**:
- Look at recent commits (from Step 2)
- Use same conventions (scope names, tone, length)

---

### Step 4: Run Quality Checks (TIER 2)

**IMPORTANT**: This is what makes `/commit-review` different from `/commit`

**Run formatting + lint + tests with coverage**:
```bash
# Run make review (or equivalent)
make review
```

**What `make review` does**:
1. **Auto-format** - Black + isort (fixes formatting automatically)
2. **Lint** - Flake8 (checks code quality)
3. **Tests with coverage** - pytest with coverage report

---

### Step 4.5: Automated Code Review (MANDATORY - Session 171)

**CRITICAL**: This step is **AUTOMATIC and MANDATORY** for Python changes

**When this runs**:
- ✅ **ALWAYS** if Python code changed (src/*.py or tests/*.py)
- ✅ **BLOCKING** - Cannot proceed to commit without review
- ❌ Skip if only docs changed (docs/*.md)
- ❌ Skip if only config changed (*.yaml, *.toml)

**Automatic execution**:
```bash
# Step 4.5 runs AUTOMATICALLY after Step 4 (make review)
# You MUST follow this workflow:

# 1. Check if Python files changed
PYTHON_FILES=$(git diff --name-only HEAD | grep "\.py$")

# 2. If Python files found, AUTOMATICALLY launch code-reviewer
if [ -n "$PYTHON_FILES" ]; then
    echo "🔍 Python changes detected - launching code-reviewer agent (mandatory)..."
    # Launch Task tool with code-reviewer agent
    # THIS IS NOT OPTIONAL - DO NOT SKIP THIS STEP
fi

# 3. Wait for review verdict before proceeding
# 4. If BLOCK verdict: STOP - do not commit
# 5. If REQUEST_CHANGES verdict: Ask user if they want to fix or proceed
# 6. If APPROVE verdict: Proceed to Step 5
```

**WHY THIS IS MANDATORY**:
- Tests don't catch everything (e.g., invalid user inputs)
- Fresh eyes spot edge cases humans miss
- Financial software requires extra scrutiny
- Session 172 lesson: Skipping review = 3 blocking bugs shipped

**Agent task**:
```
Review the following changes for commit readiness:

**Changed files**:
[List changed Python files]

**Changes**:
[Show git diff for Python files only]

**Focus areas**:
1. Trading accuracy (financial calculations)
2. Critical bugs (will cause failures)
3. Security issues (credentials, data exposure)
4. Test coverage (are new code paths tested?)
5. Ground truth validation (financial calcs verified?)

**Output format**:
- CRITICAL issues (block commit if found)
- MAJOR issues (fix recommended, don't block)
- MINOR issues (document for future)
- APPROVE / REQUEST_CHANGES / BLOCK

Save detailed review to: docs/code-reviews/SESSION_{{SESSION_NUMBER}}_COMMIT_{{COMMIT_SHORT_HASH}}.md
Return summary only (critical issues + verdict).
```

**Expected output**:
```
🤖 Code Review Complete

Files reviewed: 3 Python files
Review time: ~30-60 seconds

CRITICAL: None ✅
MAJOR: 1 issue found
  - src/domain/fifo.py:42 - Missing error handling for empty lot list

MINOR: 2 issues found
  - Variable naming: 'tmp_val' should be 'temporary_value'
  - Missing docstring for _helper_function()

VERDICT: REQUEST_CHANGES (1 major issue)

Full review: docs/code-reviews/SESSION_171_COMMIT_abc1234.md

Options:
1. Fix major issue and re-run /commit-review (recommended)
2. Proceed with commit (major issue documented, fix later)
3. Cancel commit (review findings, ask for help)

What would you like to do? [1/2/3]
```

**If BLOCK verdict**:
```
❌ Code review BLOCKED commit!

CRITICAL ISSUES FOUND:
1. src/domain/pl_calculator.py:78 - Incorrect FIFO calculation (will cause wrong P&L)
2. src/brokers/tastytrade.py:123 - API key exposed in logs

These MUST be fixed before commit. This is a financial system - accuracy is critical.

Full review: docs/code-reviews/SESSION_171_COMMIT_abc1234.md

**ACTION REQUIRED**:
STOP - Do NOT proceed to Step 5 (commit).
Fix these issues, then re-run /commit-review from the beginning.

This is a HARD STOP. BLOCK verdicts are non-negotiable.
```

**If APPROVE verdict**:
```
✅ Code review PASSED!

Files reviewed: 3 Python files
No critical or major issues found.

Minor improvements suggested:
- Add docstrings to 2 functions (see full review)

Full review: docs/code-reviews/SESSION_171_COMMIT_abc1234.md

Proceeding with commit...
```

**Performance**:
- Agent review time: 30-90 seconds (depends on change size)
- Uses Sonnet 4.5 (accurate, thorough)
- Parallelizable with make review (can run simultaneously)

**Token efficiency**:
- Only reviews changed files (not entire codebase)
- Skips docs/config changes
- ~2-5k tokens per review (small commits)
- ~10-20k tokens per review (large commits)

**Expected output**:
```
=== Running Review Checks ===

[1/3] Auto-formatting code...
✅ black: 12 files reformatted
✅ isort: 8 files reformatted

[2/3] Running lint checks...
✅ flake8: No issues found

[3/3] Running tests with coverage...
✅ pytest: 615 passed in 45.2s
✅ coverage: 56% (above threshold: 47%)

=== Review Checks PASSED ===
Ready to commit!
```

**If formatting changed files**:
```
ℹ️ Auto-formatting changed 12 files

These changes will be included in the commit:
- src/domain/fifo_calculator.py (formatted)
- src/cli/ytd_summary.py (imports sorted)
...

Proceeding with commit...
```

**If lint fails**:
```
❌ Lint checks failed!

flake8 issues:
src/domain/fifo_calculator.py:45:1: E302 expected 2 blank lines, found 1
src/domain/fifo_calculator.py:78:80: E501 line too long (82 > 79 characters)

Options:
1. Fix lint issues manually and try again (recommended)
2. Use /commit-wip to skip validation (not recommended for review)
3. Review the issues and ask for help

What would you like to do?
```

**If tests fail**:
```
❌ Tests failed!

Failed tests:
tests/unit/test_fifo_calculator.py::test_sign_handling FAILED

Test output:
[Show relevant test failure output]

Options:
1. Fix failing tests and try again (recommended)
2. Use /commit-wip if this is intentional work-in-progress
3. Review the failure and ask for help

What would you like to do?
```

**If coverage below threshold**:
```
❌ Coverage below threshold!

Current: 45%
Required: 47%

Files lowering coverage:
- src/domain/new_feature.py (0% coverage)

Options:
1. Add tests to improve coverage (recommended)
2. Use /commit to skip coverage check (TIER 1 - fast iteration)
3. Update dynamic threshold if this is acceptable

What would you like to do?
```

---

### Step 5: Stage Changes

**Add all changes (including formatting)**:
```bash
git add .
```

**Why add all?**
- Includes auto-formatting changes from Step 4
- Includes all code changes
- Pre-commit hooks already validated everything

---

### Step 6: Commit with Message

**Create commit**:
```bash
git commit -m "$(cat <<'EOF'
<type>(<scope>): <summary>

<body>

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
EOF
)"
```

**Success**:
```
✅ Commit created!

Commit: <commit_hash>
Message: <generated_message>

Quality checks passed:
- ✅ Auto-formatted (black + isort)
- ✅ Lint checks (flake8)
- ✅ Tests passed (615 passed)
- ✅ Coverage OK (56% > 47%)

Ready to push.
```

---

### Step 7: Push to Remote

**Push changes**:
```bash
git push origin {{CONTEXT_BRANCH}}
```

**Success message**:
```
✅ Committed and pushed! (TIER 2 - Review Ready)

Commit: <commit_hash>
Branch: {{CONTEXT_BRANCH}}
Message: <generated_message>

Quality validation:
- ✅ Formatting: black + isort applied
- ✅ Lint: flake8 passed
- ✅ Tests: 615 passed in 45.2s
- ✅ Coverage: 56% (above 47% threshold)

Files changed:
- <file1> (+X, -Y)
- <file2> (+X, -Y)

This commit is READY FOR CODE REVIEW!

Next steps:
- Continue working (more commits)
- /ship when ready for production (TIER 3 - Full CI + PR)
```

---

## 🎯 TIER 2 Quality Gates

**What `/commit-review` validates**:
- ✅ **Formatting** - Black + isort (auto-fixed)
- ✅ **Lint** - Flake8 code quality checks
- ✅ **Tests** - Full test suite (with coverage)
- ✅ **Coverage** - Dynamic threshold validation

**What `/commit-review` DOES NOT validate** (deferred to /ship):
- ❌ Security scans (bandit, safety, pip-audit)
- ❌ Complexity analysis (radon)
- ❌ Type checking (mypy)
- ❌ Mutation testing
- ❌ PR creation

---

## 📊 Progressive Quality Gates Comparison

| Feature | /commit-wip | /commit | /commit-review | /ship |
|---------|-------------|---------|----------------|-------|
| **Speed** | Instant | 30s | 3-4 min | 5-10 min |
| **Use Case** | Checkpoint | Fast iteration | Code review ready | Production ready |
| **Auto-format** | ❌ No | ❌ No | ✅ Yes | ✅ Yes |
| **Lint checks** | ❌ No | ❌ No | ✅ Yes | ✅ Yes |
| **Tests** | ❌ No | ✅ Quick tests | ✅ Full tests | ✅ Full tests |
| **Coverage** | ❌ No | ❌ No | ✅ Yes | ✅ Yes |
| **Code Review Agent** | ❌ No | ❌ No | ✅ Yes (NEW) | ✅ Yes |
| **Security** | ❌ No | ❌ No | ❌ No | ✅ Yes |
| **Complexity** | ❌ No | ❌ No | ❌ No | ✅ Yes |
| **Type checking** | ❌ No | ❌ No | ❌ No | ✅ Yes |
| **PR creation** | ❌ No | ❌ No | ❌ No | ✅ Yes |

**Philosophy**:
- **TIER 0 (/commit-wip)**: Emergency checkpoint - skip all validation (end of day, mid-refactor)
- **TIER 1 (/commit)**: Fast TDD loop - tests prove correctness (30s cycle)
- **TIER 2 (/commit-review)**: Human review ready - format + lint + coverage + AI review (3-4 min)
- **TIER 3 (/ship)**: Production ready - full validation + security + merge (5-10 min)

**NEW in Session 171**: Code review agent integration
- Automated review of Python changes before commit
- Focus on trading accuracy and critical bugs
- ~60 seconds added to TIER 2 workflow
- Saves detailed reviews to docs/code-reviews/

---

## 🔧 Special Cases

### If `make review` Not Available

**Fallback to individual commands**:
```bash
# 1. Auto-format
./venv/bin/black .
./venv/bin/isort .

# 2. Lint
./venv/bin/flake8 src tests

# 3. Tests with coverage
./venv/bin/pytest --cov=src --cov-report=term-missing
```

### If Format Changes Are Extensive

**Show summary**:
```
ℹ️ Auto-formatting changed 25 files

Large formatting change detected. This is normal if:
- First time running black on new code
- Import sorting needed across many files
- Upgrading black version

All changes will be included in commit.
```

### If Hooks Fail After `make review`

**Should not happen** (make review already validated):
```
⚠️ Unexpected: Pre-commit hook failed after make review!

This shouldn't happen. Possible causes:
- make review didn't run completely
- Files changed between make review and commit

Recommendation:
1. Run make review again
2. Check for file modifications
3. Try /commit-review again
```

---

## 📋 Checklist (Internal)

Before reporting success, verify:

- ✅ On {{CONTEXT_BRANCH}} branch
- ✅ Changes analyzed and understood
- ✅ Commit message follows conventions
- ✅ `make review` passed (or equivalent)
  - ✅ Black formatting applied
  - ✅ isort applied
  - ✅ Flake8 passed
  - ✅ Tests passed
  - ✅ Coverage above threshold
- ✅ All changes staged (git add .)
- ✅ Commit created successfully
- ✅ Pushed to origin/{{CONTEXT_BRANCH}}

---

## 🎓 Best Practices

**WHEN TO USE /commit-review**:
- ✅ Before requesting code review from team
- ✅ After completing a feature (ready for PR)
- ✅ Before end of work session (ensure quality)
- ✅ When you want "clean" commits (formatted + linted)
- ✅ Periodic quality checks during development

**WHEN TO USE /commit INSTEAD**:
- ✅ During TDD loop (fast iteration, 30s cycle)
- ✅ Rapid prototyping (quality checks slow you down)
- ✅ Small refactorings (no need for full validation)
- ✅ When you know code is clean (already formatted/linted)

**WHEN TO USE /ship INSTEAD**:
- ✅ Ready to merge to {{MAIN_BRANCH}} (full CI validation)
- ✅ Creating PR for production deployment
- ✅ Need security + complexity + type checking

**DO**:
- Run `/commit-review` before end of work session
- Use `/commit-review` to batch format/lint (reduces interruptions)
- Let auto-formatting fix issues for you
- Review coverage reports (find untested code)
- **ALWAYS run code review for Python changes** (Step 4.5 is mandatory)
- Fix blocking issues before proceeding to commit

**DON'T**:
- Use `/commit-review` during fast TDD loop (use `/commit`)
- Skip quality checks to save time (use `/commit-wip` if needed)
- Ignore lint failures (they catch real issues)
- Force commit with failing tests (fix tests first)
- **NEVER skip Step 4.5 code review for Python changes** (automatic, not optional)
- Proceed with commit if BLOCK verdict received (hard stop)

---

## 🔄 Related Commands

- `/commit` - Fast iteration (TIER 1 - tests only, 30s)
- `/commit-wip` - Skip all validation (emergency checkpoints)
- `/ship` - Production ready (TIER 3 - full CI + PR, 5-10 min)
- `/end-session` - Commit docs only (never blocked by tests)

---

## 📊 Expected Timeline

**Full commit-review process**:
1. Analysis: 10-30 seconds
2. Quality checks: 90-120 seconds
   - Black: 5-10s
   - isort: 5-10s
   - Flake8: 10-20s
   - Tests + coverage: 60-90s
3. **Code review agent**: 30-90 seconds (NEW)
   - Parse changed files: 5-10s
   - Agent analysis: 20-60s
   - Save review report: 5-10s
4. Stage + commit: 2-5 seconds
5. Push: 2-5 seconds

**Total**: 3-4 minutes (worth it for human + AI review readiness!)

**Comparison**:
- `/commit-wip`: Instant (no validation)
- `/commit`: 30s (tests only)
- `/commit-review`: 3-4 min (format + lint + coverage + AI review)
- `/ship`: 5-10 min (full CI + security + PR)

---

## 💡 Why TIER 2 Exists

**Problem**: Running full CI on every commit slows down TDD loop

**Solution**: Progressive quality gates
1. **TIER 1**: Prove correctness (tests only, fast)
2. **TIER 2**: Ensure reviewability (format + lint + coverage, medium)
3. **TIER 3**: Guarantee production readiness (full CI, slow)

**Benefit**:
- Fast TDD loop (30s with `/commit`)
- Clean code for review (2-3 min with `/commit-review`)
- Production confidence (5-10 min with `/ship`)
- Separation of concerns (tests ≠ format ≠ security)

**Session 120 Lesson**: "Fast iteration > Perfect code during development"

---

**Command**: `/commit-review`
**Tokens**: ~6k (analyze + run checks + commit)
**Purpose**: Code review ready commits with full quality validation (TIER 2)
**Philosophy**: Batch format/lint at review time, not on every commit
