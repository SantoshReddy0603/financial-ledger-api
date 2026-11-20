---
name: brain-refine
description: Triages notes in brain/inbox into the PARA folders, merges duplicates, repairs wikilinks and updates the vault index and log so the second brain stays navigable and valid.
---

# Brain Refine Skill

Turns raw intake into distilled, linked knowledge. Implements SK-5 and keeps PC-1 and SB-1 true after every change.

---

## 1. Trigger Conditions

Run this skill when:
* `brain/inbox/` contains any note (the inbox should be empty at every stage end).
* Two notes cover the same topic, or a search returns overlapping answers.
* `python3 scripts/validate_brain.py` reports broken links or bad frontmatter.
* Before a stage review note is written (SB-2).

---

## 2. Classification Rules

Move each inbox note to exactly one folder and set `status` accordingly. The validator enforces the pairing.

| Destination | `status` | Holds |
|-------------|----------|-------|
| `brain/resources/` | `resource` | Reusable research tied to a source: standards, papers, library docs |
| `brain/areas/` | `area` | An ongoing responsibility that groups resources (Concurrency, Security, Authentication) |
| `brain/projects/` | `active` or `completed` | A stage tracker, design record or review with a deadline |
| `brain/archive/` | `archived` | Superseded notes and frozen stage records; never deleted |

If a note fits none of these it is not ready: leave it in the inbox and list what is missing.

---

## 3. Procedure

1. **List the inbox.** `ls brain/inbox/` (ignore `.gitkeep`).
2. **For each note:**
   1. Check the frontmatter has `title`, `date` (YYYY-MM-DD), `tags`, `source` and the sections required by `brain-ingest`.
   2. Search for overlap: `grep -ril "<key term>" brain/resources brain/areas`.
   3. **Duplicate found:** merge the new facts into the existing note, keep both sources in `source`, add the newer date to the body, and move the inbox note to `brain/archive/` with `status: archived` and a line `Merged into [[resources/<existing>]]`. Do not delete it.
   4. **No duplicate:** `git mv` it to the destination folder, change `status`, and keep the file name (lowercase kebab-case).
   5. Add the note to its area's "Related Resources" list, and add the area link to the note's Wikilinks section (bidirectional, SB-1).
   6. Check every FR/NFR ID the note mentions exists in the SRS.
3. **Update the index.** Add or adjust the entry in `brain/index.md` under the matching PARA heading.
4. **Validate.** `python3 scripts/validate_brain.py` must exit 0.
5. **Log.** Append one entry to `brain/log.md`: date, notes moved, merges made, links repaired, validator result.
6. **Submit through a pull request** (PC-5, SB-5). The PR description lists each note moved and why.

---

## 4. Link Repair

* A link that targets a moved note keeps working when written by file name (`[[canonical-json-and-hash-chains]]`); prefer folder-qualified links (`[[resources/...]]`) for clarity and update them after a move.
* Never satisfy the validator by creating an empty target note. Either point the link at the real note or remove it.

---

## 5. Output

The run leaves an empty inbox (or a documented reason per note left), zero validator errors, an updated index, and one log entry.

---

## 6. Brain Citations

* [[resources/agentskills-specification-standards|Agent Skills Specification and Evaluation Standards]]
* [[index]]: the vault structure this skill maintains.
* [[log]]: where every run is recorded.
