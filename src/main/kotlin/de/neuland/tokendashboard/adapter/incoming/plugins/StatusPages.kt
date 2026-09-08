package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.adapter.incoming.rest.MissingContentLengthException
import de.neuland.tokendashboard.adapter.incoming.rest.RequestBodyTooLargeException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import java.time.format.DateTimeParseException

private val logger = LoggerFactory.getLogger("de.neuland.tokendashboard.adapter.incoming.plugins.StatusPages")

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            logger.warn("Bad request on {}: {}", call.request.local.uri, cause.message)
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<RequestBodyTooLargeException> { call, _ ->
            call.respond(HttpStatusCode.PayloadTooLarge)
        }
        exception<MissingContentLengthException> { call, _ ->
            call.respond(HttpStatusCode.LengthRequired)
        }
        exception<DateTimeParseException> { call, cause ->
            logger.warn("Bad request on {}: {}", call.request.local.uri, cause.message)
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Invalid payload on {}: {}", call.request.local.uri, cause.message)
            call.respond(HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, cause ->
            logger.error("Unexpected error on {}", call.request.local.uri, cause)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
