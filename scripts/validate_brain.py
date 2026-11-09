#!/usr/bin/env python3
"""
Validates the Obsidian Second Brain:
1. Verifies required PARA directories and files (brain/index.md, brain/log.md).
2. Verifies that all [[wikilinks]] resolve to existing files or directories.
"""

import sys
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
BRAIN_DIR = ROOT / "brain"
REQUIRED_DIRS = ["inbox", "projects", "areas", "resources", "archive"]
REQUIRED_FILES = ["index.md", "log.md"]


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
            is_dir_link = target_no_heading.endswith("/")
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
