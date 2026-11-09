---
title: "Agent Skills Specification and Evaluation Standards"
date: 2026-11-09
tags: [agentskills, standards, ai-assisted-sdlc, automation]
source: "agentskills.io Specification; Anthropics Claude Code Skill Standards"
status: resource
---

# Agent Skills Specification and Evaluation Standards

## 1. Summary & Core Concept
An Agent Skill is a modular, portable engineering capability codified as executable documentation. Rather than relying on loose natural language prompts, skills package explicit steps, verification triggers, parameter schemas, and error handling into a predictable standard.

## 2. Key Architectural Mechanisms
* **Folder & File Structure:**
  Each skill resides in `skills/<skill-name>/` and contains a single `SKILL.md`:
  ```
  skills/
  ├── brain-ingest/
  │   └── SKILL.md
  ├── brain-refine/
  │   └── SKILL.md
  ├── brain-query/
  │   └── SKILL.md
  ├── local-check/
  │   └── SKILL.md
  └── simulate-transfer/
      └── SKILL.md
  ```
* **Frontmatter Schema:**
  Must begin with YAML delimiters `---` and contain strictly:
  * `name`: Lowercase kebab-case matching the parent folder name.
  * `description`: Concise summary of what the skill accomplishes and when an agent must trigger it.
* **Evaluation Standard (TQ-7):**
  * Before stage submission, every skill must be executable by an agent or script without manual intervention.
  * Inputs and outputs must be validated deterministically.

## 3. Pitfalls & Failure Modes
* **Mismatched Directory and Frontmatter Name:** Placing a skill in `skills/check/SKILL.md` while frontmatter specifies `name: local-check`.
* **Ambiguous Steps:** Telling the agent to "verify the app works" without giving concrete CLI commands and expected pass/fail status codes.

## 4. Relevance to Financial Ledger API
* Satisfies **SK-1 to SK-5** requirements in the SRS.
* Mandated by **TQ-7 (Skill Evaluations)** and the SDLC Common Skills requirement.

## 5. Wikilinks
* [[index]]
* [[resources/rest-api-design-financial-services|RESTful API Design for Financial Services]]
