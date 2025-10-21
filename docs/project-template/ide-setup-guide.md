# IDE Setup Guide - Project Template

> **Purpose**: Comprehensive guide for setting up any IDE with Claude Code
> **For**: New projects starting from scratch
> **Template Status**: Reusable across projects

**Last Updated**: 2025-10-09

---

## 📋 Table of Contents

1. [IDE Selection Guide](#ide-selection-guide)
2. [VSCode Setup](#vscode-setup)
3. [JetBrains Setup](#jetbrains-setup)
4. [Terminal-Only Setup](#terminal-only-setup)
5. [Extension Recommendations](#extension-recommendations)
6. [Workspace Configuration](#workspace-configuration)
7. [Testing the Setup](#testing-the-setup)

---

## 🎯 IDE Selection Guide

### Quick Comparison

| IDE | Claude Code Support | Best For | Setup Time |
|-----|---------------------|----------|------------|
| **VSCode** | ✅ Native extension (beta) | Python, Web, Most languages | 5-10 min |
| **JetBrains** | ✅ Plugin available | Java, Kotlin, Professional devs | 10-15 min |
| **Cursor** | ✅ VSCode fork, extension works | VSCode users who want alternatives | 5-10 min |
| **Windsurf** | ✅ VSCode fork, extension works | Similar to Cursor | 5-10 min |
| **Terminal Only** | ✅ CLI works everywhere | Remote work, SSH, minimalists | 0 min |

### Recommendation by Project Type

**Python Projects** (Django, Flask, Data Science):
- 🥇 **VSCode** - Best Python support, native extension
- 🥈 JetBrains PyCharm - Professional features
- 🥉 Terminal Only - Works great for TDD

**Web Projects** (React, Node.js, TypeScript):
- 🥇 **VSCode** - Industry standard
- 🥈 Cursor/Windsurf - VSCode alternatives
- 🥉 JetBrains WebStorm - Full-featured

**Java/Kotlin Projects**:
- 🥇 **JetBrains IntelliJ IDEA** - Best Java IDE
- 🥈 VSCode - Lightweight alternative
- 🥉 Terminal Only - For backend services

**Multi-Language Projects**:
- 🥇 **VSCode** - Universal language support
- 🥈 Terminal Only - Consistent across languages

---

## 📦 VSCode Setup

### Prerequisites

- VSCode 1.98.0 or higher
- Python 3.10+ (or your language runtime)
- Git installed

### Step 1: Install VSCode

Download from: https://code.visualstudio.com/

**Mac**:
```bash
brew install --cask visual-studio-code
```

**Linux**:
```bash
# Snap
sudo snap install code --classic

# Or download .deb/.rpm from website
```

**Windows**:
Download installer from website or use Chocolatey:
```powershell
choco install vscode
```

### Step 2: Install Claude Code Extension

**Method 1: VSCode Marketplace**
1. Open VSCode
2. Click Extensions icon (Cmd+Shift+X)
3. Search: "Claude Code"
4. Click Install on "Claude Code by Anthropic"

**Method 2: Command Line**
```bash
code --install-extension anthropics.claude-code
```

**Method 3: Quick Open**
1. Press Cmd+P (Mac) or Ctrl+P (Windows/Linux)
2. Type: `ext install anthropics.claude-code`
3. Press Enter

### Step 3: Configure .vscode Directory

Create `.vscode/` directory in project root:

```bash
mkdir -p .vscode
```

Copy these templates to your project:

**`.vscode/settings.json`** (see [VSCode Settings Template](#vscode-settings-template))

**`.vscode/extensions.json`** (see [VSCode Extensions Template](#vscode-extensions-template))

**`.vscode/launch.json`** (see [VSCode Launch Template](#vscode-launch-template))

**`.vscode/tasks.json`** (see [VSCode Tasks Template](#vscode-tasks-template))

### Step 4: Install Language Extensions

**Python Projects**:
```bash
code --install-extension ms-python.python
code --install-extension ms-python.vscode-pylance
code --install-extension ms-python.black-formatter
code --install-extension ms-python.isort
code --install-extension ms-python.flake8
```

**JavaScript/TypeScript Projects**:
```bash
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension bradlc.vscode-tailwindcss
```

**Other Useful Extensions**:
```bash
code --install-extension mechatroner.rainbow-csv      # CSV viewer
code --install-extension eamodio.gitlens               # Git visualization
code --install-extension gruntfuggly.todo-tree        # TODO highlighter
```

### Step 5: Remove Conflicting Extensions

**Critical**: Remove these to avoid conflicts with Claude Code:

```bash
code --uninstall-extension github.copilot
code --uninstall-extension github.copilot-chat
code --uninstall-extension tabnine.tabnine-vscode
code --uninstall-extension visualstudioexptteam.vscodeintellicode
```

**Why?** Multiple AI assistants create confusion and performance issues.

### Step 6: Open Claude Code Panel

1. Click Spark icon (⚡) in left sidebar
2. Sign in with your Anthropic account
3. Start a conversation!

---

## 🔧 JetBrains Setup

### Prerequisites

- JetBrains IDE (IntelliJ IDEA, PyCharm, WebStorm, etc.)
- Version 2023.1 or higher recommended
- Git installed

### Step 1: Install Claude Code Plugin

1. Open JetBrains IDE
2. Go to: Settings/Preferences → Plugins
3. Click "Marketplace" tab
4. Search: "Claude Code"
5. Click Install
6. Restart IDE

**Or via command line** (IntelliJ):
```bash
# Download plugin from marketplace first
idea installPlugins /path/to/claude-code-plugin.zip
```

### Step 2: Configure Plugin Settings

1. Go to: Settings → Tools → Claude Code
2. Sign in with Anthropic account
3. Configure preferences:
   - **ESC Key Behavior**: Choose interrupt vs minimize
   - **Auto-accept mode**: Enable/disable
   - **Keyboard shortcuts**: Customize if needed

### Step 3: Configure IDE Settings

**Python (PyCharm)**:
- Interpreter: Project venv
- Formatter: Black
- Linter: Flake8
- Test runner: pytest

**Java (IntelliJ)**:
- JDK: Project JDK version
- Code style: Google Java Style (or your preference)
- Build tool: Maven/Gradle
- Test runner: JUnit 5

**Web (WebStorm)**:
- Node version: Project .nvmrc
- Formatter: Prettier
- Linter: ESLint
- Test runner: Jest/Vitest

### Step 4: Open Claude Code Panel

1. View → Tool Windows → Claude Code
2. Or use keyboard shortcut (default: Cmd+Shift+C)
3. Start conversation!

---

## 💻 Terminal-Only Setup

### Why Terminal Only?

**Advantages**:
- ✅ Works everywhere (SSH, remote servers, containers)
- ✅ Zero IDE setup time
- ✅ Consistent experience across machines
- ✅ Lightweight and fast
- ✅ Great for TDD workflow

**Disadvantages**:
- ❌ No visual diff preview
- ❌ No GUI for file selection
- ❌ No auto-accept mode
- ❌ Manual file editing

### Setup

**1. Install Claude Code CLI**:
```bash
# Mac
brew install anthropic/claude-code/claude-code

# Linux
curl -fsSL https://anthropic.com/install.sh | sh

# Windows
# Download from releases page
```

**2. Authenticate**:
```bash
claude auth login
```

**3. Start Using**:
```bash
# Start conversation
claude

# One-off command
claude -p "Explain this error"

# With files
claude @src/main.py "Add docstrings"
```

### Recommended Terminal Setup

**Shell**: zsh (Mac default) or bash

**Terminal Emulator**:
- **Mac**: iTerm2 (better than Terminal.app)
- **Linux**: GNOME Terminal, Alacritty, or Kitty
- **Windows**: Windows Terminal (built-in)

**Multiplexer**: tmux or screen (optional but recommended)

---

## 📚 Extension Recommendations

### Universal (All IDEs)

**Always Install**:
- Claude Code (native extension/plugin)
- Your language extension (Python, JavaScript, Java, etc.)
- Git visualization (GitLens for VSCode, built-in for JetBrains)

**Recommended**:
- CSV viewer (Rainbow CSV for VSCode)
- TODO highlighter
- Spell checker (for documentation)

**Never Install (Conflicts)**:
- ❌ GitHub Copilot
- ❌ Tabnine
- ❌ IntelliCode
- ❌ Other AI coding assistants

### Python Projects

**Required**:
- Python language server (Pylance for VSCode)
- Black formatter
- isort (import sorting)
- Flake8 linter

**Recommended**:
- pytest test runner
- MyPy type checker
- Coverage visualizer

### JavaScript/TypeScript Projects

**Required**:
- ESLint
- Prettier
- TypeScript (if using TS)

**Recommended**:
- Tailwind CSS IntelliSense
- Auto Rename Tag
- Path Intellisense

### Data Science Projects

**Required**:
- Jupyter extension
- Python extensions (see above)

**Recommended**:
- Rainbow CSV
- Data Preview
- Markdown Preview Enhanced

---

## ⚙️ Workspace Configuration

### VSCode Settings Template

Save as `.vscode/settings.json`:

```json
{
  "// Language Configuration": "Customize for your language",
  "python.defaultInterpreterPath": "${workspaceFolder}/venv/bin/python",
  "python.terminal.activateEnvironment": true,

  "// Editor Behavior": "",
  "editor.formatOnSave": true,
  "editor.codeActionsOnSave": {
    "source.organizeImports": "explicit"
  },
  "editor.rulers": [80, 100],
  "editor.tabSize": 4,
  "editor.insertSpaces": true,
  "files.trimTrailingWhitespace": true,
  "files.insertFinalNewline": true,

  "// Performance Optimization": "",
  "files.exclude": {
    "**/__pycache__": true,
    "**/*.pyc": true,
    "**/.pytest_cache": true,
    "**/node_modules": true,
    "**/venv": true,
    "**/.venv": true
  },
  "search.exclude": {
    "**/__pycache__": true,
    "**/node_modules": true,
    "**/venv": true,
    "**/.venv": true
  },
  "files.watcherExclude": {
    "**/__pycache__/**": true,
    "**/node_modules/**": true,
    "**/venv/**": true
  },

  "// Claude Code Optimization": "",
  "github.copilot.enable": {
    "*": false
  },

  "// Privacy": "",
  "telemetry.telemetryLevel": "off"
}
```

### VSCode Extensions Template

Save as `.vscode/extensions.json`:

```json
{
  "recommendations": [
    "anthropics.claude-code",
    "// Add your language extensions here",
    "ms-python.python",
    "ms-python.vscode-pylance"
  ],
  "unwantedRecommendations": [
    "github.copilot",
    "github.copilot-chat",
    "tabnine.tabnine-vscode",
    "visualstudioexptteam.vscodeintellicode"
  ]
}
```

### VSCode Launch Template

Save as `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Python: Current File",
      "type": "python",
      "request": "launch",
      "program": "${file}",
      "console": "integratedTerminal",
      "justMyCode": false
    },
    {
      "name": "Python: Pytest Current File",
      "type": "python",
      "request": "launch",
      "module": "pytest",
      "args": ["${file}", "-v"],
      "console": "integratedTerminal",
      "justMyCode": false
    }
  ]
}
```

### VSCode Tasks Template

Save as `.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Run Tests",
      "type": "shell",
      "command": "pytest tests -v",
      "group": {
        "kind": "test",
        "isDefault": true
      },
      "presentation": {
        "reveal": "always",
        "panel": "dedicated"
      }
    },
    {
      "label": "Format Code",
      "type": "shell",
      "command": "black . && isort .",
      "group": "build",
      "presentation": {
        "reveal": "silent"
      }
    }
  ]
}
```

---

## ✅ Testing the Setup

### 1. Verify Extensions Installed

**VSCode**:
```bash
code --list-extensions | grep -E "anthropics|python|eslint"
```

**JetBrains**:
Settings → Plugins → Installed → Look for "Claude Code"

### 2. Test Claude Code Integration

1. Open Claude Code panel
2. Type: "Hello! Can you see my project files?"
3. Try @-mentioning a file
4. Verify Claude can access project context

### 3. Test Language Server

**Python**:
```python
# Type this and verify autocomplete works
import os
os.path.  # Should show autocomplete suggestions
```

**TypeScript**:
```typescript
// Type this and verify autocomplete works
const x: string = "hello";
x.  // Should show string methods
```

### 4. Test Formatter

1. Create messy file:
```python
# test.py
import   os,sys
def hello(  ):
   print("hello")
```

2. Format (Cmd+Shift+P → "Format Document")
3. Verify it's cleaned up

### 5. Test Debugger

1. Set breakpoint in any file
2. Press F5 or Run → Start Debugging
3. Verify debugger stops at breakpoint

### 6. Test Tasks (VSCode)

1. Press Cmd+Shift+P
2. Type: "Tasks: Run Task"
3. Select "Run Tests"
4. Verify tests execute

---

## 🚨 Troubleshooting

### Claude Code Extension Not Working

**Symptoms**: Extension installed but not showing up

**Solutions**:
1. Check VSCode version: Must be 1.98.0+
2. Restart VSCode: Cmd+Q and reopen
3. Check extension logs: Output panel → "Claude Code"
4. Reinstall extension: Uninstall → Reinstall → Restart
5. Try terminal Claude CLI: Verify account is authenticated

### Python Interpreter Not Found

**Symptoms**: "Python interpreter not found"

**Solutions**:
1. Create venv: `python3 -m venv venv`
2. Activate: `source venv/bin/activate` (Mac/Linux) or `venv\Scripts\activate` (Windows)
3. Select interpreter: Cmd+Shift+P → "Python: Select Interpreter" → Choose venv
4. Verify in status bar: Should show `venv` at bottom right

### Formatter Not Working

**Symptoms**: Format on save not working

**Solutions**:
1. Install formatter extension (black, prettier, etc.)
2. Set default formatter in settings:
   ```json
   "[python]": {
     "editor.defaultFormatter": "ms-python.black-formatter"
   }
   ```
3. Enable format on save:
   ```json
   "editor.formatOnSave": true
   ```
4. Reload window: Cmd+Shift+P → "Reload Window"

### Tests Not Discovered

**Symptoms**: Test explorer shows no tests

**Solutions**:
1. Configure test framework: Cmd+Shift+P → "Python: Configure Tests"
2. Select pytest/unittest
3. Select tests directory
4. Check test file names: Must match `test_*.py` or `*_test.py`
5. Verify PYTHONPATH: Should include project root

### Performance Issues

**Symptoms**: IDE slow with many files

**Solutions**:
1. Exclude directories in settings:
   ```json
   "files.exclude": {
     "**/node_modules": true,
     "**/venv": true,
     "**/.git": true
   }
   ```
2. Disable unused extensions
3. Clear caches: Cmd+Shift+P → "Clear Editor History"
4. Close unused editors
5. Consider using terminal CLI for large projects

---

## 📖 Additional Resources

### Official Documentation

- **Claude Code**: https://docs.claude.com/en/docs/claude-code
- **VSCode Extension**: https://docs.claude.com/en/docs/claude-code/vs-code
- **JetBrains Plugin**: https://docs.claude.com/en/docs/claude-code/jetbrains
- **Terminal CLI**: https://docs.claude.com/en/docs/claude-code/quickstart

### Community Resources

- **GitHub Issues**: Report bugs and feature requests
- **Discord**: Join Claude developer community
- **Stack Overflow**: Tag questions with `claude-code`

### IDE-Specific Docs

- **VSCode**: https://code.visualstudio.com/docs
- **PyCharm**: https://www.jetbrains.com/pycharm/guide/
- **IntelliJ**: https://www.jetbrains.com/idea/guide/
- **WebStorm**: https://www.jetbrains.com/webstorm/guide/

---

## 🎓 Best Practices

### 1. Commit .vscode to Git

**Do commit**:
- `.vscode/settings.json` (team settings)
- `.vscode/extensions.json` (recommended extensions)
- `.vscode/tasks.json` (build/test tasks)
- `.vscode/launch.json` (debug configurations)

**Don't commit**:
- `.vscode/*.code-workspace` (personal workspace files)
- `.vscode/.history` (local history)

### 2. Keep Extensions Minimal

Only install extensions you actively use. More extensions = slower IDE.

### 3. Use Workspace Settings

Prefer workspace settings over user settings for team consistency:

```json
// .vscode/settings.json (commit this)
{
  "python.formatting.provider": "black"
}

// ~/Library/Application Support/Code/User/settings.json (personal)
{
  "editor.fontSize": 14
}
```

### 4. Document Custom Keybindings

If you customize keyboard shortcuts, document them in README or wiki.

### 5. Test Setup on Fresh Machine

Before sharing project, test setup instructions on clean machine/VM.

---

## 📋 Setup Checklist

Use this checklist when setting up a new project:

- [ ] IDE installed (VSCode/JetBrains/etc.)
- [ ] Claude Code extension/plugin installed
- [ ] Language extensions installed
- [ ] Conflicting extensions removed (Copilot, etc.)
- [ ] `.vscode/` directory created (or JetBrains equivalent)
- [ ] `settings.json` configured
- [ ] `extensions.json` configured
- [ ] `launch.json` configured (optional)
- [ ] `tasks.json` configured (optional)
- [ ] Python interpreter selected (or language runtime)
- [ ] Formatter working (format on save)
- [ ] Linter working (shows errors)
- [ ] Tests discoverable (test explorer)
- [ ] Debugger working (breakpoints work)
- [ ] Claude Code panel accessible
- [ ] @-mention files working in Claude
- [ ] Git integration working

---

**Template Version**: 1.0
**Last Updated**: 2025-10-09
**For**: Any project type, any IDE
**Maintainer**: Copy to your project and customize!
