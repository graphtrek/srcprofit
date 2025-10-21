# Start Session - Lightweight Context Loading

You are starting a new session on the **{{PROJECT_NAME}}** project.

**Purpose**: Load minimal context for daily development work (~7k tokens vs 50k)

---

## 🗄️ Step 0: Auto-Archive Check (SILENT - No User Interaction)

**BEFORE loading context**, check if SESSION_INDEX.md needs archiving.

**Threshold**: If SESSION_INDEX.md table has > 40 sessions (excluding header/notes)

**How to check**:
```bash
# Count table rows (excluding header and summary sections)
grep "^|" docs/workflow/session-index.md | grep -v "^|---|" | grep -v "^| # |" | wc -l
# If result > 40, trigger archive
```

**Auto-Archive Process** (if threshold exceeded):
1. **Read current session-index.md** (required for Edit)
2. **Extract oldest N sessions** (keep last 30, archive the rest)
   - Find table rows beyond row 30
   - Extract those rows to append to archive
3. **Append to archived-session-index.md**:
   ```bash
   # Format: Add archived rows to end of archive file
   # Prepend with "## Archived [DATE]" section if first archive
   ```
4. **Update session-index.md**:
   - Keep header unchanged
   - Keep last 30 rows in table
   - Keep phase summary and notes sections
5. **Silent success** - Don't tell user unless there's an error

**Error handling**:
- If archive fails, proceed anyway (don't block session start)
- Log error message for user but continue

**Why silent?**:
- User doesn't care about housekeeping
- Happens in background with plenty of context headroom
- Only speak up if there's a problem

**Example outcome** (don't show to user):
```
✓ Auto-archive: Moved 15 oldest sessions to archived-session-index.md
✓ session-index.md: 45 rows → 30 rows (kept last 30 sessions)
```

**Archive Rotation (logrotate-style)** - Check AFTER auto-archive:

If `archived-session-index.md` > 500 lines (digestible limit):

1. **Rotate archives**:
   ```bash
   # Shift existing rotations
   mv archived-session-index.2.md archived-session-index.3.md  # (if exists)
   mv archived-session-index.1.md archived-session-index.2.md  # (if exists)

   # Find split point (session boundary, not line number!)
   # Split at session header (e.g., "## Session 50" or "### Session 50")
   # Goal: Keep newest ~250 lines in main, rotate older ~250+ lines to .1
   ```

2. **Split at session boundary**:
   ```bash
   # Example: If file has 520 lines, find session near line 260
   grep -n "^## Session\|^### Session" archived-session-index.md
   # Split at closest session header to middle
   ```

3. **Create rotation**:
   - Head (newest sessions) → `archived-session-index.md` (keep name)
   - Tail (older sessions) → `archived-session-index.1.md` (rotate to .1)

4. **Update headers**:
   - Main archive: Update "Older Archives" pointer
   - Rotated .1: Add header with session range

**Rotation limits**:
- Main archive: < 500 lines (~10k tokens, digestible)
- Each rotation (.1, .2, .3): < 1500 lines (~25k tokens, readable in one Read call)
- Max rotations: Keep .1, .2, .3 (delete .4+ if exists)

**Why 500 lines for archives?**
- 2x session-index threshold (250 vs 150 lines)
- Archives have detailed narratives (more verbose than index table)
- 500 lines ≈ 10k tokens (well under 25k Read limit)

---

## 📍 Step 1: Quick Context Load

**OPTIMIZATION**: Read files in PARALLEL for 3x faster startup

Read these files simultaneously:

1. **Entry Point**: `CLAUDE.md` - Project pointer, workflow, critical lessons
2. **Active Context**: `{{ACTIVE_CONTEXT_FILE}}` - Last 3 sessions, active tasks, current focus
3. **Session Handoff** (check in priority order):
   - First: `docs/sessions/SESSION_[N]_HANDOFF.md` (emergency sessions only)
   - Second: `docs/sessions/SESSION_[N-1]_COMPLETE.md` (standard handoff)
   - Where N is "Next Session" number from active context

   **Note (as of Session 168)**:
   - COMPLETE files now ~150 lines (refactored, condensed from ~260)
   - May reference working docs (SESSION_XX_FIXES_SUMMARY.md, etc.)
   - Don't read working docs unless emergency or explicitly needed

4. **Project Status** (git): Run `git status` and `git log --oneline -20` in parallel

**Expected load time**: ~2-3 seconds (vs ~6-9 seconds sequential)

---

## 📄 Step 1.5: Understand Session File Structure (NEW - Session 168+)

**As of Session 168**, sessions follow refactoring pattern to eliminate duplication:

### File Types:

**Primary handoff file**:
- `SESSION_XX_COMPLETE.md` (~100-150 lines)
  - Brief summaries of what was completed
  - References to working docs for detail (e.g., "👉 Details: SESSION_XX_FIXES_SUMMARY.md#task-2")
  - Key lessons and next steps
  - **Read this FIRST** (sufficient 95% of time)

**Working docs** (optional, created during complex sessions):
- `SESSION_XX_FIXES_SUMMARY.md` (~400 lines) - Technical fixes detail
- `SESSION_XX_AUDIT_*.md` (~250 lines) - Root cause analysis
- `SESSION_XX_PLANNING.md` (~500 lines) - Design discussions
- Created mid-session for detailed documentation
- **Don't auto-load** - only read if COMPLETE references them AND you need detail

### Access Pattern:

**Normal startup** (95% of time):
1. Read SESSION_XX_COMPLETE.md only
2. COMPLETE has enough context to start work
3. Working docs available if referenced, but don't load unless needed

**Emergency access** (5% of time - when COMPLETE lacks critical detail):
1. Read SESSION_XX_COMPLETE.md first
2. If blocker/context unclear → Check for references to working docs in COMPLETE
3. Read specific working doc section only if COMPLETE references it
4. Use case: Debugging regression, need root cause detail

**When NOT to read working docs**:
- ❌ Routine session start (COMPLETE is sufficient)
- ❌ "Just in case" (wastes tokens)
- ✅ Only when COMPLETE explicitly references detail AND you need it

---

## 🎯 Step 2: Session Startup Checklist

After loading context, perform these steps:

1. **Verify branch**: Check we're on `{{CONTEXT_BRANCH}}`
2. **Clean up background shells**: Check `/bashes` and kill orphaned processes automatically
3. **Get session number**: Read from "Next Session" in active context (or read header from session-index)
4. **Start session timer**: Record start time for duration tracking in `/end-session`
5. **Understand current focus**: Extract from active context
6. **See active tasks**: Extract from active context

---

## 💬 Step 3: User Prompt (AUTO-DETECT EXIT TYPE)

After loading context, **check SESSION_[N-1]_COMPLETE.md for "Exit Type" metadata**:

```bash
# Get last session number
LAST_SESSION=$(grep "Latest Session:" docs/workflow/session-index.md | grep -o '[0-9]*')

# Read last session COMPLETE file
cat docs/sessions/SESSION_${LAST_SESSION}_COMPLETE.md | grep "Exit Type:"
```

**Exit types to detect**:
- `EMERGENCY_WIP` - Emergency exit (uncommitted code, low context)
- `NORMAL_WIP` - Normal exit with incomplete work
- `NORMAL_COMPLETE` - Session completed successfully (default)

### IF Exit Type = EMERGENCY_WIP or NORMAL_WIP:

```
🚨 Session started! Resuming from incomplete work (last session: EMERGENCY/WIP exit)

**Last Session**: Session [XX]
**Exit Type**: [EMERGENCY_WIP | NORMAL_WIP]
**Code Status**: [COMMITTED | UNCOMMITTED]

---

## 🎯 Where You Left Off

**You were working on**: [Extract from COMPLETE.md "What Was Attempted" or "Resume Task"]

**Blocked On**: [Extract blocker from COMPLETE.md "Next Session" section]

**Next Steps** (from last session):
1. [First step from COMPLETE.md]
2. [Second step]

---

## 📋 Uncommitted Work (if any)

**Files changed** (from git status):
- [file1.py] - [inferred from COMPLETE.md or git diff]
- [file2.py] - [inferred]

---

## 📚 Available Detail (if referenced in COMPLETE.md)

[If COMPLETE references working docs, show them]:
- SESSION_[XX]_FIXES_SUMMARY.md - Technical details
- SESSION_[XX]_PLANNING.md - Design context
- (Read these if blocker is unclear)

---

**Options**:
1. Resume from blocker above (recommended)
2. Start fresh work (park incomplete work for later)
3. Check git diff first (see uncommitted changes)

What would you like to do? [1/2/3]
```

**Key difference**: No separate `/resume-emergency` command needed - `/start-session` auto-detects!

### IF Exit Type = NORMAL_COMPLETE (or no "Next Session" section):

```
Session started! Last session completed successfully.

**Current State**:
- Session: [NUMBER] (from Next Session section or session-index header)
- Branch: [CURRENT_BRANCH]
- Last work: [LAST_SESSION_SUMMARY]
- Focus: [CURRENT_FOCUS from active context]

What would you like to work on today?

(Need more context? Use /full-context for architecture details)
```

**Key improvement**: Instant awareness of incomplete work with guided resume path.

---

## 📚 When to Load More Context

**Suggest `/full-context` if user asks about:**
- Architecture or design decisions
- How specific systems work
- Historical context (old sessions)
- Complete knowledge base

**Otherwise**: Proceed with lightweight context (saves tokens!)

---

## 🔧 Troubleshooting

**If active context feels stale:**
- Ask user to run `/full-context` once to refresh understanding
- This is normal at start of new phase

**If can't find session number:**
- Read session-index.md header: `**Latest Session**: XX`
- Session number = latest + 1

**If unsure about project:**
- This is `{{PROJECT_NAME}}` project
- Context branch: `{{CONTEXT_BRANCH}}`
- Main branch: `{{MAIN_BRANCH}}`

---

## 📋 Files NOT Loaded (Load When Needed)

Skip these unless user specifically asks:

- `{{FULL_CONTEXT_FILE}}` - Reference documentation (load via /full-context)
- `docs/knowledge-base-index.md` - Resource catalog
- `docs/sessions/SESSION_XX_COMPLETE.md` - Historical sessions
- `docs/architecture/` - Architecture documentation
- Project-specific documentation

**Why skip**: Saves 40k+ tokens for daily work

---

## 🎓 Best Practices

1. **Read files in parallel** - Use multiple tool calls in one message (3x faster)
2. **Auto-clean background shells** - Silently kill orphaned processes
3. **Auto-archive SESSION_INDEX** - Silently maintain table size (Step 0)
4. **Trust the active context** - It has what you need for daily work
5. **Load more only when needed** - Saves tokens
6. **Ask user if uncertain** - Don't guess about requirements
7. **Use TodoWrite tool** - Track tasks throughout session
8. **Reference files by line number** - Help user navigate (e.g., `file.py:42`)
9. **Start session timer** - Record time for `/end-session` duration calculation

---

## 📊 Auto-Archive Configuration

**session-index.md Thresholds** (optimized for 30/40):
- **Trigger**: > 40 sessions in table
- **Keep**: Last 30 sessions in main table
- **Archive**: Move oldest 10+ sessions to archived-session-index.md

**archived-session-index.md Rotation** (logrotate-style):
- **Trigger**: > 500 lines (digestible limit)
- **Split**: At session boundary (not line number!)
- **Keep**: Newest ~250 lines in main archive
- **Rotate**: Older ~250+ lines to archived-session-index.1.md
- **Max rotations**: .1, .2, .3 (delete .4+ if exists)

**Why these limits?**
- **session-index**: 30 sessions ≈ 1.5-2 months visible, archives every 2 weeks
- **Archives**: 500 lines ≈ 10k tokens (digestible), 2x verbose vs index
- **Rotations**: 1500 lines max ≈ 25k tokens (one Read call)

**Growth pattern**:
- session-index: 3 lines/session → 165 lines @40 sessions → archive → 135 lines
- archived-session-index: ~12 lines/session → 500 lines @40 sessions → rotate → 250 lines

**File structure**:
```
docs/workflow/session-index.md                      (30 most recent, ~135 lines)
docs/sessions/archived-session-index.md             (next 40-50 sessions, ~250-500 lines)
docs/sessions/archived-session-index.1.md           (older 40-50 sessions, ~250-500 lines)
docs/sessions/archived-session-index.2.md           (even older, ~250-500 lines)
docs/sessions/archived-session-index.3.md           (oldest kept, ~250-500 lines)
```

**Archive format**:
```markdown
# Archived Session Index

**Purpose**: Historical sessions removed from main session-index.md for token efficiency

**Latest Archive Date**: [YYYY-MM-DD]
**Rotation**: Sessions [XX-YY] archived from session-index.md
**Older Archives**: See `archived-session-index.1.md` for Sessions [AA-BB]

---

## Quick Reference Table (Most Recent Archives)

| # | Date | Focus | Status | File |
|---|------|-------|--------|------|
[Table of recently archived sessions from session-index.md]

---

## Detailed Session Summaries (Sessions XX-YY)

[Narrative summaries of sessions]
```

---

**Command**: `/start-session`
**Tokens**: ~7k (vs 50k full context)
**Savings**: 96% reduction
**Auto-Archive**: Maintains session-index.md < 150 lines permanently
