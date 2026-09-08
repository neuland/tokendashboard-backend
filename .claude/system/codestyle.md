# General Coding Agent Guide

General rules that apply to most neuland projects

## Code Formatting

Formatting is handled by **ktlint**. Run `./gradlew ktlintFormat` to format manually. The Claude `Stop` hook runs
`ktlintFormat` automatically at the end of each turn — no manual style rules need to be memorised.

## Domain Model

- Never use types from other systems or database models as is in domain. An anti-corruption-layer (ACL) shields the
  domain from outside world.
- No raw types are passed between classes inside the domain. Always wrap primitives in a domain value object —
  see [Value Objects](#value-objects).
- `LocalDate.now()` is only called in `TimeFactory` (`domain/factories/TimeFactory.kt`), registered as a Koin
  singleton (`single { TimeFactory() }`) so it can be mocked in tests. Never call `LocalDate.now()`/`Instant.now()`
  directly elsewhere.
- DI is handled by **Koin** (`src/main/kotlin/de/neuland/tokendashboard/di/AppModules.kt`). Architecture boundaries are
  enforced by `ArchitectureTest.kt` using Konsist.
- All domain models with validity constraints can only be created valid.

### `NonEmptyList` construction — the sanctioned `!!` exception

`arrow.core.NonEmptyList` encodes "non-empty" as a compile-time guarantee and is kept deliberately — it is more
valuable as a type than a nullable list would be. Constructing one from a runtime collection returns a nullable
(`toNonEmptyListOrNull()`), so a non-null assertion is unavoidable at the point of construction.

`!!` on `toNonEmptyListOrNull()` is **explicitly allowed** in two situations:

- **`NonEmptyList` extension helpers**, where the source is already a `NonEmptyList` so the result cannot be empty
  (exemplar: `UsageResult.groupByNonEmpty` in `domain/model/UsageResult.kt`).
- **Outbound repository adapters**, where the `!!` is immediately preceded by an `isEmpty()` guard that routes the
  empty case elsewhere, e.g.:

  ```kotlin
  if (list.isEmpty()) UsageResult.NoUsage else UsageResult.HasUsage(list.toNonEmptyListOrNull()!!)
  ```

In both cases the null branch is provably unreachable — add a short comment stating why. Outside these two
contexts, prefer the null-safe styles described above.

### Avoid negation in if-conditions

Prefer positive conditions. If only a negated check exists, introduce a named method with positive semantics instead of
using `!`.

```kotlin
class Anything {
    fun wrong() {
        if (!conditionFailed()) { /* do something */
        }
    }

    fun right() {
        if (conditionSucceeded()) { /* do something */
        }
    }

    fun conditionSucceeded() = !conditionFailed()
    fun conditionFailed() = true
}
```

### Prefer named imports over star imports

Import symbols by name. Avoid wildcard imports — they obscure where a symbol comes from.

### Error handling in the domain

- **Exceptions only at system boundaries** — value object constructors that receive external input may throw.
- **`Either` for expected domain failures** — a domain operation that can fail for a business reason returns
  `Either<Failure, Success>` from Arrow instead of throwing. Not currently used in this codebase (no domain
  operation needs it yet) — reach for it when one does.
- **Nullable return type** — when an operation simply produces no result (not a failure), return a nullable type
  instead of `Either`.

```kotlin
// Domain-internal: no result is not a failure — use nullable
fun subtract(other: TicketQuantity): TicketQuantity? {
    val result = value - other.value
    return if (result < 0) null else TicketQuantity(result)
}
```

### Private constructors and factory methods

A class that exposes a factory method must have a **private constructor**. A public constructor alongside a factory is a
contradiction — either the factory is redundant or the constructor should not be directly accessible.

A private constructor that is only called from the class's own factory does not re-validate its arguments. The factory
is the system boundary where raw input is validated and valid domain objects are constructed. Everything the private
constructor receives has already passed through that boundary and can be trusted.

```kotlin
class DateRange private constructor(
    val first: RentalDay,
    val count: DayCount
) {
    companion object {
        fun of(first: RentalDay, count: DayCount): DateRange = DateRange(first, count)
    }
}
```

## Value Objects

Wrapper values (a single wrapped type like `CentPerMillionToken`, `NanoCent`, `ModelName`) are standalone *
*`@JvmInline value class`** value objects — there is no `Value<T>` base hierarchy in this project. Exemplar:
`src/main/kotlin/de/neuland/tokendashboard/domain/model/CentPerMillionToken.kt`.

Serialization is **kotlinx.serialization**; persistence is via **JDBI** row mappers — not Jackson/JPA
`AttributeConverter`.

**Compound values** — multiple fields that together form one concept, e.g. `DayRange(Day, Day)` — do **not** fit a
single value class. Use a `data class` instead.

## Testing

- Tests use **Kotest `FunSpec`** with **MockK** (`mockk<…>()`, `every { … } returns …`, `verify { … }`) — not
  JUnit/Mockito. Exemplar: `src/test/kotlin/de/neuland/tokendashboard/application/CopilotUsageIngestServiceTest.kt`.
- Structure test bodies with blank lines. Where a single test asserts several distinct business cases, 
- introduce each with a short comment naming the case ( `// when requesting only until june 13`) — see
  `src/test/kotlin/de/neuland/tokendashboard/integrationtests/AllProviderUsageIntegrationTest.kt`. A test body that
  needs more than a handful of sections is too long: split it into separate `test(…)` blocks instead of annotating it.
- Test names: Kotest `test("someDescription") { … }` blocks — no `@DisplayName`, no
  `methodShouldDoSomethingWhenCondition()` naming.
- Test all branches/guard paths as **separate test blocks** in the existing
  `*Test` — do not create a new test class per feature.
- Use the **exact type and class names** from the codebase — verify by reading
  the source, never guess.
- When extracting logic into a new utility/helper class, always create a
  **dedicated unit test class** for it; tests in the calling service are not sufficient.
- `TimeFactory` is mocked for testing. Never use real system time.
- **Assertion library** — use **Kotest assertions** (`io.kotest:kotest-assertions-core`). For `Either` results use *
  *`io.kotest.extensions:kotest-assertions-arrow`** — never use `isInstanceOf(Either.Right::class.java)` or similar
  `instanceof` checks. Standard imports:
    - `io.kotest.matchers.shouldBe`
    - `io.kotest.matchers.nulls.shouldBeNull` / `shouldNotBeNull`
    - `io.kotest.assertions.throwables.shouldThrow`
    - `io.kotest.assertions.arrow.core.shouldBeRight` / `shouldBeLeft`

- **Custom assertions for domain types** — when a domain type's tests get repetitive, add extension functions on
  the type itself, named with the `assert` prefix so callers can distinguish them from regular methods:

  ```kotlin
  // in src/test/.../assertions/ClaudeUsageRecordAssertions.kt
  fun ClaudeUsageRecord.assertHasCost(expected: DollarCent) {
      costUsdCent shouldBe expected
  }
  ```

  Naming convention: `assertHasXxx` for property checks, `assertIsXxx` for state checks. Keep one file per domain type,
  named `<Type>Assertions.kt`.

## General Kotlin Style

Prefer Kotlin's **collection operations** (`.filter`, `.map`, `.fold`, `.groupBy`, …) over imperative loops. Use a loop
only when a functional equivalent would be materially less readable.

Prefer `val` over `var` everywhere. Use `var` only when mutation is genuinely required.

## Design Principles

- **Pragmatism > Dogma**: Prefer the simplest solution that solves the actual problem.
  Architectural patterns (extraction into policy classes, new abstractions) are only
  justified when inline improvements are insufficient. A 3-line inline fix beats a
  3-file architectural solution when the outcome is equally readable.
- **Clean Code**: Extract helper methods when a method exceeds ~20 lines or handles
  multiple concerns.
- **Testability**: Pure functions and small units over monolithic blocks.
