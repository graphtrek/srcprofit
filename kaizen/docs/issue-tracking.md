# Issue Tracking Workflow

**Purpose**: Document how to create, manage, and close issues in this project.

---

## Quick Start

### 1. Create a New Issue

```bash
# Copy the template
cp docs/issues/TEMPLATE.md docs/issues/ISSUE-XXX-brief-description.md

# Edit the file and fill in:
# - Status: OPEN
# - Priority: CRITICAL | HIGH | MEDIUM | LOW
# - Category: Feature | Bug | Code Quality | Documentation | Testing | Infrastructure
# - Problem, Approach, Success Criteria

# Update the index
python3 scripts/update_issue_index.py
```

### 2. Work on an Issue

```bash
# Update Status in the issue file as you progress:
# - OPEN → IN PROGRESS → CLOSED
# - Or: BLOCKED (if stuck)

# When done, update:
# - **Status**: CLOSED
# - **Completed**: YYYY-MM-DD (Session XXX)
# - **Actual Effort**: X hours

# Regenerate index
python3 scripts/update_issue_index.py
```

### 3. View All Issues

```bash
# Open auto-generated index
cat docs/issues/README.md

# Or browse by priority
grep "CRITICAL\|HIGH" docs/issues/*.md
```

---

## Issue Lifecycle

```
OPEN
  ↓
IN PROGRESS ← → BLOCKED (temporary)
  ↓
CLOSED
```

**Status definitions**:
- **OPEN**: Ready to work on, not started
- **IN PROGRESS**: Currently being worked on
- **BLOCKED**: Can't proceed (waiting on something)
- **CLOSED**: Completed and verified

---

## Issue Template Structure

```markdown
# ISSUE-NNN: [Brief Description]

**Created**: YYYY-MM-DD (Session NNN)
**Status**: OPEN | IN PROGRESS | BLOCKED | CLOSED
**Priority**: CRITICAL | HIGH | MEDIUM | LOW
**Category**: [Feature | Bug | Code Quality | etc.]

## Problem
[What's wrong or what's needed?]

## Approach
[How will we solve it?]

## Success Criteria
- [ ] Criterion 1
- [ ] Criterion 2
```

---

## Priority Guidelines

| Priority | Description | Examples |
|----------|-------------|----------|
| **CRITICAL** | Blocks development, production broken | API broken, data loss, security issue |
| **HIGH** | Important feature, affects users | New feature request, major bug |
| **MEDIUM** | Nice to have, improves quality | Refactoring, optimization |
| **LOW** | Can wait, minor improvement | Documentation, small UI tweak |

---

## Category Guidelines

| Category | Description |
|----------|-------------|
| **Feature** | New functionality |
| **Bug** | Something broken |
| **Code Quality** | Refactoring, technical debt |
| **Documentation** | Docs updates |
| **Testing** | Test coverage, quality assurance |
| **Infrastructure** | CI/CD, tooling, workflow |
| **Developer Experience** | Claude Code tools, templates |

---

## Auto-Generated Index

The `docs/issues/README.md` file is **auto-generated** by `scripts/update_issue_index.py`.

**Never edit README.md manually!**

**When to regenerate**:
- After creating a new issue
- After closing an issue
- After changing issue Status/Priority
- At `/end-session` (automatic via Step 5)

**What it shows**:
- Total issue count
- Open/Closed/Partial breakdown
- Issues grouped by priority
- Issues grouped by category
- Quick links to all issues

---

## Best Practices

### DO:
✅ Keep issue descriptions concise (1-2 paragraphs max)
✅ Update Status as you progress
✅ Add actual effort when closing
✅ Reference related issues (Blocks, Blocked by)
✅ Regenerate index after changes

### DON'T:
❌ Edit README.md manually (it's auto-generated)
❌ Create issues without using TEMPLATE.md
❌ Leave issues in IN PROGRESS for >2 sessions (update or close)
❌ Mark as CLOSED without meeting success criteria

---

## Integration with Workflow

### `/end-session` Command

Step 5 automatically runs `python3 scripts/update_issue_index.py` to keep the index current.

### `/commit-review` Command

If code review finds issues, create an ISSUE to track fixes:

```bash
cp docs/issues/TEMPLATE.md docs/issues/ISSUE-XXX-code-review-findings.md
# Fill in findings, then:
python3 scripts/update_issue_index.py
```

### `/retro` Command

Step 7a reads `docs/issues/README.md` for analytics (issues closed this week).

---

## Examples

### Example 1: Create Feature Request

```bash
cp docs/issues/TEMPLATE.md docs/issues/ISSUE-060-add-dark-mode.md

# Edit file:
**Status**: OPEN
**Priority**: MEDIUM
**Category**: Feature

## Problem
Users want dark mode for better readability at night.

## Approach
1. Add theme toggle to settings
2. Create dark color palette
3. Apply theme throughout UI

## Success Criteria
- [ ] Theme toggle works
- [ ] All pages support dark mode
- [ ] User preference persists

# Regenerate index
python3 scripts/update_issue_index.py
```

### Example 2: Close Bug

```bash
# Edit ISSUE-XXX.md:
**Status**: CLOSED
**Completed**: 2025-10-22 (Session 198)
**Actual Effort**: 2 hours

# Regenerate index
python3 scripts/update_issue_index.py
```

---

## FAQ

**Q: Can I rename an issue?**
A: Yes, but use `git mv` to preserve history:
```bash
git mv docs/issues/ISSUE-001-old-name.md docs/issues/ISSUE-001-new-name.md
python3 scripts/update_issue_index.py
```

**Q: Can I delete an issue?**
A: Prefer closing over deleting (preserves history). If you must delete:
```bash
git rm docs/issues/ISSUE-XXX.md
python3 scripts/update_issue_index.py
```

**Q: What if README.md is out of sync?**
A: Just regenerate it:
```bash
python3 scripts/update_issue_index.py
```

**Q: Can I have sub-issues?**
A: Use "Related: ISSUE-XXX" in the issue file to link them.

---

## Related Documentation

- **TEMPLATE.md** - Issue template (copy this to create new issues)
- **README.md** - Auto-generated index (don't edit manually!)
- **update_issue_index.py** - Script that generates README.md

---

**Last Updated**: 2025-10-22 (Session 198)
**Auto-generated by**: `python3 scripts/update_issue_index.py`
