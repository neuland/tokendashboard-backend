package de.neuland.tokendashboard.testsupport

import de.neuland.tokendashboard.adapter.incoming.plugins.appJson
import de.neuland.tokendashboard.adapter.incoming.plugins.configureRouting
import de.neuland.tokendashboard.adapter.incoming.plugins.configureStatusPages
import de.neuland.tokendashboard.di.repositoryModule
import de.neuland.tokendashboard.di.serviceModule
import de.neuland.tokendashboard.domain.factories.TimeFactory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationBuilder
import io.ktor.server.testing.testApplication
import org.jdbi.v3.core.Jdbi
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

// TestApplicationBuilder (not just its ApplicationTestBuilder subtype used by testApplication{}) so this
// also works inside a manually started TestApplication { } kept alive across multiple tests in a spec.
fun TestApplicationBuilder.configureIntegrationTestApp(
    jdbi: Jdbi,
    timeFactory: TimeFactory,
) {
    application {
        install(ContentNegotiation) { json(appJson) }
        install(Koin) {
            modules(
                module {
                    single { jdbi }
                    single<TimeFactory> { timeFactory }
                },
                repositoryModule,
                serviceModule,
            )
        }
        configureRouting()
    }
}

// For routing tests that mock individual ports instead of the real repository/service layers.
fun testRoutingApp(
    vararg modules: Module,
    block: suspend ApplicationTestBuilder.() -> Unit,
) = testApplication {
    application {
        install(ContentNegotiation) { json(appJson) }
        install(Koin) { modules(*modules) }
        configureStatusPages()
        configureRouting()
    }
    block()
}
