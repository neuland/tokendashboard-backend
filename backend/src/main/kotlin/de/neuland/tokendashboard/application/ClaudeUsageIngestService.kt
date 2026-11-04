package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestClaudeUsagePort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import org.slf4j.LoggerFactory

class ClaudeUsageIngestService(
    private val usageRepository: ClaudeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
) : IngestClaudeUsagePort {
    private val logger = LoggerFactory.getLogger(ClaudeUsageIngestService::class.java)

    override fun ingest(command: IngestClaudeUsageRecordCommand) {
        logger.info("Ingesting {} Claude prompts", command.prompts.size)
        userRepository.upsert(command.userId, CLAUDE, command.pluginVersion)

        val records =
            command
                .nonSyntheticPrompts()
                .map { usage ->
                    with(usage) {
                        ClaudeUsageRecord(
                            model = model,
                            timestamp = timestamp,
                            sessionId = sessionId,
                            promptId = promptId,
                            tokens = tokenCounts,
                            pluginVersion = command.pluginVersion,
                        )
                    }
                }

        usageRepository.insertAll(records)
    }
}
