# Commit - Smart Code Commit with Auto-Generated Message

Commit code changes to **claude-coder-work** with an intelligently generated commit message.

**Purpose**: Analyze changes, generate conventional commit message, run pre-commit hooks, push

---

## 🔄 Commit Workflow

Follow these steps in order:

### Step 1: Pre-Commit Analysis

**Check current state**:

```bash
# Check branch
git branch --show-current
# Should be: claude-coder-work

# Check status
git status --short

# Check diff (staged + unstaged)
git diff HEAD
```

**Verify**:
- ✅ On `claude-coder-work` branch
- ✅ Has changes to commit
- ✅ Not on `main` (protected)

**If on main**:
```
❌ Cannot commit to main!

main branch is protected. Please:
1. Switch to claude-coder-work: git checkout claude-coder-work
2. Try /commit again
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

**Example analysis**:
```
Changed files:
- src/data/fifo_calculator.py
- tests/data/test_fifo_calculator.py

Diff shows:
- Fixed sign handling in FIFO matching
- Added tests for negative net_value
- Updated 7 test assertions

Type: fix (bug fix)
Scope: fifo
Summary: Sign handling in FIFO calculator
```

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

3. **Footer** (optional) - Breaking changes, issue refs
   - `BREAKING CHANGE: ...`
   - `Closes #123`
   - `Refs #456`

**Match project style**:
- Look at recent commits (from Step 2)
- Use same conventions (scope names, tone, length)

**Example good messages**:
```
feat(cli): add ytd-summary command

Aggregates realized + unrealized P/L by underlying with
support for both EOD and live quotes.

Closes #123
```

```
fix(fifo): sign handling for options

TastyTrade API returns all net_value as positive. Now properly
applying debit/credit signs based on action and instrument type.
```

```
test(adapter): add position matching tests

Covers equity/option/futures position matching with
characteristic-based fallback when order history unavailable.
```

---

### Step 4: Stage Changes

**Add all changes**:
```bash
git add .
```

**Why add all?**
- Simpler workflow (most common case)
- Pre-commit hooks will validate everything
- If you want selective staging, use git manually

---

### Step 5: Commit with Hooks

**Run commit**:
```bash
git commit -m "<generated_message>"
```

**Pre-commit hooks run automatically**:
1. Black (formatting)
2. isort (import sorting)
3. Quick tests (<30s)

**If hooks pass**:
```
✅ Pre-commit hooks passed!

Commit created: <commit_hash>
Message: <generated_message>

Ready to push.
```

**If hooks fail**:
```
❌ Pre-commit hooks failed!

Issues:
[List specific failures from hook output]

Options:
1. Fix issues and try /commit again (recommended)
2. Use /commit-wip to commit without hooks (skip validation)
3. Use git commit --no-verify manually

What would you like to do?
```

---

### Step 6: Push to Remote

**Push changes**:
```bash
git push origin claude-coder-work
```

**Success message**:
```
✅ Committed and pushed!

Commit: <commit_hash>
Branch: claude-coder-work
Message: <generated_message>

Files changed:
- <file1> (+X, -Y)
- <file2> (+X, -Y)

Ready to continue working or /ship when ready!
```

---

## 🔧 Special Cases

### If Tests Fail

**Don't force commit!**

```
❌ Tests failed in pre-commit hook!

Failed tests:
[List of failing tests]

Recommendation:
1. Fix tests first (ensures code quality)
2. Or use /commit-wip if this is work-in-progress

What would you like to do?
```

### If Merge Conflicts

**Unlikely but possible**:
```
❌ Push rejected - branch diverged!

Your branch is behind 'origin/claude-coder-work'.

Fix:
1. git pull --rebase origin claude-coder-work
2. Resolve any conflicts
3. Try /commit again
```

### If Nothing Staged

**Auto-stage everything**:
```
ℹ️ No staged changes, staging all changes...

Staged:
- <file1>
- <file2>

Proceeding with commit...
```

---

## 📋 Checklist (Internal)

Before reporting success, verify:

- ✅ On claude-coder-work branch
- ✅ Changes analyzed and understood
- ✅ Commit message follows conventions
- ✅ Changes staged (git add .)
- ✅ Pre-commit hooks passed
- ✅ Commit created successfully
- ✅ Pushed to origin/claude-coder-work

---

## 🎓 Best Practices

**DO**:
- Commit frequently (small, focused changes)
- Write clear, descriptive messages
- Let pre-commit hooks catch issues early
- Match project's commit style
- Use imperative mood ("add" not "added")

**DON'T**:
- Commit broken code (let hooks validate)
- Write vague messages ("fix bug", "update code")
- Commit docs with code (use /end-session for docs)
- Skip hooks without good reason (use /commit-wip if needed)

---

## 🔄 Related Commands

- `/commit-wip` - Commit work-in-progress (skips hooks)
- `/ship` - Create PR and merge to main
- `/end-session` - Commit docs only

---

## 📊 Expected Timeline

**Full commit process**:
1. Analysis: 10-30 seconds
2. Stage: 1 second
3. Hooks: 5-30 seconds
4. Push: 2-5 seconds

**Total**: 20-60 seconds (mostly automated)

---

**Command**: `/commit`
**Tokens**: ~5k (analyze changes, generate message)
**Purpose**: Smart commits with auto-generated conventional messages
