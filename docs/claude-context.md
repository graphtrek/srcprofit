# Claude Context - SrcProfit

**Last Updated**: 2025-10-21 (Session 1 - Migration)
**Project**: SrcProfit Options Trading Tracker
**Phase**: Phase 1 - Workflow Migration
**Focus**: Migrating proven workflows from contrarian project

---

## 🎯 Project Overview

**SrcProfit** is a Spring Boot 3.5.6 application built with Java 24 for tracking and analyzing options trading positions. It integrates with multiple financial data providers (Alpaca, Interactive Brokers/IBKR, Alpha Vintage) to fetch market data, track net asset values, and calculate trading metrics like daily premium and annualized ROI.

### Tech Stack
- **Java 24** with Spring Boot 3.5.6
- **Database**: PostgreSQL 15 (via JPA/Hibernate)
- **Templating**: JTE (Java Template Engine) for server-side rendering
- **Build Tool**: Maven (`./mvnw` wrapper)
- **Containerization**: Docker with multi-stage builds
- **Virtual Threads**: Enabled for improved concurrency

### Key Features
- Options position tracking (PUT/CALL with OPEN/CLOSED status)
- Multi-source market data (Alpaca, IBKR, Alpha Vintage)
- Financial calculations (Annualized ROI using Black-Scholes, P&L analysis)
- Portfolio analytics (Net Asset Value tracking across accounts)
- Web UI (JTE templates with HTMX for dynamic updates)

---

## 🏗️ Architecture

### Package Structure
```
co.grtk.srcprofit/
├── config/         - RestClient and ObjectMapper configuration
├── controller/     - REST and MVC controllers for different data sources
├── dto/            - Data Transfer Objects for API responses
├── entity/         - JPA entities with Hibernate mappings
├── mapper/         - Conversion logic between entities and DTOs
├── repository/     - Spring Data JPA repositories
└── service/        - Business logic for data retrieval and processing
```

### Entity Hierarchy
- `BaseAsset` - `@MappedSuperclass` with common fields (tradeDate, quantity, positionValue, etc.)
- `OptionEntity` - Extends BaseAsset, adds option-specific fields (expirationDate, strike, status, type)
- `InstrumentEntity` - Stocks/tickers with earnings data
- `NetAssetValueEntity` - Daily portfolio valuation

### External Integrations
- **Alpaca API**: Stock/option quotes, market snapshots
- **IBKR**: Flex Web Service (CSV trades/NAV), watchlist, market data
- **Alpha Vintage**: Additional market data provider

---

## 📊 Current State

### Database
- **PostgreSQL 15** with 5 databases:
  - `srcprofit`, `srcprofit1`, `srcprofit2` (multi-tenant)
  - `moneypenny`, `stableips` (additional apps)
- **User**: `srcprofit` / `srcprofit`
- **Init script**: `init/init-db.sh` (auto-creates databases)

### Deployment
- **Docker Compose**: 3 app instances + PostgreSQL + pgAdmin
- **Ports**: 8080 (srcprofit_imre), 8081 (srcprofit_krisztian), 8082 (srcprofit_graphtrek)
- **GitHub Actions**: CI/CD pipeline with Docker image publishing to ghcr.io

### Git Status
- **Branch**: `claude` (just created for workflow migration)
- **Main branch**: `master`
- **Last commits**:
  - `23e8dbb` - docker-compose.yaml
  - `b284b88` - Add documentation and database initialization scripts
  - `0638ae6` - Fix: use default postgres user if not defined

---

## 📚 Knowledge Base (Building)

### Session 1 (Current)
- **Focus**: Migrating workflow processes from contrarian project
- **Goal**: Establish session tracking, issue tracking, quality protocols, agents
- **Status**: In progress - Phase 1 complete (branch + docs structure)

### Documentation Being Created
- Session State Transfer Protocol
- Definition of Done
- Quality Gates (4-tier strategy)
- Testing Strategy (Ground Truth TDD for options)
- Issue Tracking System ✅ (operational as of Session 2)
- Slash Commands (/start-session, /end-session, /commit, /ship)
- Agents (code-reviewer, trading-specialist, java-pro, etc.)

---

## 📋 Issue Tracking System

**Status**: ✅ Operational (migrated from contrarian/kaizen, Session 2)

### Quick Reference

**Create Issue**:
```bash
cp docs/issues/TEMPLATE.md docs/issues/ISSUE-XXX-brief-description.md
# Edit file: Status, Priority, Category, Problem, Approach, Success Criteria
python3 scripts/update_issue_index.py
```

**Close Issue**:
```bash
# Edit ISSUE-XXX.md: Update Status to CLOSED, add Completed date & Actual effort
python3 scripts/update_issue_index.py
```

**View Issues**:
```bash
cat docs/issues/README.md  # Auto-generated index
```

### Key Features

1. **Auto-Generated Index** (`docs/issues/README.md`):
   - Total issues count (Open/Closed/Partial percentages)
   - Issues grouped by priority (CRITICAL → LOW)
   - Issues grouped by category (Feature, Bug, Code Quality, etc.)
   - Quick links to all issue files
   - **Never edit manually** - script regenerates it

2. **Script** (`scripts/update_issue_index.py`):
   - Scans all `ISSUE-*.md` files in `docs/issues/`
   - Extracts metadata (Status, Priority, Category, etc.)
   - Generates statistics and categorization
   - **Auto-detects project name** from git remote URL (generic for all Kaizen projects)

3. **Template** (`docs/issues/TEMPLATE.md`):
   - Standard structure for all issues
   - Fields: Status, Priority, Category, Created, Completed, Actual effort
   - Sections: Problem, Root Cause, Approach, Success Criteria, Acceptance Tests, Related Issues

4. **Workflow Documentation** (`kaizen/docs/issue-tracking.md`):
   - Complete guide to issue lifecycle (OPEN → IN PROGRESS → CLOSED)
   - Priority guidelines (CRITICAL/HIGH/MEDIUM/LOW)
   - Category guidelines (Feature/Bug/Code Quality/etc.)
   - Best practices (DO/DON'T)
   - Integration with `/end-session` command

### Issue Lifecycle

```
OPEN → IN PROGRESS ← → BLOCKED (temporary) → CLOSED
```

**Statuses**:
- **OPEN**: Ready to work on, not started
- **IN PROGRESS**: Currently being worked on
- **BLOCKED**: Can't proceed (waiting on something)
- **CLOSED**: Completed and verified

### Integration with Workflow

**`/end-session` Command** (Step 5):
- Automatically runs `python3 scripts/update_issue_index.py`
- Keeps issue index current after every session
- No manual intervention required

**Issue Categories**:
- Feature - New functionality
- Bug - Something broken
- Code Quality / Technical Debt - Refactoring
- Documentation - Docs updates
- Testing / Quality Assurance - Test coverage
- Infrastructure - CI/CD, tooling
- Workflow / Process Improvement - Development workflow
- Developer Experience - Claude Code tools

### Current Issues

**Total**: 1 issue (as of Session 2)
- **ISSUE-001**: Add CALL Option Sell Obligation Display to Positions Page
  - Status: ✅ CLOSED (Session 2)
  - Priority: MEDIUM
  - Category: Feature
  - Actual effort: 2 hours

---

## 🚨 Critical Reminders

### Financial Calculations
- **Decimal Precision**: Use `Decimal` for all monetary values (never `double` for money)
- **Ground Truth**: Validate calculations against broker APIs (IBKR, Alpaca)
- **Black-Scholes**: Annualized ROI calculations use Apache Commons Math3
- **P&L Validation**: Compare against source system (IBKR Flex reports)

### Database
- **JPA open-in-view**: DISABLED - fetch all lazy relationships within transaction boundaries
- **Batch operations**: batch_size=200, fetch_size=50
- **DDL auto-update**: ENABLED (caution in production)
- **Connection pool**: HikariCP (20 max, 5 min idle)

### Development
- **Build**: `./mvnw clean install`
- **Run**: `./mvnw spring-boot:run`
- **Test**: `./mvnw test`
- **Docker**: `docker-compose --env-file docker-compose.env up`

---

## 📍 Quick Links

### Documentation (Being Created)
- Context: `docs/claude-context.md` (this file)
- Active context: `docs/claude-active-context.md`
- Session summaries: `docs/sessions/SESSION_XX_COMPLETE.md`
- Issues: `docs/issues/` (README.md auto-generated, TEMPLATE.md for new issues)
- Issue workflow: `kaizen/docs/issue-tracking.md`
- Workflows: `docs/workflow/`
- Scripts: `scripts/update_issue_index.py` (auto-gen issue index)

### Source Code
- Entities: `src/main/java/co/grtk/srcprofit/entity/`
- Services: `src/main/java/co/grtk/srcprofit/service/`
- Controllers: `src/main/java/co/grtk/srcprofit/controller/`
- Templates: `src/main/jte/`

### Configuration
- Application: `src/main/resources/application.yaml`
- Docker: `docker-compose.yaml`
- CI/CD: `.github/workflows/ci.yml`
- Project info: `CLAUDE.md` (root)

---

## 🔮 Next Steps

1. Complete workflow migration from contrarian
2. Set up first development session with new workflows
3. Start tracking issues and sessions systematically
4. Begin feature development with proven TDD methodology

---

**Version**: 1.0 (Session 1 - Migration from contrarian)
**Workflow Source**: Contrarian Trading Portfolio System (180+ sessions, proven patterns)
