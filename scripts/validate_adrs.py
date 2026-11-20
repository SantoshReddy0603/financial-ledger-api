#!/usr/bin/env python3
"""
Validates Architecture Decision Records in docs/adr/ (PC-3, SB-3):
1. Files are named NNNN-kebab-title.md with unique, consecutive numbers from 0001.
2. Each ADR has a level-1 title, a Status line and the sections
   Context, Decision and Consequences.
3. Each ADR cites at least one existing brain note as a [[wikilink]].
The template (0000-template.md) is skipped.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ADR_DIR = ROOT / "docs" / "adr"
BRAIN_DIR = ROOT / "brain"
NAME = re.compile(r"^(\d{4})-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
WIKILINK = re.compile(r"\[\[([^\]|#]+)(?:#[^\]|]*)?(?:\|[^\]]*)?\]\]")
STATUS = re.compile(r"^\*\*Status:\*\*\s*(Proposed|Accepted|Superseded by ADR-\d{4}|Deprecated)\s*$", re.MULTILINE)
REQUIRED_SECTIONS = ["Context", "Decision", "Consequences"]


def note_exists(target: str) -> bool:
    cleaned = target.strip().rstrip("/")
    if (BRAIN_DIR / f"{cleaned}.md").is_file():
        return True
    return any(BRAIN_DIR.rglob(f"{Path(cleaned).name}.md"))


def validate_adrs() -> int:
    if not ADR_DIR.is_dir():
        print(f"Error: {ADR_DIR.relative_to(ROOT)} does not exist", file=sys.stderr)
        return 1
    errors = []
    numbers = []
    files = sorted(p for p in ADR_DIR.glob("*.md") if p.name != "0000-template.md" and p.name != "README.md")
    print(f"Validating {len(files)} ADR(s)...")
    for path in files:
        label = path.relative_to(ROOT)
        match = NAME.match(path.name)
        if not match:
            errors.append(f"{label}: File name must look like NNNN-kebab-title.md")
            continue
        numbers.append(int(match.group(1)))
        content = path.read_text(encoding="utf-8")
        if not re.search(r"^# ADR-\d{4}: .+", content, re.MULTILINE):
            errors.append(f"{label}: First heading must look like '# ADR-NNNN: Title'")
        if not STATUS.search(content):
            errors.append(f"{label}: Missing or invalid '**Status:**' line")
        for section in REQUIRED_SECTIONS:
            if not re.search(rf"^## {section}\b", content, re.MULTILINE):
                errors.append(f"{label}: Missing '## {section}' section")
        if not any(note_exists(t) for t in WIKILINK.findall(content)):
            errors.append(f"{label}: Must cite at least one existing brain note as [[wikilink]]")
    if numbers and sorted(numbers) != list(range(1, len(numbers) + 1)):
        errors.append("docs/adr: ADR numbers must be unique and consecutive from 0001")
    if errors:
        print("\nADR validation FAILED with errors:", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1
    print("All ADRs successfully validated!")
    return 0


if __name__ == "__main__":
    sys.exit(validate_adrs())
