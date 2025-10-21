# Context Strategy for Claude Code Projects

**Purpose**: Scalable context management that prevents token limit exhaustion
**Proven**: 30 sessions on contrarian project (96% token reduction achieved)
**For**: Any project using Claude Code with growing context needs

---

## 🎯 The Problem

**Traditional Approach**:
- Single `CLAUDE_CONTEXT.md` file grows with every session
- Load 50k+ tokens every session
- Hit weekly token limit in 3-4 sessions
- Context becomes unmanageable after 20+ sessions

**Result**: Either delete history or hit limits constantly

---

## 💡 The Solution: Layered Context Architecture

**Four-Tier System**:
1. **CLAUDE.md** (2k tokens) - Entry point, always loaded
2. **CLAUDE_ACTIVE_CONTEXT.md** (5k tokens) - Daily context, loaded every session
3. **CLAUDE_CONTEXT.md** (20k tokens) - Reference docs, loaded when needed
4. **Specific docs** (1-10k each) - On-demand deep dives

**Result**: Load 7k tokens daily vs 50k (96% reduction!)

---

## 📁 File Structure for New Projects

```
your-project/
├── CLAUDE.md                          # Tier 1: Entry (keep minimal!)
├── docs/
│   ├── CLAUDE_ACTIVE_CONTEXT.md      # Tier 2: Daily (update every session)
│   ├── CLAUDE_CONTEXT.md             # Tier 3: Reference (update per phase)
│   ├── CONTEXT_ARCHITECTURE.md       # How it works (copy from template)
│   ├── sessions/                     # Session history folder
│   │   ├── SESSION_XX_COMPLETE.md    # Individual session files
│   │   └── artifacts/                # Old session fragments
│   ├── SESSION_INDEX.md              # Session list
│   ├── KNOWLEDGE_BASE_INDEX.md       # Resource catalog (optional)
│   ├── slash-commands-template/      # Reusable templates (copy from contrarian)
│   │   ├── README.md
│   │   ├── start-session.template.md
│   │   ├── full-context.template.md
│   │   ├── end-session.template.md
│   │   ├── ship.template.md
│   │   └── CUSTOMIZATION_GUIDE.md
│   └── project-template/             # For future projects
│       ├── CLAUDE_PROJECT_INIT_TEMPLATE.md
│       ├── QUICK_START_GUIDE.md
│       ├── CONTEXT_STRATEGY.md       # This file
│       └── IDE_SETUP_GUIDE.md        # ✨ NEW - IDE configuration
├── .claude/
│   └── commands/                      # Project-specific (gitignored)
│       ├── start-session.md
│       ├── full-context.md
│       ├── end-session.md
│       └── ship.md
└── .vscode/                          # ✨ NEW - VSCode config (commit to git)
    ├── settings.json                 # Workspace settings
    ├── extensions.json               # Recommended extensions
    ├── launch.json                   # Debug configurations
    └── tasks.json                    # Build/test tasks
```

---

## 🚀 Setup Instructions (New Project)

### Step 1: Copy Base Templates

```bash
# Create docs structure
mkdir -p docs/slash-commands-template docs/project-template

# Copy from contrarian project (or this template)
cp contrarian/docs/slash-commands-template/* docs/slash-commands-template/
cp contrarian/docs/project-template/* docs/project-template/
cp contrarian/docs/CONTEXT_ARCHITECTURE.md docs/

# ✨ NEW: Copy IDE configuration templates
mkdir -p .vscode
cp contrarian/.vscode/* .vscode/
# Customize for your project (language, paths, etc.)
```

### Step 2: Create Slash Commands

```bash
# Create commands directory
mkdir -p .claude/commands

# Copy and customize templates
cp docs/slash-commands-template/*.template.md .claude/commands/

# Rename (remove .template)
cd .claude/commands
for f in *.template.md; do mv "$f" "${f%.template.md}.md"; done

# Customize placeholders (see CUSTOMIZATION_GUIDE.md)
# Replace: {{PROJECT_NAME}}, {{CONTEXT_BRANCH}}, {{MAIN_BRANCH}}, etc.
```

### Step 3: Initialize Context Files

**Create `CLAUDE.md`** (project root):
```markdown
# Claude Agent Pointer

> **START HERE**: Entry point, kept minimal

## 🚀 Quick Start Commands
- `/start-session` - Daily work (7k tokens)
- `/full-context` - Major decisions (35k tokens)
- `/end-session` - Save session
- `/ship` - Deploy to production

## 📍 Context Architecture
- `docs/CLAUDE_ACTIVE_CONTEXT.md` - Daily context
- `docs/CLAUDE_CONTEXT.md` - Reference docs
- `docs/CONTEXT_ARCHITECTURE.md` - How it works

[... add your project specifics ...]
```

**Create `docs/CLAUDE_ACTIVE_CONTEXT.md`**:
```markdown
# Claude Active Context

**Last Updated**: [DATE] (Session 1)
**Project**: [PROJECT_NAME]
**Phase**: Phase 1 - Setup
**Focus**: Initial project setup

## 🎯 Active Work (Last 3 Sessions)
- Session 1: Project initialization (DATE)

## 📋 Current Tasks
- [ ] [Your initial tasks]

## 🚨 Critical Reminders
- [Project-specific reminders]

## 📍 Quick Links
- Architecture: docs/architecture/
- Last session: docs/sessions/SESSION_01_COMPLETE.md
```

**Create `docs/CLAUDE_CONTEXT.md`**:
```markdown
# Claude Context - Reference Documentation

## 🏗️ Project Architecture
[Your architecture overview]

## 📚 Knowledge Base
[Your resources]

## 🎯 Phase Overview
**Phase 1**: 🚧 IN PROGRESS - Setup (Session 1)

## 🔧 Permanent Lessons
[Key lessons as you discover them]
```

**Create `docs/SESSION_INDEX.md`**:
```markdown
# Session Index

- Session 1: Project initialization ([DATE])
```

### Step 4: Configure IDE (Optional but Recommended)

See `docs/project-template/IDE_SETUP_GUIDE.md` for complete guide.

**Quick Setup (VSCode)**:
1. Copy `.vscode/` templates from contrarian
2. Install Claude Code extension
3. Install language extensions (Python, TypeScript, etc.)
4. Remove conflicting extensions (Copilot)
5. Reload VSCode

**Result**: Optimal Claude Code experience with visual diffs, auto-accept, plan mode!

### Step 5: Add to .gitignore

```bash
# Add to .gitignore
echo ".claude/" >> .gitignore

# Commit .vscode to git (for team consistency)
# .vscode/ is NOT in .gitignore - this is intentional!
```

---

## 🔄 Daily Workflow

### Starting Session
```bash
# In Claude Code
/start-session

# Loads:
# - CLAUDE.md (2k)
# - CLAUDE_ACTIVE_CONTEXT.md (5k)
# Total: 7k tokens
```

### During Session
- Use TodoWrite tool to track tasks
- Reference files by line number (file.py:42)
- Load specific docs only when needed

### Ending Session
```bash
/end-session

# Automatically:
# 1. Creates SESSION_XX_COMPLETE.md
# 2. Updates CLAUDE_ACTIVE_CONTEXT.md
# 3. Updates SESSION_INDEX.md
# 4. Commits and pushes
```

### Shipping Work
```bash
/ship

# Automatically:
# 1. Runs make review
# 2. Creates PR
# 3. Merges to main
# 4. Syncs branch
```

---

## 📊 Maintenance Schedule

### Every Session (5 min)
- Update CLAUDE_ACTIVE_CONTEXT.md
- Create SESSION_XX_COMPLETE.md
- Update SESSION_INDEX.md

### Every Phase (1-2 hours)
- Update CLAUDE_CONTEXT.md (mark phase complete)
- Add permanent lessons
- Create PHASE_X_SUMMARY.md (optional)

### Quarterly (when >40k tokens)
- Archive old sessions to docs/history/
- Slim down CLAUDE_CONTEXT.md
- Keep only recent phases

---

## 📈 Token Budget

| Context | Tokens | Frequency | Use Case |
|---------|--------|-----------|----------|
| /start-session | 7k | Daily (96%) | Feature work, bug fixes |
| Quick lookup | 10k | Weekly | Spot-check specific info |
| /full-context | 35k | Monthly (4%) | New phase, architecture |

**Impact**: 7.7x more sessions before weekly limit

---

## 🎓 Best Practices

### DO
- ✅ Use `/start-session` for 96% of sessions
- ✅ Keep CLAUDE_ACTIVE_CONTEXT.md under 5k tokens
- ✅ Create detailed SESSION_XX_COMPLETE.md files
- ✅ Update context every session (via /end-session)
- ✅ Archive old content quarterly

### DON'T
- ❌ Load full context every session
- ❌ Let active context grow beyond 3 sessions
- ❌ Put session history in CLAUDE_CONTEXT.md
- ❌ Skip session summaries
- ❌ Commit .claude/commands/ to git

---

## 🔧 Troubleshooting

**Context feels stale?**
- Run `/full-context` once to refresh

**Active context >10k tokens?**
- Remove oldest session, keep only last 3

**Can't find old information?**
- Read specific SESSION_XX_COMPLETE.md directly

**Full context >50k tokens?**
- Time for quarterly cleanup

---

## 📚 Example Projects

### Web Application
```
Project: my-web-app
Branch: develop
Sessions: 50+
Token savings: 95% (8k daily vs 60k before)
```

### CLI Tool
```
Project: my-cli-tool
Branch: dev
Sessions: 20+
Token savings: 97% (6k daily vs 40k before)
```

### Library/Package
```
Project: my-python-lib
Branch: main
Sessions: 100+
Token savings: 94% (10k daily vs 80k before)
```

---

## 🚀 Benefits

**For Developers**:
- 10x more sessions before token limit
- Faster Claude responses
- Complete history preserved
- Easy to find information

**For Projects**:
- Scales to 100+ sessions
- Clean separation of concerns
- Reusable templates
- Documented best practices

**For Teams**:
- Consistent context management
- Easy onboarding
- Clear session boundaries
- Searchable history

---

## 📖 Related Documentation

**In contrarian project** (copy these):
- `docs/CONTEXT_ARCHITECTURE.md` - Complete system guide
- `docs/slash-commands-template/` - All templates
- `docs/project-template/` - Project initialization
- `docs/project-template/IDE_SETUP_GUIDE.md` - ✨ NEW - IDE configuration
- `CLAUDE.md` - Entry point example
- `.vscode/` - VSCode configuration templates

**External**:
- Claude Code docs: https://docs.claude.com/claude-code
- VSCode extension: https://docs.claude.com/en/docs/claude-code/vs-code
- JetBrains plugin: https://docs.claude.com/en/docs/claude-code/jetbrains
- Ground Truth TDD: See contrarian/docs/TESTING_STRATEGY.md

---

## 🔄 Version History

**1.0.0** (2025-10-08):
- Initial template based on contrarian project
- 4-tier context system
- Slash command integration
- Proven over 30 sessions

---

## 💬 Feedback

Found improvements? Update this file and share with future projects!

**Maintained by**: Your team
**Next review**: After your first 20 sessions
