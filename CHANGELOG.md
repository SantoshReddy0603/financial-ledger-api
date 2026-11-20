# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow Semantic Versioning.

## [Unreleased]

### Added
- Stage 2 design: architecture, data model, UML class and sequence diagrams, OpenAPI 3 contract (`docs/design/`, `docs/api/`).
- ADR-0001 to ADR-0012, each citing brain notes, plus `scripts/validate_adrs.py`.
- Development plan and requirements traceability matrix (`docs/plan.md`, `docs/traceability.md`).
- Skills `brain-refine`, `brain-query` and `local-check` with `scripts/local_check.py`.
- Java 21 / Maven foundation: configuration, SQLite storage with WAL and a single serialized writer, schema V1 with append-only triggers, error catalogue, request ids, structured request log and `GET /api/v1/health`.
- CI for Linux and Windows builds, repository checks and a secret scan.

### Changed
- The submitted SRS (`docs/srs.pdf`) is the authoritative specification; the technology stack is Java 21 with Maven.
- Brain and skill validators check frontmatter and reject secrets.

## [stage-1] - 2026-11-16

### Added
- Second brain vault with 10 research notes, `brain-ingest` skill, SRS baseline and CI validation.
