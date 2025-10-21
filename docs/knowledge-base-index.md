# Knowledge Base Index - SrcProfit

**Purpose**: Central catalog of all documentation and resources
**Last Updated**: 2025-10-21 (Session 1)

---

## 📚 Core Documentation

### Context Management
- **CLAUDE.md** - Entry point for Claude Code sessions
- **docs/claude-context.md** - Full project context and architecture
- **docs/claude-active-context.md** - Current session state and focus
- **docs/context-architecture.md** - How the layered context system works

### Quality Protocols
- **docs/workflow/session-state-transfer-protocol.md** - Zero-degradation handoffs
- **docs/planning/definition-of-done.md** - Completion criteria
- **docs/workflow/quality-gates.md** - 4-tier quality validation (to be created)

### Development Workflow
- **docs/workflow/testing-strategy.md** - Ground Truth TDD for options trading (to be created)
- **docs/workflow/documentation-standards.md** - ADRs, session summaries (to be created)

---

## 🎯 Session Management

### Session Summaries
- **docs/workflow/session-index.md** - Complete session list
- **docs/sessions/SESSION_01_COMPLETE.md** - Workflow migration (this session)
- *Future sessions will be added here*

### Session Commands
- **/.claude/commands/start-session.md** - Begin daily work (7k tokens)
- **/.claude/commands/end-session.md** - Save session state
- **/.claude/commands/full-context.md** - Load everything (35k tokens)
- **/.claude/commands/commit.md** - Fast TDD commit (TIER 1)
- **/.claude/commands/commit-review.md** - Review-ready commit (TIER 2)
- **/.claude/commands/commit-wip.md** - Emergency checkpoint (TIER 0)
- **/.claude/commands/ship.md** - Production deployment (TIER 3)

---

## 🤖 Specialized Agents

### Technical Agents
- **.claude/agents/code-reviewer.md** - Java/Spring Boot code review
- **.claude/agents/java-pro.md** - Spring Boot 3, Java 24 expertise
- **.claude/agents/jpa-specialist.md** - Hibernate, entities, repositories
- **.claude/agents/test-automator.md** - JUnit 5, AssertJ, Mockito
- **.claude/agents/database-administrator.md** - PostgreSQL, JPA
- **.claude/agents/api-integration-specialist.md** - REST clients, external APIs
- **.claude/agents/documentation-generator.md** - JavaDoc, Markdown

### Trading Domain Agents
- **.claude/agents/trading-specialist.md** - Options trading expertise
- **.claude/agents/fact-checker.md** - Ground truth validation

---

## 📊 Issue Tracking

### Issue Management
- **docs/issues/README.md** - Issue index and statistics
- **docs/issues/ISSUE-XXX-description.md** - Individual issues

### Issue Template
```markdown
# ISSUE-XXX: Title

**Created**: YYYY-MM-DD (Session XX)
**Status**: OPEN | PARTIAL | CLOSED
**Priority**: CRITICAL | HIGH | MEDIUM | LOW
**Category**: Feature | Bug | Infrastructure | Testing | Documentation
**Estimated**: X hours/sessions
```

---

## 💡 Trading Domain Knowledge

### TastyTrade Methodology
- **docs/trading/tastytrade-data-sources.md** - Data source documentation
- **docs/trading/tastytrade-cli-feature-gap-analysis.md** - Feature comparison
- **docs/trading/tastytrade-cli-option-analysis.md** - Options analysis patterns
- **docs/trading/tastytrade-cli-porting-strategy.md** - Integration patterns
- *Additional trading docs to be migrated*

### Options Trading Concepts
- Premium selling strategies
- P&L calculations (FIFO cost basis)
- Risk management (position sizing, portfolio heat)
- Greeks (delta, theta, gamma, vega)
- Black-Scholes pricing

---

## 🏗️ Architecture

### Architecture Decision Records (ADRs)
- **docs/architecture/** - All ADRs stored here
- *ADRs to be created as architectural decisions are made*

### ADR Template
```markdown
# ADR-NNN: [Decision Title]

**Status**: Proposed | Accepted | Deprecated | Superseded
**Date**: YYYY-MM-DD
**Decision Makers**: [Who decided]

## Context
## Decision
## Consequences
## Alternatives Considered
```

---

## 🔧 Technical Reference

### SrcProfit Architecture
- **Package Structure**: `docs/claude-context.md` → Architecture section
- **Entity Model**: JPA entities (BaseAsset, OptionEntity, InstrumentEntity)
- **API Integrations**: Alpaca, IBKR, Alpha Vintage
- **Database**: PostgreSQL 15, multi-tenant (5 databases)

### Technology Stack
- **Java 24** with Spring Boot 3.5.6
- **JPA/Hibernate** for data access
- **JTE** (Java Template Engine) for UI
- **Maven** build tool (`./mvnw`)
- **Docker** multi-stage builds

### External APIs
- **Alpaca**: Stock/option quotes, market snapshots
- **IBKR**: Flex Web Service (trades/NAV), market data
- **Alpha Vintage**: Additional market data

---

## 📖 Project Templates

### Reusable Templates
- **docs/project-template/claude-project-init-template.md** - Project initialization
- **docs/project-template/context-strategy.md** - Context management strategy
- **docs/project-template/quick-start-guide.md** - Daily workflow guide
- **docs/project-template/migration-from-contrarian.md** - This migration

---

## 🎓 Learning Resources

### Internal Documentation
- Testing strategy for Ground Truth TDD
- Quality gates documentation (4-tier)
- Session workflow optimization
- Trading methodology

### External Resources
- [Spring Boot 3.5.6 Documentation](https://docs.spring.io/spring-boot/docs/3.5.6/reference/html/)
- [JPA/Hibernate Documentation](https://hibernate.org/orm/documentation/)
- [IBKR API Documentation](https://www.interactivebrokers.com/api/)
- [Alpaca API Documentation](https://alpaca.markets/docs/)
- [TastyTrade Platform](https://www.tastytrade.com/)

---

## 🔍 Quick Reference

### Common Commands
```bash
# Build
./mvnw clean install

# Test
./mvnw test

# Run
./mvnw spring-boot:run

# Docker
docker-compose --env-file docker-compose.env up

# Git
git checkout claude      # Work branch
git checkout master      # Production branch
```

### File Locations
- **Source code**: `src/main/java/co/grtk/srcprofit/`
- **Tests**: `src/test/java/co/grtk/srcprofit/`
- **Templates**: `src/main/jte/`
- **Config**: `src/main/resources/application.yaml`
- **Docker**: `docker-compose.yaml`, `init/init-db.sh`
- **CI/CD**: `.github/workflows/ci.yml`

---

## 📈 Metrics & Status

### Current Stats (Session 1)
- **Sessions**: 1 (migration in progress)
- **Issues**: 0 (tracking system being set up)
- **Agents**: 0 (to be migrated)
- **Tests**: Existing (via GitHub Actions CI)
- **Documentation**: Building foundation

### Goals
- Establish proven workflow from 180+ sessions (contrarian)
- 96% token reduction with layered context
- Zero-degradation session handoffs
- Ground Truth TDD for financial calculations
- Complete trading domain knowledge

---

## 🔄 Maintenance

### Update Schedule
- **Every session**: Update claude-active-context.md
- **Every session**: Add new SESSION_XX_COMPLETE.md
- **As needed**: Update this index with new resources
- **Quarterly**: Review and archive old sessions

### Version History
- **v1.0** (2025-10-21, Session 1): Initial index created during workflow migration

---

**Last Updated**: 2025-10-21 (Session 1)
**Maintained By**: Claude Code with user oversight
**Next Review**: After Session 5
