# ISSUE-034: Replace Generic Sidebar Icons with Context-Appropriate Icons

**Created**: 2025-11-22 (Session 2)
**Status**: IN PROGRESS
**Priority**: LOW
**Category**: Developer Experience
**Blocking**: None

---

## Problem

The `index_jte.jte` sidebar navigation uses `bi-card-list` (a generic list icon) for 6 active menu items, making the UI visually monotonous and harder to navigate. Users can't quickly identify sections by icon alone.

---

## Root Cause

Template was created with placeholder icons that were never updated to be contextually meaningful.

---

## Approach

Replace each generic `bi-card-list` icon with a Bootstrap Icons icon that semantically matches the menu item's function:

| Menu Item | Current Icon | New Icon | Rationale |
|-----------|--------------|----------|-----------|
| Trade Log | `bi-card-list` | `bi-journal-text` | Journal/ledger for trades |
| Position Calculator | `bi-card-list` | `bi-calculator` | Calculator function |
| Instruments | `bi-card-list` | `bi-collection` | Collection of items |
| Net Asset Value | `bi-card-list` | `bi-pie-chart` | Portfolio value chart |
| Earnings | `bi-card-list` | `bi-graph-up-arrow` | Profit/growth indicator |

Note: Trade History already uses `bi-archive` (appropriate) and Import History uses `bi-clock-history` (appropriate).

---

## Success Criteria

- [x] All 5 sidebar menu items with `bi-card-list` have unique icons
- [x] Icons are from Bootstrap Icons library (already loaded)
- [x] Visual hierarchy improved for navigation
- [x] Build compiles successfully

---

## Acceptance Tests

```bash
# Verify build compiles
./mvnw clean compile

# Visual verification: each menu item has distinct icon
```

---

## Related Issues

- None

---

## Notes

- Bootstrap Icons library is already loaded in the template
- Commented-out admin sections also have generic icons but are not in scope
- Unused icon libraries (Boxicons, Remix Icon) could be removed in future cleanup
