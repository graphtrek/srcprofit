# Claude Agent Pointer - SrcProfit

> **START HERE**: Entry point for Claude Code sessions

---

## 🚀 Quick Start Commands

- `/start-session` - Daily work (7k tokens)
- `/full-context` - Major decisions (35k tokens)
- `/end-session` - Save session state
- `/ship` - Deploy to production

---

## 📍 Context Architecture

**Layered Context System** (96% token reduction):
- **Tier 1**: `CLAUDE.md` (this file) - Entry point (2k tokens)
- **Tier 2**: `docs/claude-active-context.md` - Daily context (5k tokens)
- **Tier 3**: `docs/claude-context.md` - Reference docs (20k tokens)
- **Tier 4**: Specific docs - On-demand deep dives

**How it works**: See `docs/context-architecture.md`

---

## 📚 Memory Locations

- **Primary branch**: `claude` (all workflow development)
- **Main branch**: `master` (production releases)
- **Context file**: `docs/claude-context.md`
- **Active context**: `docs/claude-active-context.md`
- **Knowledge base**: `docs/knowledge-base-index.md`
- **Session summaries**: `docs/sessions/SESSION_XX_COMPLETE.md`

---

## 🎯 Quality Protocols

**MANDATORY reading before any session handoff**:
- `docs/workflow/session-state-transfer-protocol.md` - Zero-degradation handoffs
- `docs/planning/definition-of-done.md` - Completion criteria

**These protocols prevent**:
- False "done" claims (saves 30min+ per session)
- API guessing failures (RTFM enforcement)
- Vague handoffs (next session starts immediately)
- Context waste (strategic planning)

---

## 🏗️ Project Overview

**SrcProfit** is a Spring Boot 3.5.6 application for tracking options trading positions.

### Tech Stack
- **Java 24** with Spring Boot 3.5.6
- **Database**: PostgreSQL 15 (JPA/Hibernate)
- **Templating**: JTE (Java Template Engine)
- **Build**: Maven (`./mvnw`)
- **Container**: Docker with multi-stage builds

### Key Features
- Options position tracking (PUT/CALL)
- Multi-source market data (Alpaca, IBKR, Alpha Vintage)
- Financial calculations (ROI using Black-Scholes, P&L analysis)
- Portfolio analytics (Net Asset Value tracking)
- Web UI (JTE + HTMX)

---

## 🔧 Development Workflow

### Daily Commands
```bash
# Build
./mvnw clean install

# Test
./mvnw test

# Run
./mvnw spring-boot:run

# Docker (all services)
docker-compose --env-file docker-compose.env up
```

### Git Workflow
```bash
# Work on claude branch
git checkout claude

# After PR merge
git pull origin master
git push origin claude
```

### Issue Tracking
```bash
# Create new issue
cp docs/issues/TEMPLATE.md docs/issues/ISSUE-XXX-brief-description.md
# Edit the issue file, then:
python3 scripts/update_issue_index.py

# View all issues
cat docs/issues/README.md

# Close an issue
# Update issue file Status to CLOSED, add Completed date, then:
python3 scripts/update_issue_index.py
```

**Key Points**:
- `docs/issues/README.md` is **auto-generated** - never edit manually
- Script auto-detects project name from git remote
- Full workflow guide: `kaizen/docs/issue-tracking.md`
- `/end-session` automatically updates issue index

---

## 📊 Quality Gates (4-Tier)

**Progressive validation** (fast iteration > perfect code during development):

- **TIER 0** (`/commit-wip`) - Emergency checkpoint (skips all hooks)
- **TIER 1** (`/commit`) - Fast TDD (tests only, <30s)
- **TIER 2** (`/commit-review`) - Review ready (format + tests + coverage, 2-3min)
- **TIER 3** (`/ship`) - Production (full CI + PR + merge, 5-10min)

---

## 🚨 Critical Lessons

### Always
- Push immediately after commit (team visibility)
- Run tests at every gate
- Validate financial calculations against broker data
- Use `Decimal` for monetary values (never `double`)
- Review BEFORE PR (not in PR comments)

### Never
- Skip Definition of Done criteria
- Guess API behavior (RTFM - Read The Manual first)
- Claim "done" with failing tests
- Commit directly to master
- Use optimistic completion percentages

---

## 💡 Trading Domain

**SrcProfit follows TastyTrade methodology** for options trading:
- **Strategies**: Premium selling, defined risk
- **P&L**: FIFO cost basis, real-time calculations
- **Risk**: Position sizing, portfolio heat
- **Ground Truth**: Validate against IBKR/Alpaca APIs

**Reference**: `docs/trading/` for methodology docs

---

## 📁 Project Structure

```
srcprofit/
├── CLAUDE.md                           # This file
├── docs/
│   ├── claude-context.md               # Full context
│   ├── claude-active-context.md        # Session state
│   ├── knowledge-base-index.md         # Resource catalog
│   ├── sessions/                       # Session history
│   ├── issues/                         # Issue tracking
│   │   ├── README.md                   # Auto-generated index
│   │   ├── TEMPLATE.md                 # Issue template
│   │   └── ISSUE-*.md                  # Individual issues
│   ├── workflow/                       # Process docs
│   ├── trading/                        # Trading methodology
│   └── architecture/                   # ADRs
├── kaizen/
│   └── docs/
│       └── issue-tracking.md           # Issue workflow guide
├── scripts/
│   └── update_issue_index.py           # Auto-gen issue README
├── .claude/
│   ├── commands/                       # Slash commands
│   └── agents/                         # Specialized agents
└── src/main/java/co/grtk/srcprofit/
    ├── entity/                         # JPA entities
    ├── repository/                     # Data access
    ├── service/                        # Business logic
    ├── controller/                     # REST/MVC endpoints
    └── config/                         # Spring config
```

---

## 🎓 Learning Resources

### Internal
- **Testing Strategy**: `docs/workflow/testing-strategy.md`
- **Quality Gates**: `docs/workflow/quality-gates.md`
- **Documentation Standards**: `docs/workflow/documentation-standards.md`
- **Issue Tracking Workflow**: `kaizen/docs/issue-tracking.md`
- **TastyTrade Methodology**: `docs/trading/tastytrade-*.md`

### External
- Spring Boot 3.5.6 docs
- JPA/Hibernate documentation
- IBKR API docs
- Alpaca API docs

---

## 🔄 Session Workflow

### Start Session
```
/start-session
→ Loads CLAUDE.md + claude-active-context.md + last session
→ ~7k tokens (96% reduction vs loading everything)
→ Ready to work immediately
```

### During Session
- Use TodoWrite tool for complex tasks
- Reference files by line number (file.java:42)
- Load specific docs only when needed

### End Session
```
/end-session
→ Creates SESSION_XX_COMPLETE.md
→ Updates claude-active-context.md
→ Updates session-index.md
→ Commits docs/
→ Ready for next session
```

---

## 📈 Success Metrics

After migration from contrarian project (180+ sessions), we have:
- ✅ Zero-degradation session handoffs
- ✅ 96% token reduction (7k vs 50k daily)
- ✅ Honest completion tracking
- ✅ Ground Truth TDD methodology
- ✅ 4-tier quality gates
- ✅ Issue tracking system
- ✅ Trading domain expertise

---

**Version**: 1.0 (Session 1 - Workflow Migration)
**Source**: Contrarian Trading Portfolio System (180+ sessions, proven)
**Last Updated**: 2025-10-21
