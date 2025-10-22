# Issue Tracking - SrcProfit

**Purpose**: Lightweight file-based issue tracking for development
**Last Updated**: 2025-10-21 (Session 1)

---

## 📊 Summary Statistics

- **Total Issues**: 1
- **Open**: 0
- **In Progress**: 0
- **Closed**: 1

---

## 🎯 Active Issues

### Critical Priority
*No critical issues*

### High Priority
*No high priority issues*

### Medium Priority
*No medium priority issues*

### Low Priority
*No low priority issues*

---

## ✅ Recently Closed Issues

### ISSUE-001: Add CALL Option Sell Obligation Display to Positions Page
**Status**: ✅ CLOSED
**Completed**: 2025-10-21 (Session 2)
**Category**: Feature
**Priority**: Medium
**Estimated**: 1.5 hours
**Actual**: 2 hours
**File**: [ISSUE-001-call-option-sell-obligations.md](ISSUE-001-call-option-sell-obligations.md)

**Summary**: Implemented 3 new CALL obligation cards on positions page mirroring PUT structure with coverage status indicators and percentage calculations.

---

## 📋 Issue Template

**Template File**: `docs/issues/ISSUE-TEMPLATE.md`

To create a new issue:
1. Copy `ISSUE-TEMPLATE.md` to `ISSUE-XXX-short-description.md`
2. Fill in all metadata and sections
3. Update this README with the new issue

---

## 🏷️ Issue Categories

### Feature
New functionality or enhancement to existing features

### Bug
Something that's broken or not working as expected

### Infrastructure
Build system, CI/CD, deployment, tooling

### Testing
Test coverage, test infrastructure, test data

### Documentation
Documentation updates, ADRs, guides

---

## 📈 Issue Workflow

### Creating Issues
1. Copy template above
2. Assign next available ISSUE-XXX number
3. Fill in all metadata fields
4. Save as `docs/issues/ISSUE-XXX-short-description.md`
5. Update this README (can be done manually or automatically)

### Working on Issues
1. Update Status to PARTIAL when starting
2. Add progress notes in issue file
3. Reference issue in commit messages: `feat: implement X (ISSUE-001)`

### Closing Issues
1. Fill in "Resolution" section
2. Update Status to CLOSED
3. Add Completed date
4. Add Actual time spent
5. Update this README

---

## 🔍 Finding Issues

### By Priority
```bash
grep -l "Priority: CRITICAL" docs/issues/ISSUE-*.md
grep -l "Priority: HIGH" docs/issues/ISSUE-*.md
```

### By Status
```bash
grep -l "Status: OPEN" docs/issues/ISSUE-*.md
grep -l "Status: PARTIAL" docs/issues/ISSUE-*.md
grep -l "Status: CLOSED" docs/issues/ISSUE-*.md
```

### By Category
```bash
grep -l "Category: Feature" docs/issues/ISSUE-*.md
grep -l "Category: Bug" docs/issues/ISSUE-*.md
```

---

## 📊 Issue Numbering

- **ISSUE-001** through **ISSUE-099**: Foundation/Infrastructure
- **ISSUE-100** through **ISSUE-199**: Core Features
- **ISSUE-200** through **ISSUE-299**: Enhancements
- **ISSUE-300** through **ISSUE-399**: Bugs
- **ISSUE-400+**: Future use

---

## 🔄 Maintenance

### Update Schedule
- **After creating issue**: Add to this README
- **After closing issue**: Update statistics
- **Every 10 issues**: Review and clean up

### Archive Policy
- Closed issues remain in `docs/issues/` permanently
- This README shows recently closed issues (configurable limit)
- Archive older closed issues to CHANGELOG.md when list grows large

---

## 📚 Related Documentation

- **Session Index**: `docs/workflow/session-index.md`
- **Quality Protocols**: `docs/workflow/session-state-transfer-protocol.md`
- **Knowledge Base**: `docs/knowledge-base-index.md`

---

**Version**: 1.1 (Session 2)
**Last Issue**: ISSUE-001 (closed)
