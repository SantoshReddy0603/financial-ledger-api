---
name: brain-query
description: Answers architecture and requirements questions from the second brain with explicit note citations, and drafts specs or ADRs whose every claim traces to a brain note or SRS requirement.
---

# Brain Query Skill

Uses the vault as the source of truth for design reasoning. Implements SK-6 and supports PC-3 (every ADR cites a distilled note).

---

## 1. Trigger Conditions

Run this skill when:
* Drafting an ADR, a design section, a test plan or a review note.
* Answering "why did we choose X?" or "what does the SRS require for Y?".
* Checking a proposed change against recorded decisions before implementing it.

---

## 2. Procedure

1. **Frame the question** as one sentence naming the requirement IDs involved, for example "How should FR-3.1 serialize writers on SQLite?".
2. **Search the vault**, areas first, then resources, then projects:
   ```bash
   grep -ril "<term>" brain/areas brain/resources brain/projects
   grep -n "<FR-x.y>" brain/ -r
   ```
   Open the area note for the topic and follow its "Related Resources" links.
3. **Search the decisions:** `grep -il "<term>" docs/adr/*.md`. An accepted ADR outranks a resource note; the SRS (`docs/srs.pdf`) outranks both.
4. **Compose the answer** using only what the notes and the SRS say:
   * Every factual sentence ends with a citation: `[[resources/<note>]]`, `ADR-NNNN` or `SRS FR-x.y`.
   * If the notes disagree, quote both and state which one wins and why (precedence above).
   * If the vault has no answer, say "Not covered in the brain" and propose a `brain-ingest` run with a named source. Do not fill the gap from memory.
5. **When drafting an ADR,** copy `docs/adr/0000-template.md` to the next free number, fill Context, Decision, Alternatives and Consequences from step 4, and put the citations under "Brain notes". Run `python3 scripts/validate_adrs.py`.
6. **Log** the question, notes consulted and output file in `brain/log.md`.

---

## 3. Answer Format

```
Question: <one sentence>
Answer: <2-6 sentences, each with a citation>
Sources: [[note-a]], [[note-b]], ADR-0003, SRS FR-3.1
Gaps: <what the brain could not answer, or "none">
```

---

## 4. Quality Checks

* No claim without a citation; no citation to a note that does not exist (the validators reject broken links).
* Requirement IDs are quoted exactly as in the SRS.
* Drafts are proposals: a human reviews them through a pull request before they are marked Accepted (PC-9).

---

## 5. Brain Citations

* [[index]]: entry point and map of the vault.
* [[areas/ledger-architecture|Area: Double-Entry Ledger Architecture]]
* [[resources/hexagonal-architecture-provider-ports|Hexagonal Architecture and Provider Ports]]
