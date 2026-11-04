package de.neuland.tokendashboard.adapter.outgoing.plugins

import org.jdbi.v3.core.Jdbi
import org.slf4j.LoggerFactory

class DatabaseHealthCheck(
    private val jdbi: Jdbi,
) {
    private val logger = LoggerFactory.getLogger(DatabaseHealthCheck::class.java)

    fun isReachable(): Boolean =
        try {
            jdbi.withHandle<Unit, Exception> { it.execute("SELECT 1") }
            true
        } catch (e: Exception) {
            logger.error("Health check DB ping failed", e)
            false
        }
}
