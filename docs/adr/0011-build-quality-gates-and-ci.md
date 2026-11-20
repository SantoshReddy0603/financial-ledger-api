# ADR-0011: Build Quality Gates and CI

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** NFR-MNT-2, NFR-MNT-3, NFR-POR-1, PC-8, NFR-SEC-4, SK-1, TQ-5

## Context

PC-8 requires every push and pull request to run tests against a fresh isolated database, plus build, lint/format/static checks, secret scanning, skill validation and brain schema/link validation. NFR-MNT-3 requires one documented formatting style and a static analysis gate with zero errors. NFR-MNT-2 requires 85% line coverage of domain and service layers with documented paths. NFR-POR-1 requires Linux and at least one of Windows or macOS ([[resources/agentskills-specification-standards]], [[resources/hexagonal-architecture-provider-ports]]).

## Decision

**Maven `verify` is the single quality gate**, identical locally and in CI:

| Gate | Tool | Rule |
|------|------|------|
| Format | Spotless with google-java-format 1.28.0 | `spotless:check` fails on any drift; `./mvnw spotless:apply` fixes it |
| Static analysis | `javac -Xlint:all -Werror` | any compiler warning fails the build |
| Dependency hygiene | Maven Enforcer | dependency convergence, Java ≥ 21, Maven ≥ 3.9 |
| Tests | Surefire with JUnit 5 | unit, integration and system tests; every test that needs a database creates its own under JUnit `@TempDir` |
| Coverage | JaCoCo 0.8.14 | ≥ 85% line coverage over `org/ledgerapi/domain/**` and `org/ledgerapi/application/**`; no exclusions |

**GitHub Actions** (`.github/workflows/ci.yml`) on every push and pull request:

1. `java` job, matrix `ubuntu-latest` and `windows-latest`, Temurin 21, `./mvnw -B verify`.
2. `repository-checks` job: `validate_skills.py`, `validate_brain.py`, `validate_adrs.py`, flake8 on `scripts/`, and a check that no `float`/`double` appears in domain or application sources.
3. `secret-scan` job: gitleaks over the full history.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Checkstyle / PMD / SpotBugs | Useful, but overlapping rules and false positives cost time; `-Xlint -Werror` plus formatting is the documented gate. Can be added later by ADR. |
| Coverage on all packages | HTTP and SQLite adapters are covered by system and integration tests; the SRS scopes the gate to domain and services. |
| macOS leg | Windows already satisfies NFR-POR-1 and catches path and line-ending issues. |

## Consequences

* A contributor runs `./mvnw verify` (or the `local-check` skill) and gets exactly CI's verdict.
* Windows CI time roughly doubles total minutes; acceptable for portability evidence.

## Brain Notes

* [[resources/agentskills-specification-standards]]
* [[resources/hexagonal-architecture-provider-ports]]
