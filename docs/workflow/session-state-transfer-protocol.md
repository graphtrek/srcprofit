# Session State Transfer Protocol

**Purpose**: Zero-degradation handoff standards for session continuity across 100+ sessions

**Last Updated**: 2025-10-18 (Session 161)

---

## 🎯 Protocol Objective

**Enable next session to start working immediately with zero ambiguity**

**Success Metrics**:
- ✅ Next session resumes in <5 minutes (no confusion)
- ✅ No duplicate research or rediscovery
- ✅ Completion percentages match reality
- ✅ Blockers are actionable with workarounds
- ✅ Unfinished tasks carry over with ALL context

---

## 📋 Definition of DONE Criteria

Before claiming work is "done", verify ALL criteria are met:

### Code Implementation

- [ ] **Implementation complete** - All functions/classes written
- [ ] **Tests written** - Unit + integration tests for new code
- [ ] **Tests passing** - All existing + new tests green
- [ ] **Code committed** - Changes in git with meaningful message
- [ ] **Code pushed** - Remote has latest code

### Quality Standards

- [ ] **Lint passing** - No flake8/black/isort errors
- [ ] **Type hints added** - mypy-compliant annotations
- [ ] **Documentation updated** - README, code comments, docstrings
- [ ] **No regressions** - Existing functionality unaffected

### User Validation

- [ ] **User testing complete** - Real data tested (not just unit tests)
- [ ] **Edge cases handled** - Boundary conditions tested
- [ ] **Error handling** - Graceful failures with helpful messages

### Definition Compliance

- [ ] **All above criteria met** - Not just some
- [ ] **No "almost done"** - Either DONE or WIP (be honest)
- [ ] **No "90% complete"** - If user testing pending, it's 70% max

---

## 🚨 Honest Completion Percentages

### Percentage Guidelines

| % Complete | Criteria Met | Status |
|------------|--------------|--------|
| **100%** | ALL Definition of Done criteria | ✅ DONE |
| **90%** | All code done, tested, committed, user testing complete | ⚠️ Minor polish only |
| **80%** | Code done, tests passing, committed, docs updated | ⚠️ User testing pending |
| **70%** | Code done, tests passing, committed | ⚠️ Docs + user testing pending |
| **60%** | Code done, some tests passing | ⚠️ Test fixes + docs pending |
| **50%** | Code done, tests failing or not written | ⚠️ Testing phase |
| **40%** | Most code done, uncommitted | ⚠️ Debugging phase |
| **30%** | Some code done, blockers present | ⚠️ Active development |
| **20%** | Implementation started, incomplete | ⚠️ Early development |
| **10%** | Research done, design complete | ⚠️ Planning phase |

### Common Mistakes

❌ **"Implementation complete" when tests fail** → Actually 50-60%
❌ **"90% done" when user testing not started** → Actually 70% max
❌ **"Almost done" when uncommitted** → Actually 40-50%
❌ **"Done" when docs not updated** → Actually 80-90%

---

## 📝 Unfinished Task Carryover Standard

**Problem**: Vague "next steps" waste time in next session

**Solution**: Detailed task carryover with ALL context

### Carryover Template (Mandatory for WIP Sessions)

```markdown
**Unfinished Tasks** (carry over to Session [N+1]):

**ISSUE-XXX: [Issue Title]** ([XX%] → [YY%]):

**Task 1: [Specific implementation needed]**
- **File**: [exact file path, e.g., src/brokers/adapter.py]
- **Add/Modify**: [specific function/class, e.g., "Add fetch_quotes() method"]
- **Pattern**: [reference to similar code if applicable, e.g., "Similar to fetch_positions() in same file:142"]
- **Blocker**: [what's blocking this, if any, e.g., "API returns empty for futures symbols"]
- **Workarounds Attempted**:
  1. [What was tried, e.g., "Tried REST API /quotes endpoint"]
  2. [Result, e.g., "Returns 200 but empty array"]
  3. [Why it didn't work, e.g., "Futures not supported by REST /quotes"]
- **Next Approach**:
  1. [IMMEDIATE: First thing to try, e.g., "Try DXLink streaming API (docs/api/dxlink.md)"]
  2. [BACKUP: If that fails, e.g., "Check Position model for cached last_price field"]
  3. [FALLBACK: Last resort, e.g., "Add --price flag for manual entry"]
- **Dependencies**: [what must exist first, e.g., "DXLink streamer session (see Session 145)"]
- **Test Command**: [exact command to test when done, e.g., "contrarian position-risk /GCZ5"]
- **Estimate**: [X hours, e.g., "2-3 hours (DXLink integration)"]

**Task 2: [Another specific task]**
[Same structure...]
```

### Why This Level of Detail?

| Question | Vague Handoff | Detailed Carryover |
|----------|--------------|-------------------|
| **What file?** | "Fix quote fetching" | src/brokers/adapter.py:fetch_quotes() |
| **What's broken?** | "API not working" | API returns 200 but empty for futures |
| **What was tried?** | Unknown (redo same failures) | REST /quotes failed, DXLink next |
| **What to try next?** | Guess | Try DXLink (docs/api/dxlink.md) |
| **How to test?** | Figure it out | contrarian position-risk /GCZ5 |
| **How long?** | Unknown | 2-3 hours (DXLink integration) |

**Time saved**: 30-60 minutes of rediscovery per task

---

## 🔍 Test Execution Decision Matrix

**When to run tests in /end-session?**

| Scenario | Run Tests? | Reason |
|----------|------------|--------|
| **All code committed** | ❌ NO | Tests already passed in /commit |
| **Uncommitted code** | ✅ YES | Verify current state |
| **Emergency exit** | ❌ NO | Low context, infer state |
| **WIP session** | ✅ YES | Document what's passing/failing |
| **Docs-only changes** | ❌ NO | No code to test |

### Test Status Reporting

**If tests run, report EXACT status**:

```markdown
**Test Status**:
- ✅ Passing: 615/623 tests (8 failures)
- ❌ Failing:
  - test_fetch_quotes_futures (API empty response)
  - test_position_risk_futures (depends on fetch_quotes)
  - [list all 8 failures with brief reason]
- ⚠️ Skipped: 15 integration tests (require credentials)
```

**Don't say**: "Most tests passing" (too vague)
**Do say**: "615/623 passing (8 failures listed below)"

---

## 🎯 Session Handoff Checklist

### Before Creating SESSION_XX_COMPLETE.md

1. **Verify Definition of Done** for all claimed "complete" work
2. **Calculate honest completion %** (see percentage guidelines)
3. **Document ALL blockers** (with error messages)
4. **List workarounds attempted** (prevent duplicate failures)
5. **Provide actionable next steps** (not vague "continue work")
6. **Specify exact files/functions** for unfinished tasks
7. **Include test commands** (copy-paste ready)
8. **Estimate remaining time** (helps planning)

### Session File Required Sections

**Mandatory** (all sessions):
- 🎯 Mission Accomplished (honest summary)
- ✅ What Was Completed (with completion %)
- 📊 Impact (metrics, files changed)
- 🔮 Next Session (immediate tasks)

**Required for WIP**:
- 🚨 Unfinished Tasks (detailed carryover format)
- 🐛 Blockers (with error messages)
- 💡 Workarounds Attempted (prevent repeats)

**Optional** (if applicable):
- 🐛 Bugs Fixed
- 📚 Key Lessons
- 💡 Architecture Insights

---

## 🔄 Session Types and Handoff Requirements

### NORMAL_COMPLETE

**Exit Criteria**:
- All session goals met (100% Definition of Done)
- All code committed and pushed
- All tests passing
- Documentation updated

**Handoff Requirements**:
- Standard SESSION_XX_COMPLETE.md
- "Next Session" section with NEW goals (not carryover)
- Metrics (tests, coverage, performance)

### NORMAL_WIP

**Exit Criteria**:
- Partial progress made (50-90% complete)
- Code may be committed or uncommitted
- Tests may be passing or failing
- Blockers identified

**Handoff Requirements**:
- SESSION_XX_COMPLETE.md with WIP sections
- "Unfinished Tasks" with detailed carryover
- "Blockers" section with error messages
- "Workarounds Attempted" section
- "Next Steps" prioritized and actionable

### EMERGENCY (Low Context)

**Exit Criteria**:
- Context usage > 90% (180k/200k tokens)
- Cannot continue safely
- May have uncommitted code

**Handoff Requirements**:
- SESSION_XX_COMPLETE.md (NOT separate EMERGENCY.md file)
- Exit Type: "EMERGENCY (low context)"
- Git state captured (uncommitted files listed)
- Last error message or blocker
- Simplified handoff (infer what possible)
- Resume checklist (exact steps to continue)

**Template**: See `docs/workflow/emergency-session-template.md`

---

## 📊 Context Efficiency Strategies

### Minimize Token Bloat in Handoffs

**DO**:
- ✅ Reference existing docs (don't repeat)
- ✅ Use tables for structured data
- ✅ Be concise but complete
- ✅ Link to code with file:line format

**DON'T**:
- ❌ Repeat information from other docs
- ❌ Include full code listings (use file paths)
- ❌ Write essays (bullets are better)
- ❌ Duplicate git log output

### Session File Size Guidelines

| File Type | Target Size | Max Size | Reason |
|-----------|-------------|----------|--------|
| SESSION_XX_COMPLETE.md | 200-300 lines | 500 lines | Digestible in one read |
| SESSION_XX_HANDOFF.md | DEPRECATED | N/A | Use COMPLETE.md only |
| Emergency session | 150-250 lines | 350 lines | Fast resume critical |

**If over max size**: Extract details to separate research docs, reference them

---

## 🎓 Examples of Good vs Bad Handoffs

### BAD: Vague Next Steps

```markdown
**Next Session**:
- Fix the quote fetching issue
- Continue working on position risk
- Update tests
```

**Problems**:
- No file paths (where is quote fetching code?)
- No blocker details (what's the issue?)
- No test specifics (which tests?)

### GOOD: Actionable Next Steps

```markdown
**Unfinished Tasks**:

**ISSUE-006: Position Risk Calculator** (60% → 100%):

**Task 1: Fix futures quote fetching**
- File: src/brokers/adapter.py:142 (fetch_quotes method)
- Blocker: REST /quotes endpoint returns empty for futures symbols
- Workarounds Attempted:
  1. Tried /quotes?symbols=/GCZ5 → 200 OK but empty array
  2. Tried /quotes?symbols=GC → same result
  3. API limitation: futures not in REST /quotes
- Next Approach:
  1. IMMEDIATE: Try DXLink streaming (see docs/api/dxlink.md)
  2. BACKUP: Check Position.last_price field (may be cached)
  3. FALLBACK: Add --price CLI flag for manual entry
- Test Command: contrarian position-risk /GCZ5 --account=XXX
- Estimate: 2 hours (DXLink integration) or 0.5 hours (fallback)

**Task 2: Update integration tests**
- File: tests/integration/test_position_risk.py
- Add: test_futures_quote_fetching() (currently missing)
- Pattern: Similar to test_equity_quote_fetching():45
- Depends on: Task 1 completion
- Estimate: 0.5 hours
```

**Benefits**:
- Next session knows EXACTLY what to do
- Blockers clear with evidence
- Multiple approaches prevent getting stuck
- Time estimates help planning

---

## 🔒 Quality Assurance

### Before Ending Session

**Run this mental checklist**:

1. ✅ Would I be able to resume from this handoff in 1 week?
2. ✅ Are completion percentages honest (per guidelines)?
3. ✅ Are blockers specific (with error messages)?
4. ✅ Are file paths exact (not "the adapter file")?
5. ✅ Can next steps be executed without research?
6. ✅ Are test commands copy-paste ready?
7. ✅ Did I document what I tried (prevent reruns)?

**If any "no"**: Improve handoff before saving

---

## 📈 Success Metrics

### Good Handoff Indicators

- ✅ Next session starts coding in <5 minutes
- ✅ No "wait, what was I doing?" moments
- ✅ No repeating failed approaches
- ✅ Completion claims match reality
- ✅ Blockers are unblocked in next session

### Poor Handoff Indicators

- ❌ Next session spends 30+ min understanding state
- ❌ Re-runs same failures (workarounds not documented)
- ❌ Claims "90% done" but takes 3 more sessions
- ❌ Vague blockers ("API not working")
- ❌ Missing file paths, test commands, estimates

---

## 🔗 Related Documentation

- **Emergency Session Template**: `docs/workflow/emergency-session-template.md`
- **Archive Rotation Policy**: `docs/workflow/archive-rotation-policy.md`
- **Definition of Done**: This file (see "Definition of DONE Criteria" section)
- **Session Templates**: `docs/slash-commands-template/end-session.template.md`

---

**Reference**: Used by `/end-session` Step 2 (session summary creation)
**Enforced by**: Code review, session quality checks
**Next review**: 2026-10-18 (or after 50 sessions, whichever first)
