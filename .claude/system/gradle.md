# Gradle

Build tool for this project. Build script is written in **Kotlin DSL**
(`build.gradle.kts`, `settings.gradle.kts`) — not Groovy.

## Daily commands

```bash
./gradlew build                           # compile + run all tests + checks
./gradlew test                            # run all unit tests
./gradlew test --tests '*ValueTest'       # run a single test class (pattern)
./gradlew test --tests '*ValueTest.methodShouldDoSomethingWhenCondition'
                                          # run a single test method
./gradlew check                           # all verification tasks (test + architecture test + any added static analysis)
./gradlew run                             # run the application locally (needs `docker-compose up -d` first)
                                          # config via env vars / application.conf (Ktor, no Spring profiles)
./gradlew clean                           # delete build/ output
```

## Dependencies & troubleshooting

```bash
./gradlew dependencies                    # full dependency tree
./gradlew dependencyInsight --dependency <name>
                                          # why is version X of <name> on the classpath?
./gradlew --refresh-dependencies build    # re-resolve snapshots / cached artefacts
./gradlew tasks                           # list available tasks
./gradlew tasks --all                     # including tasks from plugins
./gradlew build --scan                    # upload a build scan for debugging
```

## Local infra

```bash
docker-compose up -d                      # start local Postgres (+ anything else in compose.yml)
docker-compose down                       # stop local infra
```

## Notes

- Always use the wrapper (`./gradlew`), never a system-installed `gradle`.
  The wrapper pins the Gradle version via `gradle/wrapper/gradle-wrapper.properties`.
- `./gradlew build` is the canonical pre-commit command. It runs tests **and**
  `ArchitectureTest.kt` (Konsist) — a green `build` means the hexagonal layering is intact.