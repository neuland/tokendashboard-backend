package de.neuland.tokendashboard.domain.model

@JvmInline
value class CentPerMillionToken(
    val value: Long,
) {
    init {
        require(value >= 0) { "CentPerMillionToken must not be negative, but was $value" }
    }

    companion object {
        val ZERO = CentPerMillionToken(0)
    }

    fun centPerToken(): Double = value / 1_000_000.0
}
