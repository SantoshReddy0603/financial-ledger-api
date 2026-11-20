---
name: local-check
description: Builds the service, runs the full automated test suite, starts it against a disposable database, probes the health endpoint and shuts it down, reporting PASS, FAIL or SKIP for each step.
---

# Local Check Skill

Answers one question before a commit, a pull request or a stage submission: does this checkout build, pass its tests and actually start? Implements SK-2.

---

## 1. Trigger Conditions

Run this skill when:
* A change is ready to commit or push, or before opening or approving a pull request.
* CI fails and you need to reproduce the failure locally.
* At the end of each stage, before skill evaluations are logged (SK-7).

Do not use it for wallet or transfer behaviour; that belongs to `simulate-transfer` (SK-3).

---

## 2. Prerequisites

* JDK 21 or later on `PATH` (`java -version`). Maven is not required; the Maven Wrapper (`mvnw`, `mvnw.cmd`) downloads the pinned version.
* Python 3.9 or later (the script uses only the standard library).
* No credentials are needed. The health endpoint is unauthenticated (NFR-API-5), and the skill never reads, prints or stores keys (SK-8).

---

## 3. Procedure

From the repository root:

```bash
python3 scripts/local_check.py
```

The script runs these steps in order. A step whose input is missing is marked `SKIP` (no tests without a build, no probe without a running server); otherwise every step runs, so one report shows all failures:

| # | Step | What it does | Passes when |
|---|------|--------------|-------------|
| 1 | build | `mvnw -B -q package -DskipTests -Djacoco.skip=true` | exit code 0 and `target/financial-ledger-api.jar` exists |
| 2 | tests | `mvnw -B -q verify`: unit, integration and system tests, formatting check, `-Werror` compile, 85% coverage gate on domain and application | exit code 0 |
| 3 | start server | runs the jar on a free loopback port with `LEDGER_DB_PATH` in a fresh temporary directory | `/api/v1/health` answers before the startup timeout |
| 4 | health probe | `GET /api/v1/health` | HTTP 200, `status=OK`, `database=reachable`, and an `X-Request-Id` header |
| 5 | stop server | sends a termination signal and waits | the process exits by itself (`graceful`); having to kill it is a failure |
| 6 | discard database | deletes the temporary directory | the directory is gone |

The last 4000 characters of the Maven or server log are printed under a failing build, test or start step.

Options:
* `--skip-build` reuses the existing jar and skips steps 1 and 2 (useful while iterating on startup problems).
* `--startup-timeout N` waits up to `N` seconds for step 3 (default 60).

The exit code is `0` only when every step that ran passed, and the last line reads `OVERALL: PASS` or `OVERALL: FAIL`.

---

## 4. Interpreting Failures

| Failing step | Usual cause | Next action |
|--------------|-------------|-------------|
| build | compile error or `-Xlint` warning (warnings fail the build) | read the first `[ERROR]` line |
| tests | failing test, formatting drift, coverage below 85% | `./mvnw spotless:apply` for formatting; open `target/site/jacoco/index.html` for coverage |
| start server | port or path problem, configuration error (exit code 2), incompatible schema | the captured server output is printed under the step |
| health probe | database unreachable (503 `DEGRADED`) | check the temporary path is writable |
| stop server | `had to be killed` means the shutdown hook hung | inspect `LedgerApplication.close` |

Never "fix" a failure by deleting or weakening a test; see PC-10.

---

## 5. Recording the Run

After a stage-end run, add a row to `docs/skill_evaluations.md` with the date, commit, per-step results and anything that had to be repeated. Append a short entry to `brain/log.md`.

---

## 6. Brain Citations

* [[resources/sqlite-wal-locking-mechanics|SQLite WAL and Transaction Locking Mechanics]]: why the check uses a file-backed database rather than in-memory SQLite.
* [[resources/agentskills-specification-standards|Agent Skills Specification and Evaluation Standards]]: skill format and evaluation fields.
* [[areas/concurrency-acid|Area: Concurrency, Locking and ACID Guarantees]]
