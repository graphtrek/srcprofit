#!/usr/bin/env python3
"""
Auto-generate docs/issues/README.md from issue files.

Usage:
    python scripts/update_issue_index.py

Scans all ISSUE-*.md files in docs/issues/, extracts metadata,
and generates an up-to-date README.md with statistics and categorization.
"""

import re
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional


@dataclass
class Issue:
    """Represents a parsed issue with metadata."""

    number: int
    title: str
    status: str
    priority: str
    created: str
    completed: Optional[str]
    category: Optional[str]
    estimated: Optional[str]
    actual: Optional[str]
    related: list[str]
    blocks: list[str]
    blocked_by: list[str]
    milestone: Optional[str]
    file_path: Path

    @property
    def is_open(self) -> bool:
        """Check if issue is still open."""
        return self.status in ("OPEN", "IN_PROGRESS", "BLOCKED")

    @property
    def is_closed(self) -> bool:
        """Check if issue is fully closed."""
        return self.status == "CLOSED"

    @property
    def is_partial(self) -> bool:
        """Check if issue is partially complete."""
        return self.status == "PARTIAL"


def extract_field(content: str, field: str, default: str = "") -> str:
    """Extract a single-line field from issue header."""
    pattern = rf"\*\*{field}\*\*:\s*(.+?)(?:\n|$)"
    match = re.search(pattern, content, re.MULTILINE)
    if not match:
        return default

    value = match.group(1).strip()

    # Clean up emojis and status variations for Status field
    if field == "Status":
        # Remove emojis
        value = re.sub(r"[✅⚠️🔴❌]", "", value).strip()
        # Extract primary status word
        value = value.split("(")[0].strip()  # Remove parenthetical notes
        # Normalize RESOLVED to CLOSED
        if value == "RESOLVED":
            value = "CLOSED"

    return value


def extract_list_field(content: str, field: str) -> list[str]:
    """Extract comma-separated list field from issue header."""
    value = extract_field(content, field)
    if not value or value in ("-", "None", "N/A"):
        return []
    return [item.strip() for item in value.split(",")]


def parse_issue_file(file_path: Path) -> Issue:
    """Parse an issue file and extract metadata."""
    content = file_path.read_text()

    # Extract issue number from filename
    match = re.search(r"ISSUE-(\d+)", file_path.name)
    if not match:
        raise ValueError(f"Invalid issue filename: {file_path.name}")
    number = int(match.group(1))

    # Extract title from first heading
    title_match = re.search(r"^#\s+ISSUE-\d+:\s*(.+)$", content, re.MULTILINE)
    title = title_match.group(1).strip() if title_match else "Unknown"

    return Issue(
        number=number,
        title=title,
        status=extract_field(content, "Status", "UNKNOWN"),
        priority=extract_field(content, "Priority", "MEDIUM"),
        created=extract_field(content, "Created", "Unknown"),
        completed=extract_field(content, "Completed") or None,
        category=extract_field(content, "Category") or None,
        estimated=extract_field(content, "Estimated") or None,
        actual=extract_field(content, "Actual") or None,
        related=extract_list_field(content, "Related"),
        blocks=extract_list_field(content, "Blocks"),
        blocked_by=extract_list_field(content, "Blocked By"),
        milestone=extract_field(content, "Milestone") or None,
        file_path=file_path,
    )


def generate_readme(issues: list[Issue]) -> str:
    """Generate README.md content from parsed issues."""
    # Statistics
    total = len(issues)
    open_issues = [i for i in issues if i.is_open]
    closed_issues = [i for i in issues if i.is_closed]
    partial_issues = [i for i in issues if i.is_partial]

    open_count = len(open_issues)
    closed_count = len(closed_issues)
    partial_count = len(partial_issues)
    open_pct = (open_count / total * 100) if total > 0 else 0
    closed_pct = (closed_count / total * 100) if total > 0 else 0

    # Detect project name from git repo or directory name
    import subprocess
    try:
        result = subprocess.run(
            ["git", "config", "--get", "remote.origin.url"],
            capture_output=True,
            text=True,
            check=True
        )
        # Extract project name from git URL (e.g., .../contrarian.git -> Contrarian)
        url = result.stdout.strip()
        project_name = url.rstrip("/").split("/")[-1].replace(".git", "")
        project_name = project_name.capitalize()
    except:
        # Fallback: use directory name
        project_name = Path(__file__).parent.parent.name.capitalize()

    # Generate content
    lines = [
        f"# Issue Tracking - {project_name}",
        "",
        "**Auto-generated** - Run `python scripts/update_issue_index.py` to update",
        "",
        f"**Total**: {total} issues",
        f"**Open**: {open_count} ({open_pct:.0f}%)",
        f"**Closed**: {closed_count} ({closed_pct:.0f}%)",
        f"**Partial**: {partial_count}",
        "",
        "---",
        "",
    ]

    # Open issues by priority
    if open_issues:
        lines.append("## Open Issues")
        lines.append("")

        # Group by priority
        priority_order = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
        for priority in priority_order:
            priority_issues = [i for i in open_issues if i.priority == priority]
            if not priority_issues:
                continue

            lines.append(f"### {priority} ({len(priority_issues)})")
            lines.append("")

            for issue in sorted(priority_issues, key=lambda i: i.number):
                lines.append(
                    f"- **[ISSUE-{issue.number:03d}]({issue.file_path.name})** " f"- {issue.title}"
                )
                meta = [f"Priority: {issue.priority}", f"Status: {issue.status}"]
                if issue.category:
                    meta.append(f"Category: {issue.category}")
                if issue.created:
                    meta.append(f"Created: {issue.created}")
                if issue.estimated:
                    meta.append(f"Est: {issue.estimated}")
                if issue.blocked_by:
                    meta.append(f"Blocked by: {', '.join(issue.blocked_by)}")
                if issue.blocks:
                    meta.append(f"Blocks: {', '.join(issue.blocks)}")

                lines.append(f"  - {' | '.join(meta)}")
            lines.append("")

        lines.append("---")
        lines.append("")

    # Partial issues
    if partial_issues:
        lines.append("## Partial Completion")
        lines.append("")
        for issue in sorted(partial_issues, key=lambda i: i.number):
            lines.append(
                f"- **[ISSUE-{issue.number:03d}]({issue.file_path.name})** " f"- {issue.title}"
            )
            meta = [f"Priority: {issue.priority}"]
            if issue.created:
                meta.append(f"Created: {issue.created}")
            lines.append(f"  - {' | '.join(meta)}")
        lines.append("")
        lines.append("---")
        lines.append("")

    # Closed issues
    if closed_issues:
        lines.append("## Closed Issues")
        lines.append("")
        for issue in sorted(closed_issues, key=lambda i: i.number):
            lines.append(
                f"- **[ISSUE-{issue.number:03d}]({issue.file_path.name})** " f"- {issue.title}"
            )
            meta = [f"Priority: {issue.priority}"]
            if issue.completed:
                meta.append(f"Completed: {issue.completed}")
            if issue.actual:
                meta.append(f"Actual: {issue.actual}")
            lines.append(f"  - {' | '.join(meta)}")
        lines.append("")
        lines.append("---")
        lines.append("")

    # By category
    lines.append("## By Category")
    lines.append("")
    categories = {}
    for issue in issues:
        cat = issue.category or "Uncategorized"
        if cat not in categories:
            categories[cat] = []
        categories[cat].append(issue)

    for category in sorted(categories.keys()):
        cat_issues = categories[category]
        lines.append(f"**{category}** ({len(cat_issues)}):")
        issue_refs = []
        for issue in sorted(cat_issues, key=lambda i: i.number):
            status_icon = "✅" if issue.is_closed else "⚠️" if issue.is_partial else "🔴"
            issue_refs.append(f"ISSUE-{issue.number:03d} {status_icon}")
        lines.append(f"  - {', '.join(issue_refs)}")
        lines.append("")

    lines.append("---")
    lines.append("")

    # Footer
    today = datetime.now().strftime("%Y-%m-%d")
    lines.append(f"**Last updated**: {today}")
    lines.append("")
    lines.append("**How to update**: Run `python scripts/update_issue_index.py`")
    lines.append("")

    return "\n".join(lines)


def main():
    """Main entry point."""
    # Find all issue files
    issues_dir = Path(__file__).parent.parent / "docs" / "issues"
    issue_files = sorted(issues_dir.glob("ISSUE-*.md"))

    if not issue_files:
        print("No issue files found in docs/issues/")
        return 1

    print(f"Found {len(issue_files)} issue files")

    # Parse all issues
    issues = []
    for file_path in issue_files:
        try:
            issue = parse_issue_file(file_path)
            issues.append(issue)
            print(f"  ✓ ISSUE-{issue.number:03d}: {issue.title} [{issue.status}]")
        except Exception as e:
            print(f"  ✗ {file_path.name}: {e}")
            return 1

    # Generate README
    readme_content = generate_readme(issues)
    readme_path = issues_dir / "README.md"
    readme_path.write_text(readme_content)

    print(f"\n✅ Generated {readme_path}")
    print("\nSummary:")
    print(f"  Total: {len(issues)}")
    print(f"  Open: {len([i for i in issues if i.is_open])}")
    print(f"  Closed: {len([i for i in issues if i.is_closed])}")
    print(f"  Partial: {len([i for i in issues if i.is_partial])}")

    return 0


if __name__ == "__main__":
    exit(main())
