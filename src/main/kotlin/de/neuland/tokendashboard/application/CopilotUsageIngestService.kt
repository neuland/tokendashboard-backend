package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestCopilotUsagePort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import org.slf4j.LoggerFactory

class CopilotUsageIngestService(
    private val repository: CopilotUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
) : IngestCopilotUsagePort {
    private val logger = LoggerFactory.getLogger(CopilotUsageIngestService::class.java)

    override fun ingest(command: IngestCopilotUsageRecordCommand) {
        logger.info("Ingesting {} Copilot prompt entries", command.prompts.size)
        userRepository.upsert(command.userId, COPILOT, command.pluginVersion)

        val co2FactorLookup = Co2FactorLookup(co2FactorRepository)

        val records =
            command.prompts.map { usage ->
                val day = Day.atUtc(usage.timestamp)
                val family = usage.model.family()
                val factor = co2FactorLookup.factorFor(family, day)
                with(usage) {
                    CopilotUsageRecord(
                        model = model,
                        timestamp = timestamp,
                        sessionId = sessionId,
                        tokens = tokenCounts,
                        nanoAiu = nanoAiu,
                        co2Gram = factor?.calculateCo2Gram(tokenCounts) ?: Co2Gram(0.0),
                        pluginVersion = command.pluginVersion,
                    )
                }
            }

        repository.insertAll(records)
    }
}
