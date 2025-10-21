# Definition of Done

**Purpose**: Clear, enforceable completion criteria for all work

**Version**: 1.0 (Session 122)

**Applies To**: All agents, all tasks, all sessions

---

## 🎯 The Core Formula

```
DONE = Code Written
     + Tests Pass
     + User Tested (if user-facing)
     + UX Validated (if UI/output)
     + Bugs Fixed
     + Code Committed
```

**ALL criteria must be met** to claim "done"

---

## ✅ Completion Checklist

### For Feature Implementation

- [ ] Code written and follows project standards
- [ ] Unit tests written and passing
- [ ] Integration tests passing (if applicable)
- [ ] No regression (existing tests still pass)
- [ ] User tested (if user-facing feature)
- [ ] UX validated (if output/UI changes)
- [ ] Documentation updated (if needed)
- [ ] Code committed and pushed
- [ ] No known bugs

### For Bug Fixes

- [ ] Root cause identified and documented
- [ ] Fix implemented
- [ ] Test reproduces bug (before fix)
- [ ] Test passes (after fix)
- [ ] No regression (existing tests still pass)
- [ ] User confirmed fix works (if user-reported)
- [ ] Code committed and pushed

### For Refactoring

- [ ] Code restructured/simplified
- [ ] All existing tests still pass
- [ ] No behavior changes (unless intentional)
- [ ] Documentation updated (if API changed)
- [ ] Code committed and pushed
- [ ] Performance verified (if performance refactor)

### For Documentation

- [ ] Content written
- [ ] Technical accuracy verified
- [ ] Examples tested (if code examples)
- [ ] Links validated (if external links)
- [ ] User can understand (clarity check)
- [ ] Committed and pushed

### For Agent Updates

- [ ] Agent file updated
- [ ] Changes follow agent template structure
- [ ] Examples provided (if new capability)
- [ ] Tested with representative task (if possible)
- [ ] Committed and pushed

---

## ❌ What is NOT Done

### False Completion Claims

❌ "Implementation complete" → Tests failing
❌ "Feature ready" → Not shown to user
❌ "Bug fixed" → User reports still broken
❌ "Done" → Code uncommitted
❌ "95% complete" → 5 critical bugs remain
❌ "Ready for production" → No user testing
❌ "Works on my machine" → Not tested in actual environment

### Partial Work States

Use these honest descriptions instead:

✅ "Implementation complete, ready for testing"
✅ "Code written, 3/8 tests failing, debugging"
✅ "Feature functional, needs user UX feedback"
✅ "Bug fix deployed, awaiting user confirmation"
✅ "Refactor complete, performance test pending"

---

## 🎯 Percentage Completion Guide

### How to Calculate Honest Percentages

**Example: Feature Implementation**

If Definition of Done has 8 criteria:
1. Code written ✅
2. Unit tests pass ✅
3. Integration tests pass ❌ (not run)
4. No regression ✅
5. User tested ❌ (not done)
6. UX validated ❌ (not done)
7. Documentation updated ✅
8. Committed ✅

**Calculation**: 5/8 = 62.5% complete (round to 60%)

**Wrong**: "Implementation complete, just needs testing" (implies 90%+)

**Right**: "60% complete - code working, tests passing, needs user validation"

### Common Completion Ranges

- **0-25%**: Started, significant work remains
- **25-50%**: Substantial progress, major components incomplete
- **50-75%**: Core work done, testing/validation incomplete
- **75-90%**: Nearly done, minor issues or final validation pending
- **90-99%**: All criteria met except one or two final items
- **100%**: ALL Definition of Done criteria met

**Rule**: Never claim >90% if user testing not done

---

## 🧪 Testing Requirements

### When User Must Test

User testing REQUIRED for:
- CLI command output (formatting, readability)
- Interactive workflows (multi-step processes)
- Real portfolio data validation
- Edge cases requiring domain knowledge
- UX/usability evaluation
- Ground truth comparison (TastyTrade CSV)

### When Automated Tests Sufficient

Automated tests SUFFICIENT for:
- Internal functions (no user visibility)
- Data transformations (with known inputs/outputs)
- Algorithm correctness (unit testable)
- Regression prevention (existing tests)
- Code quality (lint, format, type checks)

### Test Execution Rules

**Agent states rule, user can override:**

Agent: *"According to test execution rules, I will run tests now."*
- Code changes that could break existing functionality
- New code with automated test coverage
- Pre-commit checks

Agent: *"According to test execution rules, this requires user testing."*
- CLI output formatting
- Real data validation
- UX feedback

User: *"Skip tests"* or *"Run tests anyway"* (agent complies)

---

## 📊 Quality Gates

### Pre-Commit Gate (Automatic)

Blocked if:
- ❌ Tests failing
- ❌ Lint errors
- ❌ Format issues
- ❌ Committing to main (should use claude-coder-work)

Work is NOT done if pre-commit fails.

### User Validation Gate (Manual)

Required for:
- CLI commands (run and verify output)
- Data processing (compare to ground truth)
- Workflows (test full end-to-end)
- UI/UX changes (user evaluates)

Work is NOT done if user hasn't validated.

### Production Gate (CI/CD)

Blocked if:
- ❌ Full test suite failing
- ❌ Coverage below threshold
- ❌ Security vulnerabilities
- ❌ Complexity too high
- ❌ Type errors

Work is NOT done if CI/CD fails.

---

## 🚨 Common Pitfalls

### Pitfall 1: "Just needs testing"

**Problem**: Implies work is done except testing

**Reality**: Testing IS part of done

**Fix**: "Implementation complete, testing in progress (60% done)"

### Pitfall 2: "Works for me"

**Problem**: Agent validates, user hasn't tested

**Reality**: User testing required for user-facing work

**Fix**: "Ready for user testing - command: `./contrarian-ctl test`"

### Pitfall 3: "Minor bugs remaining"

**Problem**: Claims "done" with known bugs

**Reality**: Bugs block completion

**Fix**: "90% complete - 2 bugs remaining before done"

### Pitfall 4: "Code complete"

**Problem**: Only considers code written

**Reality**: Tests, validation, commit all required

**Fix**: "Code written (25%), tests pending (25%), user testing pending (25%), commit pending (25%)"

### Pitfall 5: "Basically done"

**Problem**: Vague completion claim

**Reality**: "Basically" = not actually done

**Fix**: Use exact percentage or clear status: "75% complete - awaiting user UX feedback"

---

## 🎯 Enforcement

### Agent Responsibility

Every agent MUST:
1. Check Definition of Done before claiming completion
2. Calculate honest completion percentages
3. Explicitly list incomplete criteria
4. Never claim "done" with failing tests
5. Never skip user testing for user-facing work

### User Responsibility

User SHOULD:
1. Report false "done" claims
2. Validate completion before accepting
3. Test user-facing features before accepting "done"
4. Provide feedback on completion accuracy

### Session Handoff

Every session handoff MUST:
1. Verify each task against Definition of Done
2. Report honest completion percentages
3. List incomplete criteria explicitly
4. Provide actionable next steps for incomplete work

---

## 📚 Examples

### Example 1: CLI Command Implementation

**Status**: "Implementation complete, ready for testing"

**Checklist**:
- [x] Code written (command logic implemented)
- [x] Unit tests written and passing (8/8)
- [x] No regression (existing tests pass)
- [ ] User tested (not run yet)
- [ ] UX validated (not shown to user)
- [ ] Code committed (not yet)

**Completion**: 50% (3/6 criteria)

**Next Steps**:
1. Run command: `./contrarian-ctl new-command --test`
2. User validates output format
3. User confirms UX acceptable
4. Commit code

### Example 2: Bug Fix

**Status**: "Bug fixed, awaiting user confirmation"

**Checklist**:
- [x] Root cause identified (off-by-one error in loop)
- [x] Fix implemented
- [x] Test reproduces bug (before fix)
- [x] Test passes (after fix)
- [x] No regression (all tests pass)
- [ ] User confirmed fix works (not yet)
- [x] Code committed

**Completion**: 85% (6/7 criteria)

**Next Steps**:
1. User runs: `./contrarian-ctl test-bug-fix`
2. User confirms bug no longer occurs
3. Mark as 100% complete

### Example 3: Refactoring

**Status**: "Refactoring complete, all tests passing"

**Checklist**:
- [x] Code restructured (moved to new module)
- [x] All existing tests still pass (615/615)
- [x] No behavior changes
- [x] Documentation updated (imports, README)
- [x] Code committed and pushed

**Completion**: 100% (5/5 criteria)

**Status**: DONE ✅

---

## 🔄 Continuous Improvement

### Learning from Failures

When false "done" claims occur:
1. Document the case (what was claimed vs reality)
2. Update Definition of Done if needed
3. Add to Common Pitfalls section
4. Share learning with user

### Metrics to Track

- False "done" claims per session
- Time wasted re-doing "completed" work
- User testing catch rate (bugs found)
- Completion accuracy (claimed vs actual)

### Success Indicators

✅ Work claimed "done" requires no rework
✅ User testing finds zero issues
✅ Completion percentages match reality
✅ No surprises when resuming work

---

## 📚 Related Documents

- `docs/SESSION_STATE_TRANSFER_PROTOCOL.md` - Session handoff quality
- `docs/CLAUDE_ACTIVE_CONTEXT.md` - Current work tracking
- `docs/workflow/quality-gates.md` - Automated quality checks

---

**Remember**: Done is not done until ALL criteria are met.

**Version History**:
- v1.0 (2025-01-16, Session 122): Initial definition

**Last Updated**: 2025-01-16
