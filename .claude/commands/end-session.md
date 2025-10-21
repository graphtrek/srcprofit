# End Session - Save Work and Update Context

End the current session and save all work for **{{PROJECT_NAME}}** project.

**Purpose**: Create session summary, update context, commit changes

---

## 📚 Required Reading BEFORE Session End

**MANDATORY**: Read these protocols before creating session handoff:

1. **`docs/workflow/session-state-transfer-protocol.md`** - Zero-degradation handoff standards
   - Definition of DONE criteria
   - Test execution decision matrix
   - Session handoff checklist
   - Context efficiency strategies

2. **`docs/planning/definition-of-done.md`** - Completion criteria
   - Verify ALL criteria met before claiming "done"
   - Calculate honest completion percentages
   - Distinguish between "implementation complete" vs "ready for testing" vs "DONE"

**Why mandatory?**
- Prevents false "done" claims that waste next session's time
- Ensures next session can start immediately (no ambiguity)
- Maintains quality standards across 100+ sessions

---

## 📝 Session End Workflow

Follow these steps in order:

### Step 0: Auto-Detect Exit Type (Normal vs Emergency)

**Check context state to determine exit type**:

```bash
# Check git status
git status --short

# Get context usage from conversation (if visible)
# Check for uncommitted changes
```

**Emergency indicators**:
- ✅ Uncommitted code detected
- ✅ Context >90% (180k+ tokens)
- ✅ User mentions "blocker" or "stuck" or "error"
- ✅ TodoWrite tasks show incomplete work

**If ANY emergency indicator**: Skip Step 1 questions, infer everything

**Exit types** (always creates `SESSION_XX_COMPLETE.md`):

**NORMAL_COMPLETE** (default):
```markdown
**Exit Type**: NORMAL_COMPLETE
**Code Status**: COMMITTED (commit XXXXXXX)
**Status**: ✅ COMPLETE
```

**EMERGENCY_WIP** (low context or uncommitted code):
```markdown
**Exit Type**: 🚨 EMERGENCY_WIP
**Code Status**: UNCOMMITTED (X files changed)
**Context Used**: [XX/200k tokens (XX%)]
**Status**: ⚠️ WIP - RESUME REQUIRED
```

---

**Emergency session template** (SESSION_XX_COMPLETE.md with emergency metadata):
```markdown
# Session XX Complete - [BRIEF_TITLE]

**Date**: [YYYY-MM-DD]
**Exit Type**: 🚨 EMERGENCY (low context, uncommitted code)
**Context Used**: [XX/200k tokens]
**Status**: ⚠️ WIP - RESUME REQUIRED

---

## 🚨 Work In Progress - RESUME HERE

**Uncommitted Files**:
- [file1.py] - [inferred purpose from git diff --stat]
- [file2.py] - [inferred purpose from git diff --stat]

**Active Tasks** (from TodoWrite, if present):
- [ ] [Task 1]
- [ ] [Task 2]

**Blockers**:
[Last error or user-stated blocker from conversation]

**Next Steps** (inferred from context):
1. [First thing to try based on conversation]
2. [Second thing to try]

---

## 📊 What Was Attempted This Session

[Inferred from git log --oneline -5 + conversation summary]

---

## 🔮 Next Session

**CRITICAL**: This session ended with uncommitted code due to low context.

**Resume Steps**:
1. Run `git diff` to review uncommitted changes
2. Continue from blockers above
3. Commit when resolved

---

**Session XX**: ⚠️ WIP - [BRIEF_SUMMARY]

Next: Session YY - Resume incomplete work
```

**Key difference from normal flow**:
- Skip Step 1 questions (infer everything)
- Add "Exit Type: EMERGENCY" metadata to session file
- Add "Work In Progress" section with resume instructions
- Always use standard naming: `SESSION_XX_COMPLETE.md` (never EMERGENCY.md or HANDOFF.md)

---

### Step 0A: Read Existing Working Docs (Refactoring Strategy)

**Purpose**: Avoid regenerating content that already exists - refactor instead!

**BEFORE creating SESSION_XX_COMPLETE.md**, check for working docs created during the session:

```bash
# List all session files except COMPLETE
ls docs/sessions/SESSION_{{session}}_*.md 2>/dev/null | grep -v COMPLETE
```

**If working docs exist** (e.g., FIXES_SUMMARY, AUDIT, PLANNING):

1. **Read them in PARALLEL**:
   - SESSION_{{session}}_FIXES_SUMMARY.md
   - SESSION_{{session}}_AUDIT_*.md
   - SESSION_{{session}}_PLANNING.md
   - Any other SESSION_{{session}}_*.md files

2. **Refactoring strategy**:
   - ✅ **Extract** key findings (copy 1-2 lines per task, don't regenerate)
   - ✅ **Condense** verbose sections (~70% compression: 400 lines → 150 lines)
   - ✅ **Reference** working docs for emergency detail (add links)
   - ✅ **Preserve** structure (keep section headings from working docs)
   - ❌ **Don't regenerate** - reuse existing prose!

3. **Target**: SESSION_XX_COMPLETE.md should be ~100-150 lines (scannable summary)

4. **Keep working docs**: They serve as emergency backup for full detail

**If no working docs exist** (simple session):
- Proceed normally (create COMPLETE from conversation summary)
- COMPLETE will be ~150-200 lines (all content inline)

**Why refactor instead of regenerate?**
- ✅ Zero duplication (extract from existing content)
- ✅ Token efficient (reuse ~70% of existing prose)
- ✅ Preserves detail (working docs available for emergencies)
- ✅ Faster generation (copy/condense vs write from scratch)

**Emergency access pattern**:
- **Normal**: Read SESSION_XX_COMPLETE.md (~150 lines, quick overview)
- **Emergency**: Read SESSION_XX_FIXES_SUMMARY.md (~400 lines, full technical detail)

---

### Step 0B: Check for Existing COMPLETE File (Rerun Safety)

**Purpose**: Prevent duplicate/inconsistent filenames when `/end-session` is rerun

**Check if SESSION_XX_COMPLETE.md already exists**:

```bash
# Get current session number
CURRENT_SESSION=$(grep "Latest Session:" docs/workflow/session-index.md | grep -o '[0-9]*')

# Check if COMPLETE file exists
if [ -f "docs/sessions/SESSION_${CURRENT_SESSION}_COMPLETE.md" ]; then
    echo "⚠️ SESSION_${CURRENT_SESSION}_COMPLETE.md already exists!"
fi
```

**If COMPLETE.md exists** (rerun scenario):

**Action: Update existing file (ALWAYS option 1)**

1. **Read existing SESSION_XX_COMPLETE.md** first
2. **Append or refactor** new information into existing sections:
   - If new tasks completed: Add to "What Was Completed" section
   - If new insights: Add to "Key Lessons" section
   - If blocker resolved: Update "Next Session" section
   - If context emergency: Update "Exit Type" metadata
3. **Preserve original structure** (don't create new file)
4. **Use Edit tool** to update existing COMPLETE.md (not Write tool)

**If COMPLETE.md does NOT exist** (normal flow):
- Proceed to create new SESSION_XX_COMPLETE.md

**Why always option 1 (update existing)?**
- ✅ Consistent naming (always SESSION_XX_COMPLETE.md)
- ✅ No file proliferation (no _v2, _FINAL, _SUMMARY variants)
- ✅ Git tracks changes (history preserved automatically)
- ✅ Simple for next session (one file to read)

**NEVER create these filenames**:
- ❌ SESSION_XX_COMPLETE_v2.md
- ❌ SESSION_XX_SUMMARY.md
- ❌ SESSION_XX_FINAL.md
- ❌ SESSION_XX_EMERGENCY.md
- ❌ SESSION_XX_COMPLETE_UPDATED.md

**ALWAYS use**:
- ✅ SESSION_XX_COMPLETE.md (update if exists, create if not)

---

### Step 1: Auto-Infer Session Metadata (Always, Never Ask)

**ALWAYS infer metadata automatically** (Session 120 pattern - no questions!):

```bash
# 1. Get session number
LAST_SESSION=$(grep "Latest Session:" docs/workflow/session-index.md | grep -o '[0-9]*')
CURRENT_SESSION=$((LAST_SESSION))  # Same number (update existing or create)

# 2. Infer summary from conversation
# - Last user request
# - TodoWrite task summaries
# - Git commit messages

# 3. Check git for context
git log --oneline -5  # Recent commits
git status --short    # Current state
```

**Infer from conversation**:
- Brief summary: Last major task or feature worked on
- Key accomplishments: Completed TodoWrite tasks or user confirmations
- Bugs fixed: Error messages mentioned then resolved
- Metrics: Test output in conversation, coverage reports shown

**Why never ask questions?**
- ✅ Faster (no back-and-forth)
- ✅ Works in emergency (low context)
- ✅ Consistent (same pattern always)
- ✅ Session 120 validated this approach

---

### Step 2: Create Session Summary (WITH PROTOCOL VERIFICATION)

**BEFORE creating summary**, verify against `docs/workflow/session-state-transfer-protocol.md`:

1. ✅ **Code Status Reality Check**
   - All claimed "completed" work meets Definition of Done?
   - Completion percentages reflect REALITY (not optimism)?
   - Uncommitted code explicitly listed?
   - Test failures explicitly listed (if any)?

2. ✅ **Next Session Clarity**
   - Next steps actionable (not vague)?
   - Blockers explicit (with workarounds if known)?
   - Dependencies clear (what needs to happen first)?
   - User testing queue has EXACT commands to run?

3. ✅ **Honesty Over Optimism**
   - "80% complete" = 80% of Definition of Done met?
   - "Needs testing" = Not done, testing is part of done?
   - "Implementation complete" = Only if ALL criteria met?

Create file: `docs/sessions/{{SESSION_PREFIX}}_XX_COMPLETE.md`

**Template** (following Session State Transfer Protocol + Refactoring Strategy):
```markdown
# Session XX Complete - [BRIEF_TITLE]

**Date**: [YYYY-MM-DD]
**Duration**: [HOURS]
**Status**: ✅ [STATUS]
**Exit Type**: [NORMAL_COMPLETE | NORMAL_WIP | EMERGENCY_WIP]
**Context Used**: [XXk/200k] (XX%)

---

## 🎯 Mission Accomplished

[1-2 sentence summary - COPY from working doc if exists, else write from conversation]

---

## ✅ What Was Completed

### Task 1: [Task Name]
**Status**: [DONE|PARTIAL] (XX% complete)
**Summary**: [1-line extracted from working doc or inferred from conversation]
**Files**: `path/to/file.py:123-456`

👉 **Full details**: SESSION_XX_FIXES_SUMMARY.md#task-1 (if working doc exists)

### Task 2: [Task Name]
**Status**: [DONE|PARTIAL] (XX% complete)
**Summary**: [1-line extracted from working doc]
**Files**: `path/to/file.py:789`

👉 **Full details**: SESSION_XX_AUDIT_ANALYSIS.md (if working doc exists)

---

## 🐛 Bugs Fixed (if any)

### Bug 1: [TITLE]
**Symptom**: [Description]
**Root Cause**: [Explanation]
**Fix**: [What was changed]
**Files**: [List of files]

---

## 📊 Impact

### Metrics
- Tests: XX passing
- Coverage: XX%
- Performance: [Metrics if relevant]

### Files Changed
- [file1.py] - [what changed]
- [file2.py] - [what changed]

---

## 📚 Key Lessons

1. [Lesson that applies to future work]
2. [Avoid this mistake]
3. [Best practice discovered]

---

## 🔮 Next Session

**Immediate**:
- [ ] [Next task]
- [ ] [Next task]

**Short Term**:
- [ ] [Upcoming feature]

---

**Related Documentation** (if working docs exist):
- SESSION_XX_FIXES_SUMMARY.md - Technical details ([N] tasks, [XXX] lines)
- SESSION_XX_AUDIT_*.md - Root cause analysis ([topic])
- SESSION_XX_PLANNING.md - Design decisions ([topic])

**Session XX**: ✅ COMPLETE - [BRIEF_SUMMARY]

Next: Session YY - [NEXT_FOCUS]
```

---

### Step 3: Update Active Context (ENHANCED)

Update `{{ACTIVE_CONTEXT_FILE}}`:

**Changes needed**:

1. **Add/Update "Next Session" section** (NEW - Priority 3):
   ```markdown
   ## 📋 Next Session

   **Session Number**: [XX+1]
   **Exit Type**: [NORMAL_COMPLETE / NORMAL_WIP / EMERGENCY_WIP]
   **Code Status**: [COMMITTED / UNCOMMITTED]
   **Context Used**: [XX/200k tokens (XX%)]

   **Resume Task**: [Clear 1-sentence task if WIP/EMERGENCY]
   **Blocker**: [If present]
   **Next Steps**:
   1. [First step]
   2. [Second step]
   ```

2. **Update "Active Work" section** (lines ~20-30):
   - Add new session summary with EXIT TYPE
   - Remove oldest session if >3 sessions listed
   - Keep only last 3 sessions

   **Enhanced format**:
   ```markdown
   ### Session XX (YYYY-MM-DD) - [STATUS]
   - **Exit**: [NORMAL_COMPLETE / NORMAL_WIP / EMERGENCY_WIP]
   - **Duration**: [X hours] (inferred from session start/end)
   - **Context Used**: [XX/200k] (XX%)
   - **Work**: [1-sentence summary]
   - **Next**: [Only if WIP/EMERGENCY]
   ```

3. **Update "Current Focus"** (line ~5):
   - Change to next session's focus

4. **Update "Current Tasks"** with TodoWrite persistence (NEW - Priority 4):
   - Extract open TodoWrite tasks from conversation history
   - Merge into "In Progress" subsection
   - Mark completed tasks as done
   - Add new tasks for next session

**Example diff**:
```diff
+ ## 📋 Next Session
+
+ **Session Number**: 111
+ **Exit Type**: EMERGENCY_WIP (low context, uncommitted code)
+ **Code Status**: UNCOMMITTED (3 files)
+ **Context Used**: 185k/200k (92%)
+
+ **Resume Task**: Fix quote fetching for position-risk command
+ **Blocker**: TastyTrade REST API returns empty for futures
+ **Next Steps**:
+ 1. Try DXLink streaming API
+ 2. Check Position model for cached prices
+ 3. Add --price flag as fallback
+
+ ---

  ## 🎯 Active Work (Last 3 Sessions)

+ ### Session 110 (2025-01-15) - ⚠️ PARTIAL
+ - **Exit**: EMERGENCY_WIP (low context, uncommitted code)
+ - **Duration**: 2.5 hours
+ - **Context Used**: 185k/200k (92%)
+ - **Work**: Position risk CLI integration (quote fetching blocked)
+ - **Next**: Fix futures quote fetching

- - Session 110: Position Risk CLI Integration (2025-01-15, ⚠️ PARTIAL)

  ## 📋 Current Tasks (This Sprint)

+ **In Progress** (from TodoWrite):
+ - [ ] Fix quote fetching for /GCZ5 (Session 110 blocked here)
+ - [ ] Test position-risk with real data
+
  **Completed**:
- - [ ] Orders table implementation
+ - [x] Created PositionRiskAnalyzer
+ - [x] Integrated into CLI
```

---

### Step 4: Update Session Index

**CRITICAL**: Use **Read-then-Update** pattern!

**Process**:
1. Read `{{SESSION_INDEX_FILE}}` (required by Edit tool)
2. Update header: `**Latest Session**: XX` → `**Latest Session**: XX+1`
3. Add new row to table at top (most recent first)

**New table row format**:
```markdown
| XX | YYYY-MM-DD | Brief focus | STATUS | SESSION_XX_COMPLETE.md |
```

**Status icons**:
- 🔴 IN PROGRESS - Just started, uncommitted work
- ✅ COMPLETE - Finished, committed, documented
- ⚠️ WIP - Partial completion, blockers present
- ⚠️ PARTIAL - Significant progress, some goals incomplete

**Safety check after update**:
- Run `head -20 {{SESSION_INDEX_FILE}}` to verify header and new row added
- If missing, retry update with explicit Edit

---

### Step 5: Update Issue Index (NEW - Session 174)

**Auto-generate issue tracking README**:

```bash
python scripts/update_issue_index.py
```

**What it does**:
- Scans all ISSUE-*.md files in docs/issues/
- Extracts metadata (Status, Priority, Category, Estimated, Actual)
- Generates docs/issues/README.md with statistics
- Shows open/closed/partial counts
- Groups by priority and category
- Fast execution (<1 second)

**When to run**:
- ✅ **ALWAYS** at end of session (keeps README current)
- After closing an issue
- After creating a new issue
- After updating issue metadata

**Output example**:
```
Found 21 issue files
  ✓ ISSUE-001: Port tastytrade adapter [CLOSED]
  ✓ ISSUE-021: Automated issue tracking [CLOSED]
  ...
✅ Generated docs/issues/README.md

Summary:
  Total: 21
  Open: 12
  Closed: 6
  Partial: 3
```

**Why this matters**:
- README always reflects current state
- Clear visibility into completion progress
- No manual updates (automates ISSUE-021)
- Takes <1 second (zero overhead)

---

### Step 6: Verify All Code Committed

**Check git status**:

```bash
git status
```

**If uncommitted code changes**:
```
⚠️ Uncommitted code changes detected!

Code should be committed DURING the session with /commit, not at end.

Options:
1. Commit code now with /commit (recommended - runs pre-commit hooks)
2. Commit as WIP with /commit-wip (if tests failing)
3. Proceed with /end-session anyway (will only commit docs/)

What would you like to do?
```

**Why this check?**
- Prevents accidentally leaving code uncommitted
- Enforces "commit during work, docs at end" workflow
- Ensures code goes through proper validation

---

### Step 7: Commit and Push

**IMPORTANT**: Only commit documentation files!

**Why?**
- Code commits must pass tests (pre-commit hook enforces this)
- Code commits happen DURING session with `/commit` command
- Documentation commits shouldn't be blocked by failing tests
- Separation of concerns: code ≠ docs

**Commit message format** (OPTIMIZED - Session 166):

**Key insight**: Detailed info already in SESSION_XX_COMPLETE.md, commit message should be scannable summary only.

```bash
# Single atomic operation
git add docs/ && git commit --no-verify -m "docs: Session XX complete - [Brief title]

[1-2 sentence summary of key achievements]

Files: SESSION_XX_COMPLETE.md, [other files if any]
Duration: [X.X]h | Context: [XX]% | Status: [COMPLETE/WIP]

Next: Session [XX+1] - [Next focus]

🚀 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude <noreply@anthropic.com>" && git push origin {{CONTEXT_BRANCH}}
```

**Example**:
```bash
git add docs/ && git commit --no-verify -m "docs: Session 166 complete - Issue planning

Created ISSUE-012 (methodology explanations) and ISSUE-013 (live trading).
All 13 issues now follow kebab-case convention.

Files: SESSION_166_COMPLETE.md, ISSUE-012, ISSUE-013
Duration: 0.5h | Context: 29% | Status: COMPLETE

Next: Session 167 - Resume Session 165 work (6 core scenarios)

🚀 Generated with [Claude Code](https://claude.com/claude-code)
Co-Authored-By: Claude <noreply@anthropic.com>" && git push origin claude-coder-work
```

**Why optimized format?**
- ✅ **Scannable**: Quick summary for `git log --oneline`
- ✅ **No duplication**: Details in session file (single source of truth)
- ✅ **Fast**: Single atomic operation (add + commit + push)
- ✅ **Context-efficient**: ~10 lines vs ~60 lines (saves ~100 tokens per session)
- ✅ **Preserved**: Co-authored footer for credit tracking
- ✅ **Useful**: Quick metrics for PR context (duration, context %, status)

**What NOT to include** (already in session file):
- ❌ Detailed task breakdowns
- ❌ Full metrics tables
- ❌ Bug details
- ❌ Key lessons
- ❌ File change lists with descriptions

**Why `--no-verify`?**
- Documentation commits shouldn't run tests (separation of concerns)
- Code commits happen DURING session with `/commit` (with hooks)
- Docs commits happen AT END with `/end-session` (skip hooks)
- Prevents test failures from blocking session documentation

---

### Step 8: Check Context Usage

Before ending, check context usage:

```bash
# Context usage is shown at bottom of conversation
# Example: "Token usage: 45000/200000; 155000 remaining"
```

**Decision logic**:
- **If < 100k tokens used**: Safe to continue without clear
  - Message: "Context usage: XX/200k (XX% used). Safe to continue next session without /clear."
- **If > 100k tokens used**: Suggest /clear before next session
  - Message: "⚠️ Context usage: XX/200k (XX% used). Recommend /clear before next session to ensure space for long work."

**Always recommend /clear after /end-session**:
- Reason: Next session might be long and need full token budget
- User can choose to continue if they know next session is short

---

### Step 9: Report to User

**Success message**:
```
✅ Session XX saved successfully!

**Summary**:
- Created: docs/sessions/SESSION_XX_COMPLETE.md
- Updated: {{ACTIVE_CONTEXT_FILE}}
- Updated: {{SESSION_INDEX_FILE}}
- Updated: docs/issues/README.md (auto-generated)
- Committed: "docs: Session XX complete - [Brief title]"
- Pushed to: {{CONTEXT_BRANCH}}

**Session XX Achievements**:
[List key accomplishments]

**Metrics**:
- Tests: XX passing
- Coverage: XX%
- Files changed: X

**Next Session Focus**: [NEXT_FOCUS]

**Context Usage**: XX/200k (XX% used)
**Recommendation**: Use /clear before next session to ensure full token budget.

Ready to continue? Use /clear then /start-session to begin Session YY.
```

---

## 🔧 Special Cases

### If Tests Failing

**Documentation commits are independent of tests!**

```
ℹ️ Tests failing, but /end-session only commits documentation.

Test failures:
[List of failing tests]

Options:
1. Fix tests and commit code with /commit (recommended)
2. Leave tests failing, commit as WIP with /commit-wip
3. Proceed with /end-session (commits docs only, code stays uncommitted)

Note: /end-session only commits docs/ directory, so failing tests don't block it.
```

### If Major Phase Complete

**Also update `{{FULL_CONTEXT_FILE}}`**:

1. Mark phase complete in "Phase Overview"
2. Add key lessons to "Permanent Lessons"
3. Consider creating `docs/sessions/PHASE_X_SUMMARY.md`

**Ask user**:
```
🎉 Phase complete! Should I also:
- Update CLAUDE_CONTEXT.md with phase completion?
- Create PHASE_X_SUMMARY.md?
```

### If Large Uncommitted Changes

**Suggest splitting**:
```
⚠️ Large changeset detected (XX files changed).

Recommend:
1. Commit work first: git add . && git commit -m "[your message]"
2. Then run /end-session for documentation updates

Proceed anyway? (y/n)
```

---

## 📋 Checklist (Internal)

Before reporting success, verify:

- ✅ docs/sessions/SESSION_XX_COMPLETE.md created with all sections
- ✅ {{ACTIVE_CONTEXT_FILE}} updated (3 changes minimum)
- ✅ {{SESSION_INDEX_FILE}} has new entry
- ✅ Tests passing (or user acknowledged failures)
- ✅ Changes committed
- ✅ Changes pushed to {{CONTEXT_BRANCH}}

---

## 🎓 Best Practices

**DO**:
- Read Session State Transfer Protocol BEFORE creating handoff
- Verify Definition of Done for all "completed" claims
- Calculate honest completion percentages
- Create detailed session summaries (helps future sessions)
- Capture lessons learned while fresh
- Update context immediately (don't defer)
- Commit even if tests failing (mark as WIP)
- Provide actionable next steps (not vague)

**DON'T**:
- Claim "done" without meeting ALL Definition of Done criteria
- Claim "90% complete" if user testing not done
- Use vague next steps ("continue working on X")
- Hide blockers or test failures
- Skip session summaries (you'll forget details)
- Update CLAUDE_CONTEXT.md every session (only per phase)
- Combine session end with code commits (separate concerns)
- Leave context stale (update before leaving)

---

## 📋 Protocol Enforcement

**Every session handoff MUST**:
1. Verify tasks against Definition of Done (docs/planning/definition-of-done.md)
2. Report honest completion percentages
3. List incomplete criteria explicitly
4. Provide actionable next steps for incomplete work
5. Follow Session State Transfer Protocol (docs/workflow/session-state-transfer-protocol.md)

**Quality indicators**:
- ✅ Work claimed "done" requires no rework
- ✅ Next session can start immediately
- ✅ Completion percentages match reality
- ✅ No surprises when resuming work

---

## 📅 Step 6: Check Weekly Retrospective Boundary (Auto-Detect)

**AFTER** committing session docs, check if weekly boundary has been crossed:

```python
from datetime import datetime
import glob
import re

def check_weekly_retrospective_reminder():
    """Auto-detect if retro is due (weekly limit reset)"""

    # Get current ISO week
    now = datetime.now()
    current_year, current_week, _ = now.isocalendar()

    # Find latest retro file
    retro_files = glob.glob("docs/retrospectives/RETRO-*.md")

    if retro_files:
        # Sort to get latest
        latest_retro = sorted(retro_files)[-1]

        # Parse RETRO-2025-W43-summary.md -> 2025, 43
        match = re.search(r'RETRO-(\d{4})-W(\d{2})-summary\.md', latest_retro)
        if match:
            last_year = int(match.group(1))
            last_week = int(match.group(2))

            # Check if we've crossed into new week
            if (current_year, current_week) > (last_year, last_week):
                print("\n" + "="*60)
                print("📅 WEEKLY RETROSPECTIVE DUE")
                print("="*60)
                print(f"\n✨ Claude weekly limit likely reset!")
                print(f"   Last retro: Week {last_year}-W{last_week:02d}")
                print(f"   Current week: Week {current_year}-W{current_week:02d}")
                print(f"\n💡 Consider running: /retro")
                print(f"   (Takes 30-45 min, helps improve process)\n")
                print("="*60 + "\n")
    else:
        # No retros yet
        print("\n📅 No retrospectives found. Consider running /retro to start weekly reflections.\n")
```

**Non-Blocking**: This is a friendly reminder only
- ✅ User can choose to run /retro now or later
- ✅ Does not block session end
- ✅ Does not create retro automatically

**Why weekly boundary?**
- Claude usage limits reset weekly
- Natural reflection cadence
- Fresh memory of week's work
- Sustainable frequency (not too often)

---

**Command**: `/end-session`
**Tokens**: ~2k (minimal context needed)
**Time**: 5-10 minutes (worth the investment!)
**Quality**: Prevents wasted time in next session
