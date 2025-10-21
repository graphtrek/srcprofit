# Full Context - Load Complete Project Knowledge

You are loading the **complete context** for the **{{PROJECT_NAME}}** project.

**Purpose**: Load everything for major decisions, new phases, or architectural work (~35k tokens)

---

## 📚 Complete Context Load

Read these files in order:

### 1. Entry Point
- `CLAUDE.md` - Project pointer, workflow, critical lessons

### 2. Active Context
- `{{ACTIVE_CONTEXT_FILE}}` - Last 3 sessions, active tasks, current focus

### 3. Reference Documentation
- `{{FULL_CONTEXT_FILE}}` - Architecture, phases, permanent lessons, knowledge base

### 4. Recent Session History
- `{{SESSION_INDEX_FILE}}` - Check table for last 3 session files
- Read last 3 `docs/sessions/{{SESSION_PREFIX}}_XX_COMPLETE.md` files for detailed history

### 5. Project Status
- Run: `git status` for current state
- Run: `git log --oneline -10` for recent work

---

## 🎯 After Loading Context

You now have complete understanding of:

- ✅ Project architecture and design decisions
- ✅ All completed phases and sessions
- ✅ Current work in progress
- ✅ Knowledge base and resources
- ✅ Team workflow and conventions
- ✅ Critical lessons learned

---

## 💬 User Prompt

After loading all context above, inform user:

```
Full context loaded successfully!

**Project**: {{PROJECT_NAME}}
**Branch**: [CURRENT_BRANCH]
**Session**: [CURRENT_SESSION_NUMBER]
**Phase**: [CURRENT_PHASE from context]

**Last 3 Sessions**:
1. Session XX: [SUMMARY]
2. Session YY: [SUMMARY]
3. Session ZZ: [SUMMARY]

**Current Focus**: [from active context]

**Architecture**: [Brief summary from CLAUDE_CONTEXT.md]

I now have complete project knowledge. What major decision or task are you working on?
```

---

## 🔍 When to Use Full Context

**Use this command when:**
- ✅ Starting a new phase
- ✅ Making architectural decisions
- ✅ Planning major refactoring
- ✅ First time working on project
- ✅ Need to understand "big picture"
- ✅ Researching how systems interact
- ✅ After long break from project

**Don't use for:**
- ❌ Daily bug fixes (use `/start-session`)
- ❌ Continuing yesterday's work (use `/start-session`)
- ❌ Small feature additions (use `/start-session`)

**Why**: Save tokens for when you really need complete context

---

## 📊 Token Usage

| Context Level | Tokens | Use Case |
|--------------|--------|----------|
| Start Session | ~7k | Daily work (96% of sessions) |
| Full Context | ~35k | Major decisions (4% of sessions) |

**Strategy**: Use start-session by default, full-context when necessary

---

## 🎓 Best Practices

After loading full context:

1. **Summarize understanding** - Show user you get the big picture
2. **Reference specific files** - Quote relevant sections
3. **Ask clarifying questions** - Ensure alignment on goals
4. **Use TodoWrite** - Break down complex tasks
5. **Leverage subagents** - Use specialized agents for deep work

---

## 🔄 When to Refresh Full Context

**Refresh every**:
- New phase start
- After 10+ sessions with only start-session
- Major architectural changes
- When feeling uncertain about project state

**Don't refresh**:
- Every session (wasteful)
- For minor questions (spot-check specific files instead)

---

## 📋 Optional: Load Additional Resources

If needed, also read:

- `docs/knowledge-base-index.md` - Complete resource catalog
- `docs/architecture/` - Architecture documentation
- Project-specific documentation (ask user for paths)

**Only load if:**
- User specifically requests
- Making decisions that need this depth
- Research phase of work

---

**Command**: `/full-context`
**Tokens**: ~35k (vs 7k start-session)
**Use Frequency**: ~4% of sessions (major decisions only)
