package de.neuland.tokendashboard.domain.model

private const val NANO_FACTOR = 1_000_000_000L

/**
 * AI-Units (AIU) are GitHub Copilot's billing unit.
 * 1,000,000,000 (1 billion) Nano-AIU correspond to costs of one cent.
 */
@JvmInline
value class NanoAiu(
    val value: Long,
) {
    fun toAiu(): Long = (value + NANO_FACTOR / 2).floorDiv(NANO_FACTOR)

    operator fun plus(other: NanoAiu): NanoAiu = NanoAiu(value + other.value)
}
