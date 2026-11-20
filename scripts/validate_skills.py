#!/usr/bin/env python3
"""
Validates that all agent skills conform to the agentskills.io specification:
1. Each skill lives in skills/<skill-name>/SKILL.md
2. Folder name matches the frontmatter 'name' property.
3. Frontmatter contains 'name' and 'description'.
4. The skill cites at least one existing brain note via a [[wikilink]] (SK-1, SB-3).
5. The skill contains nothing that looks like a credential (SK-8).
"""

import sys
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
SKILLS_DIR = ROOT / "skills"
BRAIN_DIR = ROOT / "brain"
WIKILINK = re.compile(r"\[\[([^\]|#]+)(?:#[^\]|]*)?(?:\|[^\]]*)?\]\]")
SECRET_PATTERNS = [
    re.compile(r"(?i)(api[_-]?key|password|secret|token)\s*[:=]\s*['\"]?[A-Za-z0-9_\-]{20,}"),
    re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36}\b"),
]


def resolves_to_brain_note(target: str) -> bool:
    cleaned = target.strip().rstrip("/")
    if not cleaned:
        return False
    if (BRAIN_DIR / f"{cleaned}.md").is_file() or (BRAIN_DIR / cleaned).is_file():
        return True
    return any(BRAIN_DIR.rglob(f"{Path(cleaned).name}.md"))


def parse_frontmatter(content: str) -> dict:
    match = re.match(r"^---\s*\n(.*?)\n---\s*\n", content, re.DOTALL)
    if not match:
        return {}
    raw_yaml = match.group(1)
    data = {}
    for line in raw_yaml.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if ":" in line:
            key, val = line.split(":", 1)
            data[key.strip()] = val.strip().strip("\"'")
    return data


def validate_skills() -> int:
    if not SKILLS_DIR.exists():
        print(f"Error: {SKILLS_DIR} does not exist", file=sys.stderr)
        return 1

    errors = []
    skill_dirs = [d for d in SKILLS_DIR.iterdir() if d.is_dir() and not d.name.startswith(".")]

    if not skill_dirs:
        print("Warning: No skills found under skills/")
        return 0

    print(f"Validating {len(skill_dirs)} skill(s)...")

    for sdir in skill_dirs:
        skill_file = sdir / "SKILL.md"
        if not skill_file.exists():
            errors.append(f"Missing SKILL.md in {sdir.relative_to(ROOT)}")
            continue

        content = skill_file.read_text(encoding="utf-8")
        fm = parse_frontmatter(content)

        if not fm:
            errors.append(f"{skill_file.relative_to(ROOT)}: Invalid or missing YAML frontmatter (must start with '---')")
            continue

        name = fm.get("name")
        description = fm.get("description")

        if not name:
            errors.append(f"{skill_file.relative_to(ROOT)}: Frontmatter missing required 'name' field")
        elif name != sdir.name:
            errors.append(
                f"{skill_file.relative_to(ROOT)}: Frontmatter name '{name}' does not match directory '{sdir.name}'"
            )

        if not description:
            errors.append(f"{skill_file.relative_to(ROOT)}: Frontmatter missing required 'description' field")

        citations = [t for t in WIKILINK.findall(content) if resolves_to_brain_note(t)]
        if not citations:
            errors.append(f"{skill_file.relative_to(ROOT)}: Must cite at least one existing brain note as [[wikilink]]")

        for pattern in SECRET_PATTERNS:
            if pattern.search(content):
                errors.append(f"{skill_file.relative_to(ROOT)}: Contains text that looks like a credential")
                break

    if errors:
        print("\nSkill validation FAILED with errors:", file=sys.stderr)
        for err in errors:
            print(f"  - {err}", file=sys.stderr)
        return 1

    print("All skills successfully validated against agentskills.io format!")
    return 0


if __name__ == "__main__":
    sys.exit(validate_skills())
