package de.neuland.tokendashboard.domain.model

data class TokenCounts(
    val inputTokens: TokenCount,
    val outputTokens: TokenCount,
    val cacheReadTokens: TokenCount,
    val cacheWriteTokens: TokenCount,
) {
    operator fun plus(other: TokenCounts): TokenCounts =
        TokenCounts(
            inputTokens + other.inputTokens,
            outputTokens + other.outputTokens,
            cacheReadTokens + other.cacheReadTokens,
            cacheWriteTokens + other.cacheWriteTokens,
        )

    fun sumInAndOut(): TokenCount = inputTokens + outputTokens

    fun sumOverAll(): TokenCount = inputTokens + outputTokens + cacheReadTokens + cacheWriteTokens

    companion object {
        fun zero() = TokenCounts(TokenCount(0), TokenCount(0), TokenCount(0), TokenCount(0))
    }
}
