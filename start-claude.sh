#!/bin/bash
# SrcProfit - Claude venv Wrapper
# Starts Claude inside Python 3.12 virtual environment

set -e  # Exit on error

# Get script directory (project root)
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 Starting Claude in srcprofit venv..."
echo "📁 Project: $PROJECT_DIR"

# Add GNU coreutils to PATH (if available)
if [ -d "/opt/homebrew/opt/coreutils/libexec/gnubin" ]; then
    export PATH="/opt/homebrew/opt/coreutils/libexec/gnubin:$PATH"
    echo "✅ GNU coreutils added to PATH"
fi

# Preflight check: Verify required tools
echo "🔍 Preflight check..."
MISSING_TOOLS=""
for tool in git python3 docker uv gh; do
    if ! command -v $tool &> /dev/null; then
        MISSING_TOOLS="$MISSING_TOOLS $tool"
    fi
done

if [ -n "$MISSING_TOOLS" ]; then
    echo "❌ Error: Missing required tools:$MISSING_TOOLS"
    echo "Install with: brew install$MISSING_TOOLS"
    exit 1
fi
echo "✅ All required tools installed"

# Change to project directory
cd "$PROJECT_DIR"

# Check if venv exists, create if missing
if [ ! -f "venv/bin/activate" ]; then
    echo "⚠️  Virtual environment not found, creating..."
    echo "📦 Running 'make setup' to create venv..."
    make setup
    if [ $? -ne 0 ]; then
        echo "❌ Error: Failed to create virtual environment"
        exit 1
    fi
fi

# Activate virtual environment
source venv/bin/activate
echo "✅ Virtual environment activated (Python $(python --version))"

# Verify Python version
PYTHON_VERSION=$(python --version 2>&1 | grep -oE '[0-9]+\.[0-9]+')
if [[ "$PYTHON_VERSION" != "3.12" ]]; then
    echo "⚠️  Warning: Expected Python 3.12, got $PYTHON_VERSION"
fi

# Start Claude
echo "🤖 Launching Claude Code..."
claude "$@"
