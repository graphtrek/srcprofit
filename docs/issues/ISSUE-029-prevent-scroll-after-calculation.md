# ISSUE-029: Prevent Page Scroll After Calculation

**Created**: 2025-11-15 (Session Current)
**Status**: CLOSED
**Completed**: 2025-11-15
**Priority**: MEDIUM
**Category**: Bug Fix / UX Enhancement
**Blocking**: None

---

## Problem

When users click the "Calculate" button in the Position Calculator form, the page scrolls down after the calculation completes. This creates an undesirable user experience as the form inputs are no longer visible after submission.

---

## Root Cause

HTMX's default behavior when swapping content (`hx-swap="innerHTML"`) includes auto-scrolling to the swapped element. Without explicit scroll prevention, the browser naturally scrolls to show the updated content in the `#main` container.

---

## Solution

Added `hx-scroll="false"` attribute to the calculation form in the position form template. This tells HTMX to preserve the current scroll position after the content swap completes.

---

## Changes

### File: `src/main/jte/position-form_jte.jte`

**Line 54**: Added `hx-scroll="false"` to the form element:
```html
<form class="row g-3" hx-validate="true"
      hx-post="/calculatePosition"
      hx-trigger="submit"
      hx-target="#main"
      hx-swap="innerHTML"
      hx-scroll="false"
      hx-on::afterRequest="console.log('calculatePosition');">
```

---

## Result

- Form submission now maintains scroll position
- Calculation results update in place without page jump
- User can immediately see the updated calculation results without scrolling
- Enhanced UX by keeping form inputs visible

---

## Testing

Manual testing confirms:
- Calculate button submits successfully
- Page maintains scroll position after calculation completes
- Results display correctly in the calculation form
- No regression in existing functionality

---

## Related Issues

- **Related**: ISSUE-027 (Position Calculator Recalculate Button) - Related UI improvements
- **Related**: ISSUE-026 (Position Calculator Manual Recalculation) - Related calculation logic

