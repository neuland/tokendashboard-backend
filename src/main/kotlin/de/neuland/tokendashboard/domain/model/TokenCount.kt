package de.neuland.tokendashboard.domain.model

@JvmInline
value class TokenCount(
    val value: Long,
) {
    init {
        require(value >= 0) { "TokenCount must not be negative, but was $value" }
    }

    companion object {
        val ZERO = TokenCount(0)
    }

    operator fun plus(other: TokenCount) = TokenCount(value + other.value)
}
