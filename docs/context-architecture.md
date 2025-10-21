# Context Architecture - Scalable Knowledge Management

**Version**: 1.0.0
**Last Updated**: 2025-10-08 (Session 30)
**Purpose**: Prevent token limit exhaustion while maintaining full project context

---

## 🎯 The Problem We Solve

**Before**: Loading complete context every session
- `CLAUDE_CONTEXT.md`: 1,704 lines ≈ 50k tokens
- `KNOWLEDGE_BASE_INDEX.md`: 900 lines ≈ 4k tokens
- **Total**: ~54k tokens per session
- **Result**: Hit weekly limit in 3 days (3-4 sessions)

**After**: Layered context architecture
- Daily work: ~7k tokens (96% reduction)
- Research: ~17k tokens (70% reduction)
- Major decisions: ~35k tokens (35% reduction)
- **Result**: 10x more sessions before hitting limit

---

## 🏗️ Four-Tier Architecture

### Tier 1: CLAUDE.md (Entry Point)
**File**: `CLAUDE.md` (in project root)
**Size**: ~2k tokens
**Load**: ALWAYS (first file read)

**Contents**:
- Slash command quick reference
- Context file pointers
- Workflow reminders (git, testing, quality gates)
- Critical lessons (RTFM, smart sync, etc.)
- Project basics (branches, architecture, tools)

**Updated**: Rarely (only for workflow changes)

**Purpose**: Fast orientation - "where am I and what do I need?"

---

### Tier 2: CLAUDE_ACTIVE_CONTEXT.md (Daily Context)
**File**: `docs/CLAUDE_ACTIVE_CONTEXT.md`
**Size**: ~5k tokens
**Load**: EVERY session start (via `/start-session`)

**Contents**:
- Current phase and focus
- Last 3 sessions summary (not 30!)
- Active tasks (this sprint/week)
- Critical reminders (frequently needed)
- Quick links to detailed docs

**Updated**: EVERY session (via `/end-session`)

**Maintenance**:
```markdown
## Every Session:
1. Add new session to "Active Work" (keep last 3)
2. Remove oldest session if >3 listed
3. Update "Current Focus" for next session
4. Update "Current Tasks" (mark done, add new)
5. Refresh critical reminders if new lesson learned

## Keep It Small:
- Max 3 sessions in history
- Only current sprint tasks
- Only frequently-used reminders
- No detailed explanations (link to docs)
```

**Purpose**: Everything needed for daily work, nothing more

---

### Tier 3: CLAUDE_CONTEXT.md (Reference Documentation)
**File**: `docs/CLAUDE_CONTEXT.md`
**Size**: ~20k tokens (refactored from 50k)
**Load**: When starting new phase or making major decisions (via `/full-context`)

**Contents**:
- Architecture overview (stable)
- Knowledge base references (TastyTrade docs, API specs, subagents)
- Phase summaries (not session-by-session history!)
- Permanent lessons (key discoveries that apply long-term)
- Links to detailed documentation

**Updated**: Per phase completion or major discoveries

**Maintenance**:
```markdown
## Every Phase (not every session):
1. Mark phase complete in "Phase Overview"
2. Add key lessons to "Permanent Lessons" section
3. Archive old phase details to PHASE_X_SUMMARY.md
4. Keep focused on phases ± 1 from current

## Quarterly (when >40k tokens):
1. Move completed phases to docs/history/
2. Keep only active + next 2 phases
3. Ensure all details preserved in session files
```

**Purpose**: Stable reference material that changes slowly

---

### Tier 4: Specific Documents (On-Demand)
**Files**: Various `docs/**/*.md`
**Size**: 1k-10k tokens each
**Load**: Only when specific information needed

**Includes**:
- `SESSION_XX_COMPLETE.md` - Detailed session history
- `SESSION_INDEX.md` - Chronological session list
- `KNOWLEDGE_BASE_INDEX.md` - Resource catalog
- `docs/architecture/ADR-*.md` - Architecture decisions
- `docs/tastytrade/` - Trading methodology (166K lines)
- `docs/api/` - API documentation (38 pages)

**Updated**: As created/modified

**Purpose**: Historical records and deep reference material

---

## 🔄 Maintenance Workflows

### Every Session (5-10 minutes)

**Use**: `/end-session` slash command (automates this)

**Manual steps**:

#### 1. Create Session Summary
File: `docs/SESSION_XX_COMPLETE.md`

Template:
```markdown
# Session XX Complete - [BRIEF_TITLE]

**Date**: YYYY-MM-DD
**Duration**: X hours
**Status**: ✅ COMPLETE / 🚧 IN PROGRESS / ❌ BLOCKED

## 🎯 Mission Accomplished
[1-2 sentence summary]

## ✅ What Was Completed
- [Achievement 1]
- [Achievement 2]

## 🐛 Bugs Fixed (if any)
### Bug 1: [Title]
**Symptom**: [Description]
**Root Cause**: [Why it happened]
**Fix**: [What was changed]
**Files**: [List]

## 📊 Impact
- Tests: XXX passing
- Coverage: XX%
- Performance: [Metrics if relevant]
- Files changed: X

## 📚 Key Lessons
1. [Lesson that applies to future work]
2. [Mistake to avoid]

## 🔮 Next Session
- [ ] [Next task]
- [ ] [Next task]
```

#### 2. Update Active Context
File: `docs/CLAUDE_ACTIVE_CONTEXT.md`

Changes:
```diff
- **Current Focus**: Transaction sync (Session 29 complete)
+ **Current Focus**: Orders table implementation (Session 30 complete)

  ## 🎯 Active Work (Last 3 Sessions)
+ - Session 30: Orders table implemented (7 endpoints, 405 tests)
  - Session 29: Transaction pagination bug FIXED
  - Session 28: Live integration tests + CLI improvements
- - Session 27: E2E tests + storage coverage
```

#### 3. Update Session Index
File: `docs/SESSION_INDEX.md`

Add one line:
```markdown
- Session 30: Orders table implementation complete (2025-10-08)
```

#### 4. Commit and Push
```bash
make test                    # Verify all passing
git add .
git commit -m "docs: Update context for Session 30 (Session 29 complete)"
git push
```

---

### Every Phase (1-2 hours, quarterly)

**When**: Phase transition (e.g., Phase 2C → Phase 3)

#### 1. Update CLAUDE_CONTEXT.md
```markdown
## Phase Overview
**Phase 2C**: ✅ COMPLETE - Transaction Sync (Sessions 28-30)
**Phase 3**: 🚧 IN PROGRESS - Terminal UI (Sessions 31-XX)
```

#### 2. Add Permanent Lessons
```markdown
## 🔧 Permanent Lessons
- RTFM saves hours (Session 29: read API docs first!)
- Smart sync handles squash merges (Session 20: auto-detect)
- Ground Truth TDD prevents bugs (Phase 1: test vs broker data)
```

#### 3. Create Phase Summary (optional)
File: `docs/PHASE_2C_SUMMARY.md`
```markdown
# Phase 2C Summary - Transaction Sync

**Duration**: 3 sessions (28-30)
**Goal**: 100% transaction sync with PostgreSQL

## Achievements
- [Key accomplishments]

## Metrics
- [Test coverage, performance, etc.]

## Lessons
- [What we learned]
```

---

### Quarterly Cleanup (when context >40k tokens)

**When**: Every 3 months or when CLAUDE_CONTEXT.md >40k tokens

#### 1. Archive Old Sessions
Create: `docs/history/SESSIONS_01-30.md`
```markdown
# Sessions 1-30 Archive

## Phase 1: Foundation (Sessions 1-16)
- [Condensed summary]

## Phase 2A: Storage (Sessions 17-21)
- [Condensed summary]

## Phase 2B: Tests (Sessions 22-27)
- [Condensed summary]

## Phase 2C: Transaction Sync (Sessions 28-30)
- [Condensed summary]
```

#### 2. Update SESSION_INDEX.md
```markdown
# Session Index

**Recent Sessions** (full details in docs/):
- Session 40: [Summary] (2025-11-15)
- Session 39: [Summary] (2025-11-14)
...
- Session 31: [Summary] (2025-10-10)

**Archived Sessions** (see docs/history/):
- Sessions 1-30: See docs/history/SESSIONS_01-30.md
```

#### 3. Slim Down CLAUDE_CONTEXT.md
```markdown
## 🎯 Phase Overview

**Recent Phases**:
- Phase 3: ✅ COMPLETE - Terminal UI (Sessions 31-35)
- Phase 4: 🚧 IN PROGRESS - Web API (Sessions 36-XX)

**Earlier Phases** (see docs/history/PHASE_1_SUMMARY.md):
- Phase 1: Foundation (Sessions 1-16)
- Phase 2: Storage & Testing (Sessions 17-30)
```

---

## 📊 Token Budget

| Tier | File | Tokens | Frequency | Use Case |
|------|------|--------|-----------|----------|
| 1 | CLAUDE.md | 2k | Always | Entry point |
| 2 | CLAUDE_ACTIVE_CONTEXT.md | 5k | Daily | Current work |
| 3 | CLAUDE_CONTEXT.md | 20k | Per phase | Architecture |
| 4 | Specific docs | 1-10k | As needed | Deep dive |

**Typical Sessions**:
- `/start-session`: 2k + 5k = **7k tokens** (96% reduction!)
- Quick research: 2k + 5k + 3k = **10k tokens**
- `/full-context`: 2k + 5k + 20k + 5k = **32k tokens**

**Before**: Every session = 54k tokens
**After**: Most sessions = 7k tokens
**Impact**: **7.7x more sessions** before weekly limit

---

## 🎯 Slash Command Integration

Commands automatically load appropriate context tiers:

### `/start-session` - Daily Work
```markdown
Loads:
- CLAUDE.md (2k)
- CLAUDE_ACTIVE_CONTEXT.md (5k)
- Git status (1k)

Total: ~7k tokens
```

### `/full-context` - Major Decisions
```markdown
Loads:
- CLAUDE.md (2k)
- CLAUDE_ACTIVE_CONTEXT.md (5k)
- CLAUDE_CONTEXT.md (20k)
- Last 3 SESSION_XX_COMPLETE.md (5k)
- Git status (1k)

Total: ~33k tokens
```

### `/end-session` - Save Work
```markdown
Updates:
- Creates SESSION_XX_COMPLETE.md
- Updates CLAUDE_ACTIVE_CONTEXT.md (3 changes)
- Updates SESSION_INDEX.md (1 line)
- Commits and pushes

Context loaded: ~2k tokens (minimal)
```

### `/ship` - Deploy
```markdown
Runs:
- make review (quality gates)
- make pr (create PR)
- Merge to main
- make sync (smart sync)

Context loaded: ~5k tokens
```

---

## 🔧 Troubleshooting

### "Context feels stale"
**Symptom**: Claude doesn't know about recent work
**Solution**: Run `/full-context` once to refresh understanding
**Prevention**: Update CLAUDE_ACTIVE_CONTEXT.md every session

### "Active context >10k tokens"
**Symptom**: CLAUDE_ACTIVE_CONTEXT.md growing too large
**Solution**: Keep only last 3 sessions, archive older ones
**Prevention**: Remove oldest session when adding new one

### "Can't find specific information"
**Symptom**: Need details from old session
**Solution**: Read specific `SESSION_XX_COMPLETE.md` directly
**Note**: Full history always preserved, just not loaded by default

### "Full context >50k tokens"
**Symptom**: CLAUDE_CONTEXT.md too large
**Solution**: Time for quarterly cleanup
**Action**: Archive old phases to `docs/history/`

---

## 📚 File Structure

```
contrarian/
├── CLAUDE.md                          # Tier 1: Entry (2k, always)
├── docs/
│   ├── CLAUDE_ACTIVE_CONTEXT.md      # Tier 2: Daily (5k, every session)
│   ├── CLAUDE_CONTEXT.md             # Tier 3: Reference (20k, per phase)
│   ├── CONTEXT_ARCHITECTURE.md       # This file (how it works)
│   ├── SESSION_INDEX.md              # Session list
│   ├── SESSION_XX_COMPLETE.md        # Tier 4: Session history
│   ├── KNOWLEDGE_BASE_INDEX.md       # Tier 4: Resource catalog
│   ├── history/                      # Archived content
│   │   ├── SESSIONS_01-30.md
│   │   ├── PHASE_1_SUMMARY.md
│   │   └── PHASE_2_SUMMARY.md
│   ├── slash-commands-template/      # Reusable templates (in git)
│   │   ├── README.md
│   │   ├── start-session.template.md
│   │   ├── full-context.template.md
│   │   ├── end-session.template.md
│   │   ├── ship.template.md
│   │   └── CUSTOMIZATION_GUIDE.md
│   └── project-template/             # Project initialization
│       ├── CLAUDE_PROJECT_INIT_TEMPLATE.md
│       ├── QUICK_START_GUIDE.md
│       └── CONTEXT_STRATEGY.md
└── .claude/
    └── commands/                      # Project-specific (gitignored)
        ├── start-session.md
        ├── full-context.md
        ├── end-session.md
        └── ship.md
```

---

## 🎓 Best Practices

### DO
- ✅ Use `/start-session` for 96% of sessions
- ✅ Keep CLAUDE_ACTIVE_CONTEXT.md under 5k tokens
- ✅ Update context every session (via `/end-session`)
- ✅ Create detailed SESSION_XX_COMPLETE.md files
- ✅ Use `/full-context` when starting new phases
- ✅ Archive old content quarterly

### DON'T
- ❌ Load full context every session (wastes tokens)
- ❌ Let active context grow beyond 3 sessions
- ❌ Skip session summaries (you'll forget details)
- ❌ Put session history in CLAUDE_CONTEXT.md
- ❌ Delete old sessions (archive instead)
- ❌ Commit `.claude/commands/` to git (too project-specific)

---

## 🚀 Benefits

**For Developers**:
- 10x more sessions before hitting weekly token limit
- Faster Claude responses (less to process)
- Complete history always preserved
- Easy to find specific information

**For Projects**:
- Scales to 100+ sessions without bloat
- Clean separation of current vs historical context
- Reusable templates for future projects
- Documented best practices

**For Teams**:
- Consistent context management
- Easy onboarding (read active context)
- Clear session boundaries
- Searchable history

---

## 📖 Related Documentation

- `docs/slash-commands-template/README.md` - Slash command templates
- `docs/project-template/CONTEXT_STRATEGY.md` - Template for new projects
- `CLAUDE.md` - Project entry point
- `docs/KNOWLEDGE_BASE_INDEX.md` - Complete resource catalog

---

## 🔄 Version History

**1.0.0** (2025-10-08, Session 30):
- Initial architecture design
- 4-tier context system
- Slash command integration
- Maintenance workflows
- Proven on 30 sessions of contrarian project

---

**Next Review**: After 50 sessions or when patterns change
**Feedback**: Open issue or update this doc with improvements
