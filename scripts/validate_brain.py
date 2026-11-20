#!/usr/bin/env python3
"""
Validates the Obsidian Second Brain:
1. Verifies required PARA directories and files (brain/index.md, brain/log.md).
2. Verifies that all [[wikilinks]] resolve to existing files or directories.
3. Verifies note frontmatter (title, date, tags, status) and that the status
   matches the PARA folder holding the note; inbox notes also need a source.
"""

import sys
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
BRAIN_DIR = ROOT / "brain"
REQUIRED_DIRS = ["inbox", "projects", "areas", "resources", "archive"]
REQUIRED_FILES = ["index.md", "log.md"]
REQUIRED_FIELDS = ["title", "date", "tags", "status"]
ALLOWED_STATUS = {
    "inbox": {"inbox"},
    "projects": {"active", "completed"},
    "areas": {"area"},
    "resources": {"resource"},
    "archive": {"archived"},
}
DATE_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def parse_frontmatter(content: str):
    match = re.match(r"^---\s*\n(.*?)\n---\s*\n", content, re.DOTALL)
    if not match:
        return None
    data = {}
    for line in match.group(1).splitlines():
        if ":" in line and not line.startswith((" ", "\t", "#")):
            key, value = line.split(":", 1)
            data[key.strip()] = value.strip().strip("\"'")
    return data


def frontmatter_errors(file_path: Path, content: str) -> list:
    rel = file_path.relative_to(BRAIN_DIR)
    if len(rel.parts) < 2 or rel.parts[0] not in ALLOWED_STATUS:
        return []
    label = file_path.relative_to(ROOT)
    fm = parse_frontmatter(content)
    if fm is None:
        return [f"{label}: Missing YAML frontmatter"]
    problems = [f"{label}: Frontmatter missing '{field}'" for field in REQUIRED_FIELDS if not fm.get(field)]
    if fm.get("date") and not DATE_PATTERN.match(fm["date"]):
        problems.append(f"{label}: Frontmatter date must be YYYY-MM-DD")
    folder = rel.parts[0]
    if fm.get("status") and fm["status"] not in ALLOWED_STATUS[folder]:
        allowed = ", ".join(sorted(ALLOWED_STATUS[folder]))
        problems.append(f"{label}: status '{fm['status']}' not allowed in {folder}/ (expected {allowed})")
    if folder == "inbox" and not fm.get("source"):
        problems.append(f"{label}: Inbox notes need a 'source'")
    if file_path.name != file_path.name.lower() or " " in file_path.name:
        problems.append(f"{label}: Note filenames must be lowercase kebab-case")
    return problems


def validate_brain() -> int:
    if not BRAIN_DIR.exists():
        print(f"Error: {BRAIN_DIR} does not exist", file=sys.stderr)
        return 1

    errors = []

    # 1. Check directories
    for req_dir in REQUIRED_DIRS:
        dpath = BRAIN_DIR / req_dir
        if not dpath.is_dir():
            errors.append(f"Missing required PARA directory: brain/{req_dir}/")

    # 2. Check required files
    for req_file in REQUIRED_FILES:
        fpath = BRAIN_DIR / req_file
        if not fpath.is_file():
            errors.append(f"Missing required brain file: brain/{req_file}")

    # 3. Find and check all markdown files and their wikilinks
    md_files = list(BRAIN_DIR.rglob("*.md"))
    print(f"Scanning {len(md_files)} markdown notes in brain/ for link integrity...")

    wikilink_pattern = re.compile(r"\[\[(.*?)\]\]")

    for file_path in md_files:
        content = file_path.read_text(encoding="utf-8")
        errors.extend(frontmatter_errors(file_path, content))
        matches = wikilink_pattern.findall(content)

        for raw_link in matches:
            # Handle aliases e.g. [[target|label]]
            target = raw_link.split("|")[0].strip()
            # Handle headers e.g. [[target#heading]]
            target_no_heading = target.split("#")[0].strip()

            if not target_no_heading:
                # Link is just a heading within the same file [[#heading]]
                continue

            # Strip trailing slash if present (e.g. [[inbox/]])
            cleaned_target = target_no_heading.rstrip("/")

            # Try direct relative to brain/
            resolved = False
            candidates = [
                BRAIN_DIR / cleaned_target,
                BRAIN_DIR / f"{cleaned_target}.md",
                file_path.parent / cleaned_target,
                file_path.parent / f"{cleaned_target}.md",
            ]

            for cand in candidates:
                if cand.exists():
                    resolved = True
                    break

            if not resolved:
                # Try search across entire brain by filename
                slug = Path(cleaned_target).name
                found = list(BRAIN_DIR.rglob(f"{slug}.md")) or list(BRAIN_DIR.rglob(slug))
                if found:
                    resolved = True

            if not resolved:
                errors.append(
                    f"{file_path.relative_to(ROOT)}: Broken wikilink [[{raw_link}]] (Target not found in vault)"
                )

    if errors:
        print("\nSecond Brain validation FAILED with errors:", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1

    print("All Second Brain notes and wikilinks successfully validated!")
    return 0


if __name__ == "__main__":
    sys.exit(validate_brain())
