# TokenDashboard

Company-wide dashboard for token usage, estimated cost and estimated CO₂ emissions
across AI providers (Claude Code, GitHub Copilot, OpenCode).

![The dashboard's overview page with key figures for tokens, CO₂ and cost and a history chart by provider (demo data)](docs/images/dashboard-overview.png)

| Folder | What | Docs |
| --- | --- | --- |
| [`backend/`](backend/) | Ktor/Kotlin API with PostgreSQL | [README](backend/README.md), [CONTRIBUTING](backend/CONTRIBUTING.md) |
| [`frontend/`](frontend/) | Astro + React static site | [README](frontend/README.md), [CONTRIBUTING](frontend/CONTRIBUTING.md) |

The data comes from at least one plugin, each in its own repository:

- Claude: https://github.com/neuland/tokendashboard-plugin-claude
- Copilot: https://github.com/neuland/tokendashboard-plugin-copilot
- OpenCode: https://github.com/neuland/tokendashboard-plugin-opencode

Backend and frontend were separate repositories until they were merged here with their
full history; `git log -- backend/` or `git log -- frontend/` shows the history of one
side.

## Try it out

### Using the prebuilt images

Each [release](https://github.com/neuland/tokendashboard/releases) publishes both images
(amd64 and arm64) and attaches a `docker-compose.yml` that starts them with a database:

```bash
curl -LO https://github.com/neuland/tokendashboard/releases/latest/download/docker-compose.yml
docker compose up
```

The dashboard then runs on <http://localhost:8088>. Needs Docker and Docker Compose 2.23
or newer. That file runs the images of its own release; `TOKENDASHBOARD_VERSION=1.0.0`
picks another one, `DATABASE_PASSWORD` and `TOKENDASHBOARD_PORT` change the database
password and the port. Set them in the shell or in a `.env` next to the file.

The published frontend image carries no company name (the sidebar shows "neuland"). It
is baked into the static build, so setting one means building the image yourself, see
below.

### Building from source

```bash
docker compose up --build
```

Here Compose also reads [`docker-compose.override.yml`](docker-compose.override.yml),
which builds both images from the source instead of pulling them, and
`PUBLIC_COMPANY_NAME` in the shell or in a `.env` sets the company name.
`docker compose -f docker-compose.yml up` runs the published images instead.

### Then install a plugin

The dashboard stays empty until at least one plugin sends data, so install one next.
Copy the install command from the dashboard itself, under "Install plugins" in the
navigation (<http://localhost:8088/en/faq/#plugins>) or the "Install plugin" button on a
provider page: it already carries the right `--api-base-url` for this instance.

### Claude Code in a devcontainer

Keep this in mind when trying it out with a local backend, whether started with
`docker compose up` or with `./gradlew run` in `backend/`: if Claude Code runs in a
devcontainer of your own that mounts `~/.claude`, it may not reach the backend, since
`localhost` inside the container is the container itself. Nothing is lost, but the
entries wait in the plugin's queue and are only sent the next time a Claude Code session
on the host starts or ends.

This repository's [devcontainer](.devcontainer/) solves this with port forwarding:
`localhost:8088` (the compose stack) and `localhost:8080` (`./gradlew run` on the host)
inside the container lead to the host, so the same `--api-base-url` works on both sides.
Port 8080 is therefore taken inside that container.

## Development

Each side is built from its own folder (`./gradlew …` in `backend/`, `npm …` in
`frontend/`). The [devcontainer](.devcontainer/) covers both (JDK 25, Node 24,
Playwright); `just claude` starts Claude Code inside it.

## CI

GitHub Actions runs [`.github/workflows/backend.yml`](.github/workflows/backend.yml)
and [`.github/workflows/frontend.yml`](.github/workflows/frontend.yml), each only when
its folder changed. A `vX.Y.Z` tag on `main` runs
[`.github/workflows/release.yml`](.github/workflows/release.yml): it first checks that
the tag is on `main`, matches the versions in `backend/build.gradle.kts` and
`frontend/package.json` and has a section in `CHANGELOG.md`, and only then publishes both
images to `ghcr.io/neuland/tokendashboard/backend` and `…/frontend` and creates the
GitHub Release with the changelog section and the pinned `docker-compose.yml`. How and
where the images are deployed is up to each installation (see the `examples/` folder on
each side).

## Releasing

Backend and frontend share one version and are released together.

1. Set the version in both build files: `version = "1.1.0"` in
   `backend/build.gradle.kts`, and in `frontend/`
   `npm version 1.1.0 --no-git-tag-version` (updates `package.json` and
   `package-lock.json`, without a commit or tag).
2. Add a `## [1.1.0] - YYYY-MM-DD` section to [`CHANGELOG.md`](CHANGELOG.md). It becomes
   the text of the GitHub Release.
3. Commit, merge into `main`, then tag that commit and push the tag:
   `git tag v1.1.0 && git push origin v1.1.0`.

Steps 1 and 2 can also be left to Claude Code: the `release` skill
([`.claude/skills/release/`](.claude/skills/release/SKILL.md)) sets the version, writes
the changelog section from the commits since the last release and runs the checks of
`release.yml`. It stops before commit and tag.

A version with a suffix (`1.1.0-rc.1`) becomes a pre-release: its images get only that
tag, not `1.1`, `1` or `latest`.

## License

MIT, see [LICENSE](LICENSE). The license does not cover the neuland name and logo, see
[TRADEMARKS.md](TRADEMARKS.md). Security issues: see [SECURITY.md](SECURITY.md).
