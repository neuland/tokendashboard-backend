package de.neuland.tokendashboard

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.architecture.KoArchitectureCreator.assertArchitecture
import com.lemonappdev.konsist.api.architecture.Layer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty

class ArchitectureTest :
    FunSpec({
        val productionCode = Konsist.scopeFromProduction()

        test("domain does not depend on application or adapter, application does not depend on adapter") {
            val domain = Layer("Domain", "de.neuland.tokendashboard.domain..")
            val application = Layer("Application", "de.neuland.tokendashboard.application..")
            val adapter = Layer("Adapter", "de.neuland.tokendashboard.adapter..")

            productionCode.assertArchitecture {
                domain.dependsOnNothing()
                application.dependsOn(domain)
                adapter.dependsOn(domain, application)
            }
        }

        test("driving and driven ports are interfaces and exist") {
            val incomingPorts = "de.neuland.tokendashboard.application.port.incoming.."
            val outgoingPorts = "de.neuland.tokendashboard.application.port.outgoing.."

            val portInterfaces =
                productionCode
                    .interfaces()
                    .filter { it.resideInPackage(incomingPorts) || it.resideInPackage(outgoingPorts) }

            val nonInterfacePorts =
                (
                    productionCode.classes().filter { it.resideInPackage(incomingPorts) || it.resideInPackage(outgoingPorts) } +
                        productionCode.objects().filter { it.resideInPackage(incomingPorts) || it.resideInPackage(outgoingPorts) }
                ).map { it.name }

            nonInterfacePorts.shouldBeEmpty()
            portInterfaces.shouldNotBeEmpty()
        }

        test("domain does not import framework code") {
            val forbiddenPackages =
                listOf(
                    "io.ktor",
                    "org.jdbi",
                    "kotlinx.serialization",
                    "org.koin",
                    "com.zaxxer.hikari",
                    "org.flywaydb",
                    "org.postgresql",
                    "javax.inject",
                    "jakarta.inject",
                )

            val violations =
                productionCode
                    .files
                    .filter { it.path.contains("/de/neuland/tokendashboard/domain/") }
                    .flatMap { file ->
                        file.imports
                            .filter { import -> forbiddenPackages.any { import.name.startsWith(it) } }
                            .map { "${file.path} imports ${it.name}" }
                    }

            violations.shouldBeEmpty()
        }

        test("ports do not pass raw primitives") {
            val forbiddenParameterTypes =
                listOf(
                    "kotlin.String",
                    "kotlin.Int",
                    "kotlin.Long",
                    "kotlin.Double",
                    "kotlin.Float",
                    "kotlin.Boolean",
                    "java.time.LocalDate",
                    "java.time.Instant",
                    "java.math.BigDecimal",
                )
            val forbiddenReturnTypeNames =
                listOf("String", "Int", "Long", "Double", "Float", "Boolean", "LocalDate", "Instant", "BigDecimal")

            val portFunctions =
                productionCode
                    .functions()
                    .filter { it.resideInPackage("de.neuland.tokendashboard.application.port..") }

            val parameterViolations =
                portFunctions.flatMap { function ->
                    function
                        .parameters
                        .filter { parameter -> forbiddenParameterTypes.any { parameter.representsType(it) } }
                        .map { "${function.name} parameter ${it.name}" }
                }

            val returnTypeViolations =
                portFunctions
                    .filter { function -> forbiddenReturnTypeNames.any { it == function.returnType?.name } }
                    .map { "${it.name} return type" }

            (parameterViolations + returnTypeViolations).shouldBeEmpty()
        }

        test("every production file resides in a declared layer") {
            val declaredLayerPaths =
                listOf(
                    "/de/neuland/tokendashboard/domain/",
                    "/de/neuland/tokendashboard/application/",
                    "/de/neuland/tokendashboard/adapter/",
                    "/de/neuland/tokendashboard/di/",
                )

            val violations =
                productionCode
                    .files
                    .filterNot { file -> declaredLayerPaths.any { file.path.contains(it) } }
                    .filterNot { it.path.endsWith("/Application.kt") }
                    .map { it.path }

            violations.shouldBeEmpty()
        }

        test("incoming adapter does not import the database driver") {
            val violations =
                productionCode
                    .files
                    .filter { it.path.contains("/de/neuland/tokendashboard/adapter/incoming/") }
                    .flatMap { file ->
                        file.imports
                            .filter { it.name.startsWith("org.jdbi") || it.name.startsWith("com.zaxxer.hikari") }
                            .map { "${file.path} imports ${it.name}" }
                    }

            violations.shouldBeEmpty()
        }

        test("no restore() call appears in the incoming adapter") {
            // By convention in this codebase, a factory named restore() rehydrates an already-stored,
            // already-validated value and performs no validation itself (see ModelName.restore()).
            // That is safe only as long as no restore() is ever called on external input.
            val restoreCallPattern = Regex("""\brestore\(""")
            val violations =
                productionCode
                    .files
                    .filter { it.path.contains("/de/neuland/tokendashboard/adapter/incoming/") }
                    .filter { restoreCallPattern.containsMatchIn(it.text) }
                    .map { it.path }

            violations.shouldBeEmpty()
        }
    })
