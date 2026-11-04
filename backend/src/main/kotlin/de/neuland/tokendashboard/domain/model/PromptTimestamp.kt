package de.neuland.tokendashboard.domain.model

import java.time.Instant

@JvmInline
value class PromptTimestamp(
    val value: Instant,
) : Comparable<PromptTimestamp> {
    override fun compareTo(other: PromptTimestamp): Int = value.compareTo(other.value)
}
