---
name: brain-ingest
description: Ingests technical documentation, articles, or research materials into structured markdown notes in the Obsidian Second Brain inbox.
---

# Brain Ingest Skill

This skill defines the standardized operational procedure for ingesting technical documentation, specifications, research papers, and architectural articles into the Obsidian Second Brain (`brain/`).

---

## 1. Trigger Conditions

Execute this skill when:
* Adding new technical research regarding double-entry accounting, SQLite concurrency, idempotency, or security.
* Processing documentation for a library, protocol (e.g., IETF HTTP standards), or framework.
* Capturing bug investigations, defect pre-mortems, or benchmark results.

---

## 2. Ingestion Workflow

1. **Analyze Source Material:**
   * Read the input text or file.
   * Extract the primary thesis, technical mechanisms, architectural trade-offs, and failure modes.
   * Identify direct relevance to Financial Ledger requirements (`FR-1` to `FR-5`, `NFR-1` to `NFR-6`).

2. **Generate Structured Note:**
   * Create a new file in `brain/inbox/<slug-title>.md`.
   * Include standard YAML frontmatter:
     ```yaml
     ---
     title: "Exact Descriptive Title"
     date: YYYY-MM-DD
     tags: [tag1, tag2, research]
     source: "URL or citation path"
     status: inbox
     ---
     ```
   * Populate note sections:
     * **Summary & Core Concept:** Concise explanation of the technology or concept.
     * **Key Architectural Mechanisms:** Concrete mechanics, code patterns, or math formulas.
     * **Pitfalls & Failure Modes:** What happens when misconfigured or naively implemented.
     * **Relevance to Financial Ledger API:** Explicit mapping to `FR-X` / `NFR-X` requirements.
     * **Wikilinks & References:** Bidirectional links to `[[index]]`, areas, or related notes.

3. **Log the Action:**
   * Append a summary line to `brain/log.md` detailing the title, source, and target file created.

4. **Verify Integrity:**
   * Ensure file name is lowercase kebab-case.
   * Run link check to ensure all new `[[wikilinks]]` either exist or are designated placeholders.
