# TokenDashboard

Backend for visualising the company's token usage across providers (Claude
Code, GitHub Copilot, OpenCode). See [CLAUDE.md](CLAUDE.md) for architecture,
DDD, and code style docs, and [docs/decisions.md](docs/decisions.md) for
business/domain decisions.

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

## Derivation of CO₂ factors for Claude

The CO₂ factors used in this project are based on the study
*How Hungry is AI? Benchmarking Energy, Water, and Carbon Footprint of LLM Inference* by Jegham et al.
The study does not directly measure the electricity consumption of commercial large language models.
Instead, it estimates the energy consumption of standardised inference requests by combining measured response times with an infrastructure model.
This model incorporates publicly available information about data centres together with assumptions regarding the underlying hardware, system utilisation, and request batching.

For our internal order-of-magnitude estimate, we use a factor of **840 g CO₂e per million output-equivalent tokens**.

This factor is derived from the study's long-context benchmark for Claude 3.7 Sonnet, consisting of 10,000 input tokens and 1,500 output tokens.
For this scenario, Jegham et al. estimate an energy consumption of 5.671 Wh.
Using the emission factor of 0.287 kg CO₂e/kWh applied in the study, this corresponds to approximately 1.628 g CO₂e per request.

For our simplified token model, we weight one input token as one-twentieth of an output token.
Under this assumption, the reference scenario corresponds to 2,000 output-equivalent tokens, yielding approximately 814 g CO₂e per million output-equivalent tokens.
Taking into account the uncertainty reported by Jegham et al. for the estimated energy consumption, this corresponds to a range of approximately 771 to 857 g CO₂e per million output-equivalent tokens.
For internal reporting, we use the slightly conservative rounded value of 840 g CO₂e per million output-equivalent tokens.

The study neither reports nor models prompt-caching activities separately.
Since our agentic coding workloads frequently reuse large repository contexts, we additionally account for the cache-write and cache-read tokens reported by the CLI tools.
These token categories are converted into output-equivalent tokens using our own approximation factors.
We assign cache-write tokens a weight of 1.25 times the input-token factor and cache-read tokens a weight of 1% of the input-token factor.
These weights are based on the pricing ratios of the Claude API and should be understood as pragmatic approximations rather than physically derived energy factors.
For models other than Sonnet, we estimate relative CO₂ factors using the pricing ratios of the Claude API.
This includes models such as Claude Opus, Claude Haiku, and Claude Fable.
We deliberately use a consistent pricing-based scaling instead of adopting the published estimates for individual models directly.

The factors presented here are intended solely for internal order-of-magnitude estimation.
They are not measured emission factors and are not suitable for formal greenhouse gas accounting.
The resulting model combines the infrastructure assumptions described by Jegham et al. with our own assumptions regarding the relative weighting of different token categories.

### GPT / Copilot

No CO₂ factors for GPT models. Token counting infrastructure exists; CO₂
figures need to be derived and added separately.

### OpenCode

No CO₂ factors yet. Coverage is added per model as needed, not upfront —
too many models to maintain factors for all of them.

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
