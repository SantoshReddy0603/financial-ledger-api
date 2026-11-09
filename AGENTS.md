# AGENTS.md — Financial Ledger & Digital Wallet API

This repository implements a **Financial Ledger & Digital Wallet API** using strict double-entry bookkeeping, ACID transactions, and idempotency guarantees, developed under the **AI-Assisted Software Development Life Cycle (SDLC)**.

All AI coding assistants and human developers working in this repository must adhere to the rules, structures, and verification workflows defined below.

---

## 1. Core System Invariants (Non-Negotiable)

1. **Strict Double-Entry Invariant:** Every financial transaction must be composed of balanced ledger entries where total debits equal total credits ($\sum \text{debits} = \sum \text{credits} = 0$). Single-sided balance increments/decrements are forbidden.
2. **Immutable Audit Trail:** Ledger records are append-only (`INSERT` only). `UPDATE` or `DELETE` on ledger entries is prohibited at the database and application levels. Cancellations or corrections require explicit reversal transactions.
3. **No Floating-Point Arithmetic:** Monetary values must strictly use 64-bit signed integer minor units (e.g., cents, paise, satoshis). No floating-point types (`float`, `double`, unconstrained JSON numbers) may be used for monetary calculations or storage.
4. **Idempotency Guarantee:** All transfer endpoints require an `Idempotency-Key` header. Requests are hashed against client identity, endpoint, and payload (`SHA-256`). Replayed requests with identical payloads return the cached response; replayed keys with mutated payloads are rejected with HTTP `422/409`.
5. **Concurrency Safety:** Database operations must use SQLite in Write-Ahead Logging (`WAL`) mode with explicit `BEGIN IMMEDIATE` transactions to prevent dirty reads, phantom writes, and race conditions during concurrent wallet updates.

---

## 2. Second Brain Architecture (Obsidian Vault: `brain/`)

Externalized memory is maintained in the `brain/` directory following the **PARA** method:
* `brain/inbox/`: Unfiltered raw intake notes, summaries, and initial research.
* `brain/projects/`: Active deliverables, milestone specifications, and task trackers.
* `brain/areas/`: Long-term technical responsibilities (e.g., Ledger Core, Concurrency, Security).
* `brain/resources/`: Reusable technical guides, architectural research, and references.
* `brain/archive/`: Completed stage records and frozen artifacts.
* `brain/index.md`: Vault entry point, status dashboard, and navigation map.
* `brain/log.md`: Chronological log of all agent runs, operations, and modifications.

### Brain Rules:
* **SB-1:** Bidirectional [[wikilinks]] must link related notes, requirements (`FR-1`), GitHub issues, and PRs.
* **SB-2:** A review note must be authored at the conclusion of each SDLC stage.
* **SB-3:** Every ADR in `docs/adr/` and every skill in `skills/` must cite at least one distilled note in `brain/`.
* **SB-5:** All modifications to `brain/` must go through version control branches and pull requests.
* **Validation:** No broken `[[wikilinks]]` are permitted. Validated via `scripts/validate_brain.py`.

---

## 3. Agent Skills Standard (`skills/`)

Skills follow the [agentskills.io](https://agentskills.io) specification. Each skill lives in its own subdirectory containing a `SKILL.md` file with standard YAML frontmatter:

```yaml
---
name: skill-name
description: Clear, single-sentence purpose description.
---
```

### Registered Skills:
1. `brain-ingest`: Ingests raw technical documentation, research papers, or specs into structured notes in `brain/inbox/`.
2. `brain-refine`: Triages inbox notes into PARA, reconciles duplicates, and verifies link integrity.
3. `brain-query`: Queries the second brain vault to draft specs, ADRs, or answer architectural questions.
4. `local-check`: Validates full repository health (build, test suite, server startup, endpoint ping).
5. `simulate-transfer`: Product verification skill that executes wallet creation, transfers, concurrent races, and ledger reconciliation.

---

## 4. Traceability Chain

Every code commit, test, and documentation artifact must preserve the chain:
$$\text{Requirement (FR/NFR/SK)} \longrightarrow \text{GitHub Issue} \longrightarrow \text{Pull Request} \longrightarrow \text{Automated Test} \longrightarrow \text{Agent Skill} \longrightarrow \text{Brain Note}$$

---

## 5. Development & CI Commands

* **Run Test Suite:** `pytest tests/ -v`
* **Run Linting:** `flake8 src/ tests/ scripts/` or `ruff check .`
* **Validate Brain Links:** `python3 scripts/validate_brain.py`
* **Validate Skills:** `python3 scripts/validate_skills.py`
* **Run Local Health Check:** Follow `skills/local-check/SKILL.md`
