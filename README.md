# TokenDashboard

Part of a system that measures a company's token usage. 
It requires the backend, the frontend and at least one plugin that sends the data.  
This is the backend repository.  
The frontend can be found here:  
https://github.com/neuland/tokendashboard-frontend  
and the plugins here:  
Claude: https://github.com/neuland/tokendashboard-plugin-claude  
Copilot: https://github.com/neuland/tokendashboard-plugin-copilot  
OpenCode: https://github.com/neuland/tokendashboard-plugin-opencode  

## Documentation
See [CLAUDE.md](CLAUDE.md) for architecture, DDD, and code style docs, 
and [docs/decisions.md](docs/decisions.md) for business/domain decisions.

## No authentication, no user/team attribution

Neither the dashboard nor the backend API has any form of authentication.
**Only** deploy this in trusted networks (e.g. behind a VPN).

Token usage is not stored alongside the user ID — the user ID exists solely
to track the number of active plugins. It is not possible to trace token
usage back to individuals or teams. Prompt content is never collected or
sent to the backend, only token counts.

Deliberate data-minimization choice. Any feature requiring per-user or
per-team granularity (e.g. "aggregate by team") needs a separate, dedicated
decision to introduce auth infrastructure.

Anyone who can reach the service can submit usage data and read all
aggregates, so the network boundary is the entire access control. See
[SECURITY.md](SECURITY.md) for the full security model.

## CO₂ factors

CO₂ figures shown in the dashboard are rough order-of-magnitude estimates,
not measured emission factors.
See [docs/CO2_METHODOLOGY.md](docs/CO2_METHODOLOGY.md) for the full derivation
and its assumptions.

## Token coverage

Token totals are lower bounds: each plugin has known gaps in what it
captures. See [docs/TOKEN_COVERAGE.md](docs/TOKEN_COVERAGE.md) for the
known mechanisms per plugin.

## Claude prices endpoint for plugins

`GET /api/prices/claude` returns the currently valid price per Claude model
family (input/output/cache-write/cache-read, cent per million tokens). The
Claude plugin fetches this on update to show users their own costs. See
[docs/decisions.md](docs/decisions.md#5-claude-prices-single-source-via-endpoint-not-a-duplicated-file).

## Series return number of installations, not users at bucket day

**`pluginInstallations` semantics**: Number of installations that are “active” on the bucket day, defined as:

```
first_data_sent <= day AND day < last_data_sent + 4 weeks
```

- Does not count “how many sent data on that day”, but rather cumulative installations within the 4-week window from the last data transmission.
- No drop-offs or re-entries are tracked: if I install on Monday and re-install on Wednesday (new `id`), I am counted on both days — potentially twice.
  This is **intentional**; traceability is not intended.

## Local development

Start the local Postgres:

```
docker-compose up -d
```

The app connects with the default credentials baked into
`src/main/resources/application.conf` (`postgres`/`postgres`), which match
`docker-compose.yml`. It refuses to start with that default password unless
you explicitly opt in, guarding against shipping the default password to a
real environment.

`./gradlew run` sets the opt-in automatically (see `build.gradle.kts`), so
`docker-compose up -d && ./gradlew run` just works. The deployed artifact
runs the Dockerfile's fat jar directly and never goes through `gradlew run`,
so the guard stays in effect in production.

If you run the jar directly, via IntelliJ, or want your own local
credentials, set `DATABASE_USER`/`DATABASE_PASSWORD` (or
`ALLOW_DEFAULT_DB_CREDENTIALS=true`) as environment variables — never in
`application.conf`, since that file ships with the artifact.

### Devcontainer (optional)

`.devcontainer/` provides a ready-to-use container with Claude Code
preinstalled. It mounts your host `~/.claude`, so your existing settings,
plugins and hooks work inside the container unchanged. Everything above
works without it — the devcontainer is a convenience, not a requirement.
Note that Claude inside the devcontainer is started with the parameter
`--dangerously-skip-permissions`.

## Deployment

`examples/` contains generic deployment config (CI, Kubernetes manifests,
HTTP client env) with placeholder values — use it as the starting point for
your own deployment.

## Tests

```
./gradlew ktlintCheck
./gradlew test
```

Integration tests spin up Postgres via Testcontainers, so a running Docker
daemon is required.

MockK/ByteBuddy on our JDK 25 toolchain need the following jvmArgs, applied in
[build.gradle.kts](build.gradle.kts). This means tests only run correctly via Gradle. In
IntelliJ, running a whole spec uses a "Gradle" run config and picks this up - but running a
single test is launched by the Kotest plugin via its own "Kotest" run config, which starts
the JVM directly and bypasses this block entirely. There's no supported way to inject these
flags into that Kotest-type config project-wide (see kotest-intellij-plugin#124); each dev
must add them manually once in their Kotest run configuration template if they want
single-test runs to work in the IDE.
```
jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "-Dnet.bytebuddy.experimental=true",
    "-Dapi.version=1.47",
)
```
