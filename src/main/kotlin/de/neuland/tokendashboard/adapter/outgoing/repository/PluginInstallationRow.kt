package de.neuland.tokendashboard.adapter.outgoing.repository

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.time.LocalDate

data class PluginInstallationRow(
    val date: LocalDate,
    @param:ColumnName("count") val count: Long,
)
