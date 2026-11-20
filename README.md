# Financial Ledger & Digital Wallet API

A REST service for user wallets that records every movement of money in a strict double-entry ledger. It provides idempotent transfers, append-only records, a hash-chained audit trail and on-demand reconciliation.

**Specification:** [`docs/srs.pdf`](docs/srs.pdf) · **Status:** Stage 2 (architecture and design) complete. The feature endpoints are scheduled in [`docs/plan.md`](docs/plan.md).

## Stack

Java 21, Javalin, SQLite (WAL, `synchronous=FULL`) through sqlite-jdbc, Jackson, BouncyCastle Argon2id, SLF4J/Logback. Maven builds it through the wrapper. Tests use JUnit 5 and AssertJ.

## Quick start

```bash
./mvnw verify                        # format check, compile (-Werror), tests, coverage gate, jar
java -jar target/financial-ledger-api.jar
curl -i http://127.0.0.1:8080/api/v1/health
```

On Windows use `mvnw.cmd`. The server listens on `127.0.0.1:8080` and stores data in `data/ledger.db` by default. Every setting is an environment variable (`LEDGER_PORT`, `LEDGER_DB_PATH`, `LEDGER_CURRENCIES`, `LEDGER_ADMIN_KEYS`, ...). See [architecture §6](docs/design/architecture.md).

Full repository health check (build, tests, start, probe, stop):

```bash
python3 scripts/local_check.py
```

## Documentation

| Topic | Location |
|-------|----------|
| Architecture | [`docs/design/architecture.md`](docs/design/architecture.md) |
| Data model and ER diagram | [`docs/design/data-model.md`](docs/design/data-model.md) |
| UML class and sequence diagrams | [`docs/design/uml.md`](docs/design/uml.md) |
| API contract (OpenAPI 3) | [`docs/api/openapi.yaml`](docs/api/openapi.yaml) |
| Architecture decisions | [`docs/adr/`](docs/adr/README.md) |
| Plan and task breakdown | [`docs/plan.md`](docs/plan.md) |
| Requirements traceability | [`docs/traceability.md`](docs/traceability.md) |
| Skill evaluations | [`docs/skill_evaluations.md`](docs/skill_evaluations.md) |
| Contributing and releases | [`CONTRIBUTING.md`](CONTRIBUTING.md), [`CHANGELOG.md`](CHANGELOG.md) |

## Second brain and skills

`brain/` is an Obsidian vault organised with PARA. Open that folder as a vault; [`brain/index.md`](brain/index.md) is the entry point. The agent skills live in `skills/` (`brain-ingest`, `brain-refine`, `brain-query`, `local-check`). CI checks them with:

```bash
python3 scripts/validate_brain.py
python3 scripts/validate_skills.py
python3 scripts/validate_adrs.py
```
