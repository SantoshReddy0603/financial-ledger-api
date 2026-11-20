# Contributing

This repository follows the process requirements in [`docs/srs.pdf`](docs/srs.pdf) §8 (PC-1 to PC-10). Work items come from [`docs/plan.md`](docs/plan.md).

## Workflow

1. **Issue first.** Open a GitHub issue per task (`T-xx: <title>`), listing the requirement IDs it covers.
2. **Branch from `main`.** One branch per issue:
   * `feat/T-10-transfers`, `fix/42-replay-header`, `docs/T-32-user-guide`, `test/T-25-concurrency`, `chore/<slug>`.
3. **Commit with Conventional Commits.** `type(scope): summary` in the imperative, at most 72 characters.
   * Types: `feat`, `fix`, `test`, `docs`, `refactor`, `build`, `ci`, `chore`.
   * Reference the issue in the body: `Refs #12` or `Closes #12`.
4. **Open a pull request** into `main` with the template below. `main` is never pushed to directly.
5. **Review.** At least one other team member approves. The reviewer checks behaviour, tests, the traceability row and any AI-assisted change line by line (PC-9).
6. **Merge** with a squash or merge commit once CI is green.

## Pull request checklist

* [ ] Linked issue and requirement IDs
* [ ] Tests added or updated (positive and negative); a defect fix adds a regression test (PC-10)
* [ ] `./mvnw verify` passes locally
* [ ] `python3 scripts/validate_brain.py`, `validate_skills.py` and `validate_adrs.py` pass
* [ ] OpenAPI, diagrams and ADRs updated when the contract or design changes (DC-8)
* [ ] Row(s) in [`docs/traceability.md`](docs/traceability.md) updated with this issue, PR and test
* [ ] Brain note added or updated when a decision or finding is worth keeping (PC-5)

## Second brain changes

Changes to `brain/` go through the same branch and PR flow (SB-5). New material enters `brain/inbox/` through `skills/brain-ingest` and is triaged with `skills/brain-refine`. Each run gets a line in `brain/log.md` with the author.

## Releases

* Versions follow Semantic Versioning. The Maven version is bumped in the release PR.
* Each stage ends with an annotated tag: `stage-2`, `stage-3`, and `v1.0.0` for the final delivery.
* [`CHANGELOG.md`](CHANGELOG.md) is updated in the same PR, under `Unreleased` until tagged.

## Local commands

```bash
./mvnw verify                      # build, format check, tests, coverage gate, jar
./mvnw spotless:apply              # fix formatting
python3 scripts/local_check.py     # build, test, start server, probe /health, stop
python3 scripts/validate_brain.py
python3 scripts/validate_skills.py
python3 scripts/validate_adrs.py
```

## Secrets

Never commit credentials, admin keys, tokens or `.env` files. Admin keys are configured only as SHA-256 hashes (`LEDGER_ADMIN_KEYS`). CI runs a secret scan on every push.
