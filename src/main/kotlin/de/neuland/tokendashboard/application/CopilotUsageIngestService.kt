package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestCopilotUsagePort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.CopilotUsageRecord
import de.neuland.tokendashboard.domain.model.Provider.COPILOT
import org.slf4j.LoggerFactory

class CopilotUsageIngestService(
    private val repository: CopilotUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : IngestCopilotUsagePort {
    private val logger = LoggerFactory.getLogger(CopilotUsageIngestService::class.java)

    override fun ingest(command: IngestCopilotUsageRecordCommand) {
        logger.info("Ingesting {} Copilot prompt entries", command.prompts.size)
        userRepository.upsert(command.userId, COPILOT, command.pluginVersion)

        val records =
            command.prompts.map { usage ->
                with(usage) {
                    CopilotUsageRecord(
                        model = model,
                        timestamp = timestamp,
                        sessionId = sessionId,
                        tokens = tokenCounts,
                        nanoAiu = nanoAiu,
                        pluginVersion = command.pluginVersion,
                    )
                }
            }

        repository.insertAll(records)
    }
}
