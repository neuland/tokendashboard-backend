package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.adapter.incoming.rest.ClaudeUsageReport
import de.neuland.tokendashboard.adapter.incoming.rest.CopilotUsageReport
import de.neuland.tokendashboard.adapter.incoming.rest.OpenCodeUsageReport
import de.neuland.tokendashboard.adapter.incoming.rest.requireBodyWithinLimit
import de.neuland.tokendashboard.adapter.outgoing.plugins.DatabaseHealthCheck
import de.neuland.tokendashboard.application.port.incoming.IngestClaudeUsagePort
import de.neuland.tokendashboard.application.port.incoming.IngestCopilotUsagePort
import de.neuland.tokendashboard.application.port.incoming.IngestOpenCodeUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsageSeriesPort
import de.neuland.tokendashboard.application.port.incoming.QueryCurrentPricesPort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DayRange
import de.neuland.tokendashboard.domain.model.Granularity
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import java.time.LocalDate

const val API_VERSION_1 = "v1"

private fun granularityFrom(value: String): Granularity =
    Granularity.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        ?: throw BadRequestException("unsupported value for query parameter 'granularity'")

fun Application.configureRouting() {
    val claudeIngestService: IngestClaudeUsagePort by inject()
    val copilotIngestService: IngestCopilotUsagePort by inject()
    val openCodeIngestService: IngestOpenCodeUsagePort by inject()
    val providerUsageQueryService: QueryProviderUsagePort by inject()
    val providerUsageSeriesQueryService: QueryProviderUsageSeriesPort by inject()
    val allUsageQueryService: QueryAllUsagePort by inject()
    val allUsageSeriesQueryService: QueryAllUsageSeriesPort by inject()
    val currentPricesQueryService: QueryCurrentPricesPort by inject()
    val databaseHealthCheck: DatabaseHealthCheck by inject()

    routing {
        get("/health") {
            if (databaseHealthCheck.isReachable()) {
                call.respond(mapOf("status" to "ok"))
            } else {
                call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "error"))
            }
        }

        get("/api/usage/claude/$API_VERSION_1") {
            val range = extractUsageParameters()

            val overview = providerUsageQueryService.queryUsage(CLAUDE, range)

            call.respond(overview.toResponse())
        }

        get("/api/usage/claude/series/$API_VERSION_1") {
            val (range, granularity) = extractSeriesParameters()

            val series = providerUsageSeriesQueryService.querySeries(CLAUDE, range, granularity)

            call.respond(series.toResponse(CLAUDE))
        }

        get("/api/usage/copilot/$API_VERSION_1") {
            val range = extractUsageParameters()

            val usage = providerUsageQueryService.queryUsage(COPILOT, range)

            call.respond(usage.toResponse())
        }

        get("/api/usage/copilot/series/$API_VERSION_1") {
            val (range, granularity) = extractSeriesParameters()

            val series = providerUsageSeriesQueryService.querySeries(COPILOT, range, granularity)

            call.respond(series.toResponse(COPILOT))
        }

        get("/api/usage/opencode/$API_VERSION_1") {
            val range = extractUsageParameters()

            val usage = providerUsageQueryService.queryUsage(OPENCODE, range)

            val message = usage.toResponse()
            call.respond(message)
        }

        get("/api/usage/opencode/series/$API_VERSION_1") {
            val (range, granularity) = extractSeriesParameters()

            val series = providerUsageSeriesQueryService.querySeries(OPENCODE, range, granularity)

            call.respond(series.toResponse(OPENCODE))
        }

        get("/api/usage/all/$API_VERSION_1") {
            val range = extractUsageParameters()

            val overview = allUsageQueryService.queryAllUsage(range)

            call.respond(overview.toResponse())
        }

        get("/api/usage/all/series/$API_VERSION_1") {
            val (range, granularity) = extractSeriesParameters()

            val series = allUsageSeriesQueryService.queryAllUsageSeries(range, granularity)

            call.respond(series.toResponse())
        }

        get("/api/prices/claude") {
            call.respond(currentPricesQueryService.queryCurrentPrices().toResponse())
        }

        post("/api/usage/ingest/claude") {
            requireBodyWithinLimit(call.request.contentLength())
            val request = call.receive<ClaudeUsageReport>()
            claudeIngestService.ingest(request.toCommand())
            call.respond(HttpStatusCode.Created)
        }

        post("/api/usage/ingest/copilot") {
            requireBodyWithinLimit(call.request.contentLength())
            val request = call.receive<CopilotUsageReport>()
            copilotIngestService.ingest(request.toCommand())
            call.respond(HttpStatusCode.Created)
        }

        post("/api/usage/ingest/opencode") {
            requireBodyWithinLimit(call.request.contentLength())
            val request = call.receive<OpenCodeUsageReport>()
            val command = request.toCommand()
            // toCommand() returns null when all entries were noise; that is a successful ingest of nothing.
            if (command != null) {
                openCodeIngestService.ingest(command)
            }
            call.respond(HttpStatusCode.Created)
        }
    }
}

private fun RoutingContext.extractUsageParameters(): DayRange {
    val from =
        call.parameters["from"]?.let(LocalDate::parse)?.let { Day(it) }
            ?: throw BadRequestException("missing required query parameter 'from'")
    val to =
        call.parameters["to"]?.let(LocalDate::parse)?.let { Day(it) }
            ?: throw BadRequestException("missing required query parameter 'to'")
    return DayRange.of(from, to)
}

private data class SeriesQuery(
    val range: DayRange,
    val granularity: Granularity,
)

private fun RoutingContext.extractSeriesParameters(): SeriesQuery =
    SeriesQuery(
        range = extractUsageParameters(),
        granularity = granularityFrom(call.parameters["granularity"] ?: "day"),
    )
