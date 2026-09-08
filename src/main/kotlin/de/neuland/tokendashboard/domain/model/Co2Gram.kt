package de.neuland.tokendashboard.domain.model

@JvmInline
value class Co2Gram(
    val value: Double,
) {
    operator fun plus(other: Co2Gram): Co2Gram = Co2Gram(value + other.value)
}
