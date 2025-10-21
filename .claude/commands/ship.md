# Ship - Review, Create PR, Merge to Main

Ship the current work to production for **contrarian** project.

**Purpose**: Run full quality pipeline, create PR, merge to main, sync branch

---

## 🚀 Shipping Workflow

Follow these steps in order:

### Step 1: Pre-Flight Checks

**Verify readiness**:

```bash
# Check branch
git branch --show-current
# Should be: claude-coder-work

# Check status
git status
# Should be clean or have only committed changes

# Check remote sync
git fetch origin
git status
# Should show: "Your branch is ahead of 'origin/claude-coder-work' by X commits"
```

**Ask user**:
```
Ready to ship?

Current state:
- Branch: [BRANCH_NAME]
- Commits ahead: X
- Uncommitted changes: [YES/NO]

Proceed with quality review? (y/n)
```

---

### Step 2: Quality Gates (TIER 3 - Full Production)

**Run full quality pipeline**:

```bash
make ship
```

**What this runs**:
1. **Auto-format** - Black + isort (fixes code style automatically)
2. **Lint** - flake8 (catches code issues)
3. **Full CI** - Dagger containers (tests, security, complexity)
4. **Type checking** - mypy (static type validation)
5. **Mutation reminder** - Prompts for mutation testing consideration

**Total time**: 5-10 minutes (comprehensive validation)

**If CI/review fails**:
```
❌ Quality gates failed!

Issues found:
[List specific failures]

Please fix these issues:
1. [Issue 1 - file:line]
2. [Issue 2 - file:line]

After fixing, run:
  make ci    # or make review

Then try /ship again.
```

**If quality gates pass**:
```
✅ All quality gates passed!

Results:
- Formatting: ✅ Applied
- Linting: ✅ Pass
- Type check: ✅ Pass
- Security: ✅ Pass
- Tests: XXX passing
- Coverage: XX.XX% (threshold: XX%)
- Complexity: Grade A

⚠️  REMINDER: Consider running mutation tests before major releases:
   make mutate

Ready to create PR.
```

---

### Step 3: Create Pull Request

**Run PR creation**:

```bash
make pr
```

**What this does**:
1. Pushes commits to origin
2. Creates PR to main
3. Includes review evidence in PR body
4. Returns PR URL

**Success message**:
```
✅ Pull request created!

PR: https://github.com/[org]/contrarian/pull/XXX

**PR Summary**:
- Title: [AUTO-GENERATED or USER-PROVIDED]
- Base: main
- Head: claude-coder-work
- Commits: X
- Files changed: X
- Tests: XXX passing
- Coverage: XX.XX%

Ready to merge.
```

---

### Step 4: Merge to Main

**Ask user**:
```
PR created successfully!

Options:
1. Review PR on GitHub first (recommended for team projects)
2. Auto-merge now (if all gates passed and solo project)

Which do you prefer? (1/2)
```

**If user selects review first**:
```
✅ PR ready for review!

Next steps:
1. Review PR: https://github.com/[org]/contrarian/pull/XXX
2. Merge via GitHub UI
3. Come back and run: /sync

Or wait for team review/approval.
```

**If user selects auto-merge**:
```bash
# Merge PR via gh CLI (trunk-based = NO SQUASH)
gh pr merge --merge --delete-branch=false

# Or via git (if no gh CLI)
git checkout main
git pull origin main
git merge claude-coder-work
git push origin main
git checkout claude-coder-work
```

---

### Step 5: Sync Development Branch

**Run simple sync** (trunk-based = no squash = no divergence):

```bash
git pull origin main
git push origin claude-coder-work
```

**What this does**:
1. Pulls latest commits from main
2. Fast-forwards claude-coder-work (no conflicts!)
3. Pushes to keep remote in sync

**Success message**:
```
✅ Branch synced successfully!

- Pulled latest from origin/main
- claude-coder-work now in sync with main
- Working tree: clean

Ready for next session!
```

---

### Step 6: Report to User

**Complete success**:
```
🎉 Shipped successfully!

**Deployment Summary**:
- PR: https://github.com/[org]/contrarian/pull/XXX
- Status: Merged to main
- Commits: X merged (preserves history)
- Tests: XXX passing
- Coverage: XX.XX%

**Changes Deployed**:
[List of key changes from PR description]

**Branch Status**:
- claude-coder-work: Synced with main
- Ready for next session

Use /start-session to begin new work!
```

---

## 🔧 Special Cases

### If CI/Review Fails

**Don't create PR!**

```
❌ Cannot ship - quality gates failed.

The /ship command requires passing:
- make ci (or make review)

Fix issues first, then retry /ship.

To see specific failures:
  make ci    # or make review

Or to skip (NOT recommended):
  make pr    # Still runs CI first if configured
```

### If No Make Commands Available

**Manual workflow**:

```
⚠️ No Makefile found. Using manual workflow:

1. Run tests manually:
   [PROJECT_TEST_COMMAND]

2. Create PR manually:
   gh pr create --base main --head claude-coder-work

3. Merge manually (trunk-based = NO SQUASH):
   gh pr merge --merge

4. Sync manually:
   git checkout claude-coder-work
   git pull origin main
   git push
```

### If PR Already Exists

```
⚠️ PR already exists!

Existing PR: https://github.com/[org]/contrarian/pull/XXX

Options:
1. Update existing PR (push new commits)
2. Close old PR and create new one
3. Merge existing PR

What would you like to do? (1/2/3)
```

### If Merge Conflicts

```
❌ Merge conflicts detected!

Conflicts in:
- [file1]
- [file2]

Resolution steps:
1. git checkout main
2. git pull origin main
3. git checkout claude-coder-work
4. git rebase main
5. Resolve conflicts in each file
6. git rebase --continue
7. Try /ship again

Or ask for help with specific conflicts.
```

---

## 📋 Checklist (Internal)

Before reporting success, verify:

- ✅ make ci (or make review) passed (all quality gates)
- ✅ PR created successfully
- ✅ PR merged to main
- ✅ claude-coder-work synced with main
- ✅ No merge conflicts
- ✅ Working tree clean
- ✅ Tests still passing after sync

---

## 🎓 Best Practices

**DO**:
- Always run `make ci` (or `make review`) before creating PR
- Use merge commits for trunk-based workflow (preserves history)
- Sync immediately after merging (simple `git pull`)
- Include test results in PR description

**DON'T**:
- Skip quality gates (defeats the purpose)
- Merge without PR (no review record)
- Force push to main (protected branch)
- Use squash merge (creates divergence in trunk-based workflow)
- Leave branches out of sync

---

## 📊 Expected Timeline

**Full shipping process**:
1. Pre-flight: 30 seconds
2. Quality review: 1-5 minutes
3. Create PR: 30 seconds
4. Review PR: 2-10 minutes (human review)
5. Merge: 30 seconds
6. Sync: 30 seconds

**Total**: 5-17 minutes (mostly automated)

---

**Command**: `/ship`
**Tokens**: ~5k (minimal context needed)
**Purpose**: Deploy work to production with confidence
