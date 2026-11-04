# tokendashboard (monorepo)

Two independent subprojects, each with its own build, tests and CLAUDE.md:

- `backend/` — Ktor/Kotlin. Read [backend/CLAUDE.md](backend/CLAUDE.md) before working there.
- `frontend/` — Astro + React. Read [frontend/CLAUDE.md](frontend/CLAUDE.md) before working there.

Domain language for both sides: [GLOSSARY.md](GLOSSARY.md). Repo-wide decisions (why a
monorepo, CI layout): [docs/decisions.md](docs/decisions.md).

Run every build, test and lint command from inside the subproject folder (`./gradlew`
in `backend/`, `npm` in `frontend/`); there is no build at the root.

The root holds only what is shared: `.devcontainer/` (one container for both),
`justfile`, and `.github/workflows/` with one workflow per subproject. Each workflow
runs only when its folder changed and sets `working-directory` to that folder.
