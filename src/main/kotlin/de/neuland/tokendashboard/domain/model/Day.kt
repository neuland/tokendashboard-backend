package de.neuland.tokendashboard.domain.model

import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter

data class Day(
    val value: LocalDate,
) {
    companion object {
        val format: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun atUtc(timestamp: PromptTimestamp): Day = Day(timestamp.value.atOffset(UTC).toLocalDate())
    }

    fun print(): String = value.format(format)
}
