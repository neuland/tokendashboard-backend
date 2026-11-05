package de.neuland.tokendashboard

import de.neuland.tokendashboard.adapter.incoming.plugins.appJson
import de.neuland.tokendashboard.adapter.incoming.plugins.configureRouting
import de.neuland.tokendashboard.adapter.incoming.plugins.configureStatusPages
import de.neuland.tokendashboard.adapter.outgoing.plugins.configureDatabase
import de.neuland.tokendashboard.di.repositoryModule
import de.neuland.tokendashboard.di.serviceModule
import de.neuland.tokendashboard.domain.factories.TimeFactory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) =
    io.ktor.server.netty.EngineMain
        .main(args)

fun Application.module() {
    val jdbi = configureDatabase(environment)
    install(Koin) {
        modules(
            module {
                single { jdbi }
                single { TimeFactory() }
            },
            repositoryModule,
            serviceModule,
        )
    }
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(appJson)
    }
    install(CallLogging)
    configureStatusPages()
    configureRouting()
}
