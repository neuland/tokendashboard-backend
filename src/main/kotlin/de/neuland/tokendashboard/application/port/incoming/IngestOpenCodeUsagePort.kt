package de.neuland.tokendashboard.application.port.incoming

import de.neuland.tokendashboard.application.IngestOpenCodeUsageRecordCommand

interface IngestOpenCodeUsagePort {
    fun ingest(command: IngestOpenCodeUsageRecordCommand)
}
