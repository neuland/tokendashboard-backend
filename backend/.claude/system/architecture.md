# Architecture: Hexagonal (Ports & Adapters)

Stack: **Ktor** (HTTP), **Koin** (DI), **JDBI** (persistence). Architecture boundaries enforced by
`ArchitectureTest.kt` (`src/test/kotlin/de/neuland/tokendashboard/ArchitectureTest.kt`), using
[Konsist](https://docs.konsist.lemonappdev.com/): checks layer dependencies (`domain` depends on
nothing, `application` depends only on `domain`, `adapter` depends on `domain`/`application`) and that all
driving/driven ports are interfaces.

| Package                       | Role                                                                       |
|--------------------------------|-----------------------------------------------------------------------------|
| `adapter/incoming/rest`        | Inbound HTTP DTOs and request-parsing helpers (e.g. `ClaudeUsageReport`)     |
| `adapter/incoming/plugins`     | Ktor routing (`Routing.kt`) and response-serialization extensions           |
| `adapter/outgoing/repository`  | JDBI repository interfaces plus the adapter classes implementing driven ports |
| `adapter/outgoing/plugins`     | Infrastructure plugins for outgoing concerns (e.g. `Database.kt`)           |
| `application/`                 | Application services (orchestrate domain + ports) and Command classes       |
| `application/port/incoming/`   | Driving ports — use-case interfaces invoked by inbound adapters             |
| `application/port/outgoing/`   | Driven ports — interfaces implemented by outbound adapters                  |
| `domain/`                      | Domain code that isn't a model/value object                                 |
| `domain/factories/`            | Wraps non-deterministic operations, e.g. `TimeFactory` wraps `LocalDate.now()` |
| `domain/model/`                | Domain model, value objects                                                  |
| `di/`                          | Koin modules (`AppModules.kt`)                                              |

**Constraints**:
- `domain/` never imports from `application` or `adapter`.
- `application/` never imports from `adapter`.
- `domain/factories/` classes are plain classes registered as Koin singletons (e.g. `single { TimeFactory() }` in
  `Application.kt`) — no DI annotations.

## Ports

- `application/port/incoming/` — Driving Ports: use-case interfaces implemented by application services and called
  by inbound adapters (Ktor routing).
- `application/port/outgoing/` — Driven Ports: interfaces implemented by outbound adapters (JDBI repositories).
  Application services depend only on these, never on concrete adapters.

### Commands

Command classes carry the input for a driving-port use case. Since everything under `application/port/` must be an
interface, commands live directly under `application/`, named `<UseCase>Command` (e.g.
`IngestClaudeUsageRecordCommand`).

## Read path: usage with cost and CO₂

Cost and CO₂ are not stored; they are calculated on every read (see `docs/decisions.md`, decision 9):

1. **Repository port** returns raw daily usage per model (`claudeDailyUsage(range)` etc.) — tokens plus the
   provider's raw cost value, if any.
2. **`<Provider>UsageSource`** (`application/`, implements `ProviderUsageSource`) loads the price/CO₂ factor
   history and hands both to the matching **`<Provider>UsageValuation`** (`domain/model/`), which turns each day and
   model into a `ModelUsage` with cost (whole cents) and CO₂. `ProviderUsageSources` picks the source per `Provider`.
3. **Query services** aggregate the valued days with the functions in `domain/UsageAggregation.kt` (`rollUp`,
   `sumByModel`, `buildUsageBuckets`).

## Design Principles

- **Separation of concerns**: routing handles HTTP, application services orchestrate, domain holds business logic
  and validation.
- **Naming reflects domain intent**: names express business purpose, not technical mechanism.
- **No KDoc**: names should convey intent; comments only for genuinely non-obvious *why* (e.g. `IngestLimits.kt`).
