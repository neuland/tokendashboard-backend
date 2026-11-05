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

[`docker-compose.yml`](docker-compose.yml) builds both images from this checkout and
starts them with a database:

```bash
docker compose up --build
```

The dashboard then runs on <http://localhost:8088>; point the plugins at the same
address. `PUBLIC_COMPANY_NAME`, `DATABASE_PASSWORD` and `TOKENDASHBOARD_PORT` can be set
in the shell or in a `.env` next to the file. Needs Docker Compose 2.23 or newer.

### Claude Code in a devcontainer

A devcontainer that mounts `~/.claude` (as the devcontainers of these repositories do)
shares the Claude plugin's config and queue with the host, so both send to the same
`--api-base-url`. With `http://localhost:8088` a session inside the container cannot
reach the stack: there, `localhost` is the container itself. Its entries stay in the
shared queue and are sent the next time a Claude Code session on the host starts or
ends. Nothing is lost, but it shows up late, and not at all as long as Claude Code only
runs in the container.

To have the container send right away, install the plugin with
`--api-base-url http://host.docker.internal:8088` instead (`:8080` for a backend started
with `./gradlew run` on the host). Docker Desktop resolves that name inside containers
only, so it is then the other way round: sessions on the host stay in the queue until a
session in the container starts or ends. As the config is shared, only one side can send
directly.

This only concerns a local backend: under a real hostname, as in a deployment, host and
container reach the same address.

## Development

Each side is built from its own folder (`./gradlew …` in `backend/`, `npm …` in
`frontend/`). The [devcontainer](.devcontainer/) covers both (JDK 25, Node 24,
Playwright); `just claude` starts Claude Code inside it.

## CI

GitHub Actions runs [`.github/workflows/backend.yml`](.github/workflows/backend.yml)
and [`.github/workflows/frontend.yml`](.github/workflows/frontend.yml), each only when
its folder changed. Neither builds or publishes an image — how and where the images are
deployed is up to each installation (see the `examples/` folder on each side).
