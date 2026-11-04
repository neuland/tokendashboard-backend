# Decisions

Decisions that concern the whole repository rather than one subproject. Backend
decisions live in [backend/docs/decisions.md](../backend/docs/decisions.md).

## 1. Backend and frontend in one repository

The backend (`tokendashboard-backend`) and the frontend (`tokendashboard-frontend`) were
two GitHub repositories. They now live here as `backend/` and `frontend/`, merged with
both histories. This repository is the former backend repository under a new name, so
GitHub redirects the old URL here; the frontend repository is archived with a pointer
to this one.

Why: frontend and backend usually have to change together, which one repository turns
into one pull request instead of two coordinated ones.

- **CI:** GitHub Actions only reads `.github/workflows/` at the root. `backend.yml` and
  `frontend.yml` each run only when their folder (or the workflow itself) changed, and
  run their steps inside that folder via `working-directory`. A change outside both
  folders starts no workflow.
- **History:** commits carry no `[backend]`/`[frontend]` prefix. `git log -- backend/`
  filters instead, and `git log --follow` follows a file across the move.
- **Old branches:** a branch from before the move merges with
  `git merge -X subtree=backend/ <branch>` or `-X subtree=frontend/`. A plain merge would
  put a backend branch's changes to `README.md`, `.gitignore` and the like into the root
  files, which exist under the same paths. What such a branch changes in `.devcontainer/`
  or `justfile` lives at the root now and is carried over by hand.
- **Devcontainer:** one for both sides, without access to the host's Docker daemon.
  Backend tests use a postgres sidecar instead of Testcontainers, as in CI; image builds
  run on the host.
