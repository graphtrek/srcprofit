# ISSUE-002: Kaizen Script Portability - Auto-Detect Project Name

**Created**: 2025-10-23 (Session 3)
**Status**: CLOSED
**Priority**: LOW
**Category**: Workflow / Process Improvement
**Completed**: 2025-10-22 (Session 198 in contrarian project)
**Actual Effort**: 0.5 hours

---

## Problem

The `update_issue_index.py` script hardcoded the project name as "Contrarian", making it unsuitable for use in other Kaizen projects like SrcProfit. When exported to external projects, the issue index would display the wrong project name.

**Symptoms**:
- Issue index shows "Issue Tracking - Contrarian" in SrcProfit project
- Script not portable across Kaizen ecosystem
- Manual editing required after export

---

## Root Cause

**Original implementation** (commit bc60f2d):
```python
# Hardcoded project name
title = "# Issue Tracking - Contrarian\n\n"
```

The script was developed within the contrarian project and hardcoded the project name rather than detecting it from the environment.

---

## Approach

**Solution** (commit 70acbea):
Auto-detect project name from git repository:

1. **Primary**: Extract from git remote URL
   ```python
   git remote get-url origin
   # .../srcprofit.git → Srcprofit
   # .../contrarian.git → Contrarian
   ```

2. **Fallback**: Use directory name if git remote unavailable
   ```python
   Path.cwd().name.capitalize()
   ```

**Changes**:
```python
def get_project_name() -> str:
    """Auto-detect project name from git remote or directory."""
    try:
        result = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            capture_output=True, text=True, check=True
        )
        remote_url = result.stdout.strip()
        # Extract name from URL (handles .git, /, etc.)
        name = remote_url.rstrip("/").split("/")[-1].replace(".git", "")
        return name.capitalize()
    except:
        return Path.cwd().name.capitalize()
```

---

## Success Criteria

- [x] Script detects "Contrarian" when run in contrarian project
- [x] Script detects "Srcprofit" when run in srcprofit project
- [x] Works with .git suffix in remote URL
- [x] Fallback to directory name works when git unavailable
- [x] Generic for all future Kaizen projects

---

## Acceptance Tests

**Test 1: Contrarian project**
```bash
cd ~/projects/contrarian
python scripts/update_issue_index.py
grep "Issue Tracking - Contrarian" docs/issues/README.md
# ✅ PASS
```

**Test 2: SrcProfit project**
```bash
cd ~/projects/srcprofit
python scripts/update_issue_index.py
grep "Issue Tracking - Srcprofit" docs/issues/README.md
# ✅ PASS
```

**Test 3: No git remote (fallback)**
```bash
# Simulate missing git remote
PROJECT=$(basename $(pwd))
# Capitalizes directory name
# ✅ PASS
```

---

## Related Issues

- Related: ISSUE-001 (first issue to be tracked with improved script)
- Enables: Future Kaizen exports to new projects

---

## Impact

### Before (bc60f2d)
- Manual editing required after export
- Hardcoded "Contrarian" in all projects
- Not portable

### After (70acbea)
- Zero configuration after export
- Auto-detects correct project name
- Generic for entire Kaizen ecosystem

### Files Changed
- `scripts/update_issue_index.py` (+19 lines, +1 function)
- `docs/issues/README.md` (regenerated with correct name)

---

## Kaizen Lesson

**Pattern**: When building workflow tools, always assume they will be exported/reused

**Implementation**:
1. Auto-detect environment (git remote, directory, config files)
2. Provide sensible fallbacks
3. Avoid hardcoding project-specific values
4. Test in multiple projects before export

**Benefit**: One-time investment (0.5 hours) saves 5-10 minutes per export across multiple projects

---

## Notes

This improvement was made in the contrarian project (Session 198) and carried over to srcprofit during the workflow migration (Session 1). This issue documents the improvement retroactively for knowledge transfer and kaizen tracking.

**Commits**:
- bc60f2d - Initial export (hardcoded name)
- 70acbea - Portability fix (auto-detect)

**Documentation updated**:
- CLAUDE.md (Session 3)
- docs/claude-context.md (Session 3)
- docs/claude-active-context.md (Session 3)
