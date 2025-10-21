# Dagger CI/CD Setup Guide

**Purpose**: Portable, container-based CI/CD without GitHub Actions dependency

**Created**: 2025-10-10 (Session 48)

---

## 🎯 Why Dagger?

**Problem**: No access to hosted CI/CD (GitHub Actions, GitLab CI, etc.)

**Solution**: Dagger - Local container-based CI/CD that runs everywhere

### Benefits:
- ✅ **100% local** - No cloud dependency
- ✅ **Container-based** - Reproducible, isolated environment
- ✅ **Portable** - Same pipeline works on any machine (Mac, Linux, Windows)
- ✅ **Fast** - Docker layer caching speeds up repeated runs
- ✅ **Language agnostic** - Use for Python, Go, Rust, Node, etc.
- ✅ **One skill** - Learn once, use everywhere

---

## 📦 Installation

### 1. Install Dagger CLI

```bash
# macOS
brew install dagger

# Linux
curl -L https://dl.dagger.io/dagger/install.sh | sh

# Windows
choco install dagger
```

### 2. Verify Installation

```bash
dagger version
# Should show: dagger v0.19.x or newer
```

---

## 🚀 Quick Start

### Initialize Dagger Module

```bash
# In your project root
dagger init --sdk=python --name=yourproject
```

This creates:
- `.dagger/` - Dagger module directory
- `.dagger/src/yourproject/main.py` - Pipeline definition

### Create CI Pipeline

Edit `.dagger/src/yourproject/main.py`:

```python
"""Your Project CI/CD Pipeline"""

import dagger
from dagger import dag, function, object_type


@object_type
class Yourproject:
    """CI/CD pipeline"""

    @function
    async def test(self, source: dagger.Directory) -> str:
        """Run test suite with coverage"""
        return await (
            dag.container()
            .from_("python:3.12-slim")
            .with_directory("/src", source)
            .with_workdir("/src")
            .with_exec(["pip", "install", "-r", "requirements.txt"])
            .with_exec(["pip", "install", "pytest", "pytest-cov"])
            .with_exec([
                "pytest",
                "--cov=src",
                "--cov-fail-under=80",
                "-v"
            ])
            .stdout()
        )

    @function
    async def lint(self, source: dagger.Directory) -> str:
        """Run linting checks"""
        container = (
            dag.container()
            .from_("python:3.12-slim")
            .with_directory("/src", source)
            .with_workdir("/src")
            .with_exec(["pip", "install", "black", "isort", "flake8"])
        )

        output = []

        # Black
        output.append("=== Black ===")
        output.append(await container.with_exec(["black", "--check", "src/"]).stdout())

        # isort
        output.append("\n=== isort ===")
        output.append(await container.with_exec(["isort", "--check-only", "src/"]).stdout())

        # flake8
        output.append("\n=== flake8 ===")
        output.append(await container.with_exec(["flake8", "src/"]).stdout())

        return "\n".join(output)

    @function
    async def ci(self, source: dagger.Directory) -> str:
        """Run complete CI pipeline"""
        output = []

        output.append("=" * 80)
        output.append("CI PIPELINE")
        output.append("=" * 80)

        output.append("\n### Tests ###\n")
        output.append(await self.test(source))

        output.append("\n### Lint ###\n")
        output.append(await self.lint(source))

        output.append("\n✅ ALL CHECKS PASSED!")

        return "\n".join(output)
```

---

## 🛠️ Integration with Makefile

Add to your `Makefile`:

```makefile
.PHONY: ci ci-test ci-lint

ci:
	@echo "🚀 Running full CI pipeline (Dagger)..."
	@dagger call ci --source=.

ci-test:
	@echo "🧪 Running tests (Dagger)..."
	@dagger call test --source=.

ci-lint:
	@echo "🔍 Running lint checks (Dagger)..."
	@dagger call lint --source=.

pr:
	@echo "Creating PR..."
	@$(MAKE) ci  # Run CI before creating PR
	@gh pr create --base main --fill
```

---

## 📖 Usage

### Run Full CI Pipeline

```bash
make ci
# Runs all checks in containers
```

### Run Individual Checks

```bash
make ci-test        # Tests only
make ci-lint        # Lint only
```

### Create PR with CI

```bash
make pr
# Automatically runs CI first, blocks if fails
```

---

## 🎓 Advanced Patterns

### Multi-Language Projects

```python
@function
async def test_python(self, source: dagger.Directory) -> str:
    """Test Python service"""
    return await (
        dag.container()
        .from_("python:3.12")
        .with_directory("/src", source)
        .with_workdir("/src/python-service")
        .with_exec(["pytest"])
        .stdout()
    )

@function
async def test_go(self, source: dagger.Directory) -> str:
    """Test Go service"""
    return await (
        dag.container()
        .from_("golang:1.21")
        .with_directory("/src", source)
        .with_workdir("/src/go-service")
        .with_exec(["go", "test", "./..."])
        .stdout()
    )
```

### Security Scans

```python
@function
async def security(self, source: dagger.Directory) -> str:
    """Run security scans"""
    container = (
        dag.container()
        .from_("python:3.12-slim")
        .with_directory("/src", source)
        .with_workdir("/src")
        .with_exec(["pip", "install", "bandit", "safety"])
    )

    output = []

    # Bandit (code security)
    output.append("=== Bandit ===")
    output.append(await container.with_exec(["bandit", "-r", "src/"]).stdout())

    # Safety (dependency vulnerabilities)
    output.append("\n=== Safety ===")
    try:
        output.append(await container.with_exec(["safety", "check"]).stdout())
    except Exception as e:
        output.append(f"Vulnerabilities found (non-blocking): {e}")

    return "\n".join(output)
```

---

## 🐳 How It Works

### Container Caching

Dagger automatically caches Docker layers:

```python
# First run: ~2-3 minutes (downloads base image, installs deps)
.with_exec(["pip", "install", "-r", "requirements.txt"])  # Cached!

# Subsequent runs: ~30s (only changed code re-tested)
.with_directory("/src", source)  # Only this changes
.with_exec(["pytest"])  # Fast!
```

### Source Directory

The `source` parameter is the project directory:

```bash
dagger call test --source=.
# "." = current directory
```

Dagger syncs only changed files, not entire directory.

---

## 🔧 Troubleshooting

### "Client object has no attribute 'host'"

**Problem**: Trying to use `dag.host()` in function defaults

**Solution**: Make `source` a required parameter:

```python
# ❌ Wrong
async def test(self, source: dagger.Directory | None = None):
    if source is None:
        source = dag.host().directory(".")  # Error!

# ✅ Correct
async def test(self, source: dagger.Directory):
    # source passed from CLI: dagger call test --source=.
```

### "No such file or directory"

**Problem**: Trying to access files not in source directory

**Solution**: Ensure files are included in source:

```bash
# Check what's in the container
dagger call test --source=. 2>&1 | grep "with_directory"
```

---

## 📚 Resources

- **Dagger Docs**: https://docs.dagger.io/
- **Python SDK**: https://docs.dagger.io/sdk/python
- **Examples**: https://github.com/dagger/dagger/tree/main/sdk/python/examples

---

## 🎯 Migration from GitHub Actions

### Before (GitHub Actions)

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
      - run: pip install -r requirements.txt
      - run: pytest --cov=src
```

### After (Dagger)

```python
# .dagger/src/yourproject/main.py
@function
async def test(self, source: dagger.Directory) -> str:
    return await (
        dag.container()
        .from_("python:3.12-slim")
        .with_directory("/src", source)
        .with_workdir("/src")
        .with_exec(["pip", "install", "-r", "requirements.txt"])
        .with_exec(["pytest", "--cov=src"])
        .stdout()
    )
```

**Result**: No GitHub dependency, runs 100% locally!

---

## ✨ Best Practices

1. **Use specific base images**: `python:3.12-slim` not `python:latest`
2. **Cache dependencies**: Install requirements before copying source
3. **Keep pipelines simple**: One function per check
4. **Use try-except for non-blocking checks**: Safety, pip-audit
5. **Test locally first**: `dagger call test --source=.` before committing

---

**Created**: 2025-10-10 (Session 48 - Dagger migration)
**Author**: contrarian project team
**License**: Reusable for any project
