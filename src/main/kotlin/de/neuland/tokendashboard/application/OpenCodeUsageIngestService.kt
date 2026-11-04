package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestOpenCodeUsagePort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.OpenCodeUsageRecord
import de.neuland.tokendashboard.domain.model.Provider.OPENCODE
import org.slf4j.LoggerFactory

class OpenCodeUsageIngestService(
    private val usageRepository: OpenCodeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : IngestOpenCodeUsagePort {
    private val logger = LoggerFactory.getLogger(OpenCodeUsageIngestService::class.java)

    override fun ingest(command: IngestOpenCodeUsageRecordCommand) {
        logger.info("Ingesting {} OpenCode prompts", command.prompts.size)
        userRepository.upsert(command.userId, OPENCODE, command.pluginVersion)

        val records =
            command
                .nonZeroPrompts()
                .map { usage ->
                    with(usage) {
                        OpenCodeUsageRecord(
                            model = model,
                            llmProvider = llmProvider,
                            timestamp = timestamp,
                            sessionId = sessionId,
                            promptId = promptId,
                            tokens = tokenCounts,
                            costNanoCent = cost,
                            pluginVersion = command.pluginVersion,
                        )
                    }
                }

        usageRepository.insertAll(records)
    }
}
