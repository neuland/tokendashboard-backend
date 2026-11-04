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

data class ClaudeTokenCounts(
    val inputTokens: TokenCount,
    val outputTokens: TokenCount,
    val cacheReadTokens: TokenCount,
    val cacheWriteTokens5m: TokenCount,
    val cacheWriteTokens1h: TokenCount,
    val cacheWriteTokensGeneric: TokenCount,
) {
    fun totalCached() = cacheReadTokens + cacheWriteTokens5m + cacheWriteTokens1h + cacheWriteTokensGeneric

    fun cacheWrite() = cacheWriteTokens5m + cacheWriteTokens1h + cacheWriteTokensGeneric

    fun cacheWriteBilledAt1hRate() = cacheWriteTokens1h + cacheWriteTokensGeneric

    fun toTokenCounts() =
        TokenCounts(
            inputTokens = inputTokens,
            outputTokens = outputTokens,
            cacheReadTokens = cacheReadTokens,
            cacheWriteTokens = cacheWriteTokens5m + cacheWriteTokens1h + cacheWriteTokensGeneric,
        )
}
