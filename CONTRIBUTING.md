# Contributing to TokenDashboard

Thanks for your interest in contributing! This project is maintained by
[neuland - Büro für Informatik](https://neuland-bfi.de) and developed as
open source.

## Before you start

For anything beyond a small fix (new features, architectural changes,
breaking API changes), please open an issue first to discuss the approach.
This avoids wasted effort if the direction doesn't fit the project.

See [CLAUDE.md](CLAUDE.md) and the linked docs for architecture, domain
model (DDD), and code style conventions, and
[docs/decisions.md](docs/decisions.md) for the reasoning behind existing
business/domain decisions. Please read the relevant decisions before
proposing a change that touches them.

## Development setup

```bash
docker-compose up -d   # start local Postgres
./gradlew run          # run the application (http://localhost:8080)
```

See [README.md](README.md#local-development) for details on database
credentials and configuration.

## Code style

- Formatting is handled by **ktlint** — run `./gradlew ktlintFormat` before
  committing.
- Follow the conventions in
  [.claude/system/codestyle.md](.claude/system/codestyle.md): domain
  invariants via value objects, `Either`/nullable for expected failures,
  private constructors with factory methods, no wildcard imports.
- Tests use **Kotest** (`FunSpec`) with **MockK**, structured with
  `// given` / `// when` / `// then` comments — see
  [.claude/system/codestyle.md](.claude/system/codestyle.md#testing) for the
  full conventions.

## Before opening a pull request

```bash
./gradlew ktlintCheck
./gradlew build   # runs tests + the architecture test (Konsist)
```

A green `./gradlew build` means the hexagonal layering (see
[.claude/system/architecture.md](.claude/system/architecture.md)) is intact
and all tests pass. Please keep changes focused — unrelated refactors or
formatting-only changes make review harder and should go in a separate PR.

## Pull request guidelines

- Describe the "why", not just the "what" — especially for anything
  touching domain behaviour or the API contract used by the
  Claude/Copilot/OpenCode plugins.
- Add or update tests for behavioural changes.
- If your change affects a documented decision in
  [docs/decisions.md](docs/decisions.md), update it as part of the PR.

## Reporting bugs / security issues

Open an issue with steps to reproduce. For security-relevant findings,
please avoid filing a public issue — contact the maintainers directly at
tokendashboard@neuland-bfi.de instead.
