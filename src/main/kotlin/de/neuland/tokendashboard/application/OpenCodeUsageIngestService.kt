package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestOpenCodeUsagePort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import org.slf4j.LoggerFactory

class OpenCodeUsageIngestService(
    private val usageRepository: OpenCodeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : IngestOpenCodeUsagePort {
    private val logger = LoggerFactory.getLogger(OpenCodeUsageIngestService::class.java)

    override fun ingest(command: IngestOpenCodeUsageRecordCommand) {
        logger.info("Ingesting {} OpenCode prompts", command.prompts.size)
        userRepository.upsert(command.userId, OPENCODE, command.pluginVersion)

        val co2FactorLookup = Co2FactorLookup(co2FactorRepository)

        val records =
            command
                .nonZeroPrompts()
                .map { usage ->
                    val day = Day.atUtc(usage.timestamp)
                    val family = usage.model.family()
                    val factor = co2FactorLookup.factorFor(family, day)
                    with(usage) {
                        OpenCodeUsageRecord(
                            model = model,
                            llmProvider = llmProvider,
                            timestamp = timestamp,
                            sessionId = sessionId,
                            promptId = promptId,
                            tokens = tokenCounts,
                            costNanoCent = cost,
                            co2Gram = factor?.calculateCo2Gram(tokenCounts) ?: Co2Gram(0.0),
                            pluginVersion = command.pluginVersion,
                        )
                    }
                }

        usageRepository.insertAll(records)
    }
}
