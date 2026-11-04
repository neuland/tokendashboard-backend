package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.application.IngestClaudeUsageRecordCommand

interface IngestClaudeUsagePort {
    fun ingest(command: IngestClaudeUsageRecordCommand)
}
