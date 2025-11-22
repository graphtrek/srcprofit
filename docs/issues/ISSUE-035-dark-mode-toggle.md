# ISSUE-035: Add Dark Mode Toggle Button

**Created**: 2025-11-22 (Session 2)
**Status**: IN PROGRESS
**Priority**: LOW
**Category**: Feature
**Blocking**: None

---

## Problem

The application only supports a light theme. Users have no way to switch to a dark mode for reduced eye strain, especially during evening/night trading sessions.

---

## Root Cause

No dark mode implementation exists - the CSS uses hardcoded light colors without CSS variables or theme switching capability.

---

## Approach

1. Add CSS variables for light and dark themes in `:root` and `[data-theme="dark"]`
2. Add dark mode toggle button in the header navigation
3. Use JavaScript to toggle `data-theme` attribute on `<html>` element
4. Persist user preference in localStorage
5. Respect system preference with `prefers-color-scheme` media query

---

## Success Criteria

- [x] Dark mode toggle button visible in header
- [x] Clicking toggles between light/dark themes
- [x] User preference persists across browser sessions (localStorage)
- [x] All pages render correctly in dark mode
- [x] Icon changes based on current theme (sun/moon)

---

## Acceptance Tests

```javascript
// Manual test: Click dark mode button
// - Page background should change to dark colors
// - Text should become light colored
// - Icon should change from moon to sun
// - Refresh page - theme should persist
```

---

## Related Issues

- None

---

## Notes

- Uses Bootstrap 5 compatible approach
- CSS variables enable easy color customization
- localStorage provides cross-session persistence
