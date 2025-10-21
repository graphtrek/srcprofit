# Commit WIP - Quick Work-In-Progress Commit

Commit work-in-progress to **{{CONTEXT_BRANCH}}** without running pre-commit hooks.

**Purpose**: Fast commits during development when tests/lint might be failing

**⚠️ WARNING**: Skips pre-commit hooks (tests, lint, format checks)

---

## 🚀 WIP Commit Workflow

Quick and simple - no validation!

### Step 1: Check Status

**Verify state**:
```bash
# Check branch
git branch --show-current
# Should be: {{CONTEXT_BRANCH}}

# Check what's changed
git status --short
```

**If on {{MAIN_BRANCH}}**:
```
❌ Cannot commit to {{MAIN_BRANCH}}!

{{MAIN_BRANCH}} branch is protected. Please:
1. Switch to {{CONTEXT_BRANCH}}: git checkout {{CONTEXT_BRANCH}}
2. Try /commit-wip again
```

**If no changes**:
```
ℹ️ No changes to commit

Working tree is clean. Nothing to save!
```

---

### Step 2: Auto-Generate WIP Message

**Automatically infer description from git status**:

```bash
# Get changed files
git status --short | head -10

# Infer description from changes:
# - If docs/ changed: "updating documentation"
# - If tests/ changed: "working on tests"
# - If src/ changed: "code changes in progress"
# - If issues/ changed: "issue planning/documentation"
# - Multiple areas: "wip checkpoint"
```

**Auto-generate message** (no user interaction):
```
wip: <inferred_description>

Work in progress - tests/lint may be failing.
This is a checkpoint commit.
```

**Inference examples**:
- `docs/issues/ISSUE-019-*.md` → "wip: issue documentation"
- `src/cli/options_commands.py` → "wip: options trading changes"
- `tests/test_*.py` → "wip: test updates"
- Multiple files → "wip: checkpoint"

**Why auto-generate?**
- `wip:` prefix = work-in-progress (clear to everyone)
- Faster workflow (no questions asked)
- Description auto-inferred from changes
- Easy to squash later or amend
- User can always run `git commit --amend` to change message

---

### Step 3: Stage and Commit

**Stage everything**:
```bash
git add .
```

**Commit with --no-verify** (skips hooks):
```bash
git commit --no-verify -m "wip: <description>

Work in progress - tests/lint may be failing.
This is a checkpoint commit."
```

**Success**:
```
✅ WIP commit created!

Commit: <commit_hash>
Message: wip: <description>

⚠️ Pre-commit hooks SKIPPED

This commit may have:
- Failing tests
- Lint errors
- Formatting issues

Remember to fix before /ship!
```

---

### Step 4: Push to Remote

**Push changes**:
```bash
git push origin {{CONTEXT_BRANCH}}
```

**Success message**:
```
✅ WIP committed and pushed!

Commit: <commit_hash>
Branch: {{CONTEXT_BRANCH}}
Message: wip: <description>

Files changed: X

⚠️ Remember: This is work-in-progress!
- Fix tests before shipping
- Run make ci before creating PR
- Or amend/squash this commit later

Ready to continue working!
```

---

## 🔧 When To Use

### ✅ Good Use Cases

1. **End of day checkpoint** - Save work before logging off
   ```
   wip: implementing option chain cache
   ```

2. **Trying different approaches** - Save attempt before pivoting
   ```
   wip: testing DXLink for historical prices
   ```

3. **Partial implementation** - Feature half done, need to switch tasks
   ```
   wip: ytd-summary command (partial)
   ```

4. **Debugging session** - Save diagnostic changes
   ```
   wip: adding debug logging to FIFO calculator
   ```

5. **Pairing/collaboration** - Share work-in-progress with team
   ```
   wip: refactoring adapter (review needed)
   ```

### ❌ Don't Use For

1. **Production-ready code** - Use `/commit` instead (runs hooks)
2. **Documentation only** - Use `/end-session` instead
3. **Bug fixes ready to ship** - Use `/commit` → `/ship`
4. **Emergency hotfixes** - Fix properly with tests first

---

## 🎯 Best Practices

### Cleaning Up WIP Commits

**Before shipping, clean up WIP commits:**

**Option 1: Amend** (if WIP is last commit):
```bash
# Make fixes
git add .
git commit --amend --no-edit
# Or change message:
git commit --amend -m "feat: implement option chain cache"
```

**Option 2: Squash** (if multiple WIP commits):
```bash
# Interactive rebase
git rebase -i HEAD~3
# Mark commits to squash (s)
# Rewrite final message
```

**Option 3: Reset and recommit**:
```bash
# If you want to redo commits cleanly
git reset --soft origin/{{MAIN_BRANCH}}
git commit -m "feat: proper message"
```

---

## 🔄 Workflow Example

**Day 1** - Start feature:
```bash
# Make changes
/commit-wip "implementing ytd-summary"
# → Saves checkpoint, tests might be failing
```

**Day 2** - Continue:
```bash
# Make more changes
/commit-wip "ytd-summary almost done"
# → Another checkpoint
```

**Day 3** - Finish:
```bash
# Complete feature, fix tests
git reset --soft HEAD~2  # Undo 2 WIP commits
/commit                   # Smart commit with proper message
# → "feat(cli): add ytd-summary command"
```

**Ship it**:
```bash
/ship
# → Full CI/CD, PR, merge to {{MAIN_BRANCH}}
```

---

## ⚠️ Important Notes

### WIP Commits and PRs

**GitHub will show all commits**:
- WIP commits appear in PR history
- Reviewers see "wip: xyz" messages
- Not professional for production

**Clean up before PR**:
```bash
# Use git rebase or reset to clean history
# Then force push (only to your branch!)
git push --force origin {{CONTEXT_BRANCH}}
```

**Or use squash merge**:
- PR squash merge combines all commits
- WIP commits disappear from {{MAIN_BRANCH}}
- But trunk-based workflow prefers merge commits

---

## 📋 Checklist (Internal)

Before reporting success, verify:

- ✅ On {{CONTEXT_BRANCH}} branch (not {{MAIN_BRANCH}})
- ✅ Changes staged (git add .)
- ✅ Got description from user
- ✅ Commit created with --no-verify
- ✅ Pushed to origin/{{CONTEXT_BRANCH}}
- ✅ Warned user about skipped hooks

---

## 🔄 Related Commands

- `/commit` - Smart commit with pre-commit hooks
- `/ship` - Create PR and merge to {{MAIN_BRANCH}} (requires passing tests!)
- `/end-session` - Commit docs only

---

## 📊 Expected Timeline

**WIP commit process**:
1. Check status: 1 second
2. Get description: 5-10 seconds (user input)
3. Stage: 1 second
4. Commit: 1 second (no hooks!)
5. Push: 2-5 seconds

**Total**: 10-20 seconds (super fast!)

---

**Command**: `/commit-wip`
**Tokens**: ~2k (minimal, no analysis needed)
**Purpose**: Fast checkpoint commits during development (skips validation)
**Warning**: Remember to clean up before shipping!
