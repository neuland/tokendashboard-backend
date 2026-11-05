package de.neuland.tokendashboard.domain.model

import kotlin.math.roundToLong

private const val NANO_FACTOR = 1_000_000_000L

@JvmInline
value class NanoCent(
    val value: Long,
) {
    fun toCent(): DollarCent = DollarCent((value + NANO_FACTOR / 2).floorDiv(NANO_FACTOR))

    operator fun plus(other: NanoCent): NanoCent = NanoCent(value + other.value)

    companion object {
        fun fromDollar(dollar: Double): NanoCent = NanoCent((dollar * 100 * NANO_FACTOR).roundToLong())
    }
}
