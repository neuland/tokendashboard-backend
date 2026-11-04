package de.neuland.tokendashboard.domain.model

@JvmInline
value class DollarCent(
    val value: Long,
) {
    operator fun plus(other: DollarCent): DollarCent = DollarCent(value + other.value)
}
