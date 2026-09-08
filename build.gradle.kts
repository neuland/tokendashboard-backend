plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("io.ktor.plugin") version "3.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    application
}

group = "de.neuland"
version = "0.7.0"

application {
    mainClass.set("de.neuland.tokendashboard.ApplicationKt")
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.0.3"
val arrowVersion = "2.0.0"
val jdbiVersion = "3.46.0"
val kotestVersion = "5.9.1"
val koinVersion = "4.0.2"
val flywayVersion = "12.10.0"

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Arrow
    implementation("io.arrow-kt:arrow-core:$arrowVersion")

    // JDBI (JPA-Repository equivalent — SqlObject + Kotlin)
    implementation("org.jdbi:jdbi3-core:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin:$jdbiVersion")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:$jdbiVersion")
    implementation("org.jdbi:jdbi3-postgres:$jdbiVersion")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.7")

    // Connection Pooling
    implementation("com.zaxxer:HikariCP:6.2.1")

    // Flyway
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Serialization & Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Koin
    implementation("io.insert-koin:koin-ktor:$koinVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.38")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.apache.commons:commons-compress:1.28.0")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-framework-datatest:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.4.0")
    testImplementation("io.mockk:mockk:1.14.0")
    testImplementation("com.lemonappdev:konsist:0.17.3")
}

kotlin {
    jvmToolchain(25)
}

tasks.named<JavaExec>("run") {
    // Local dev convenience only: allows startup against the docker-compose
    // default DB credentials. Never used for the deployed artifact — the
    // Dockerfile runs the fat jar directly, not `gradlew run`.
    environment("ALLOW_DEFAULT_DB_CREDENTIALS", System.getenv("ALLOW_DEFAULT_DB_CREDENTIALS") ?: "true")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "-Dnet.bytebuddy.experimental=true",
        "-Dapi.version=1.47",
    )
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
}

// The ktor plugin sets duplicatesStrategy = EXCLUDE on the shadowJar task it
// configures. In shadow-gradle-plugin 9.x that makes duplicate resource
// transformers see only one candidate resource, so mergeServiceFiles() above
// silently stops merging Flyway's META-INF/services/*.Plugin file across
// flyway-core and flyway-database-postgresql. Reset it after ktor's
// afterEvaluate has run so merging actually applies.
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
