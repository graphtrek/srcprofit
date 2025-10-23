# Kaizen Local Customizations

This directory contains project-specific Kaizen customizations.

## Structure

- `agents/` - Project-specific AI agents
- `skills/` - Project-specific skills
- `commands/` - Project-specific slash commands

## Priority

Local customizations take precedence over Kaizen base files:
1. `kaizen.local/` (highest priority - project-specific)
2. `kaizen/stacks/{stack}/` (stack-specific)
3. `kaizen/base/` (lowest priority - universal)

## Usage

Place custom agents, skills, or commands here. They will be symlinked
to `.claude/` directory by `kaizen-configure.sh`.
