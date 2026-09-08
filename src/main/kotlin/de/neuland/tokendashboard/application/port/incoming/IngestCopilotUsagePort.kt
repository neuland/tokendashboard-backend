package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.application.IngestCopilotUsageRecordCommand

interface IngestCopilotUsagePort {
    fun ingest(command: IngestCopilotUsageRecordCommand)
}
