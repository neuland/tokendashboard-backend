package de.neuland.tokendashboard.domain.model

import kotlin.math.max

data class IncomingCacheWriteTokens(
    val tokens5m: TokenCount,
    val tokens1h: TokenCount,
    val generic: TokenCount,
) {
    fun totalCached() = tokens5m + tokens1h + generic

    /**
     * Cache writes that the provider reported as a total but did not break down into a 5m/1h
     * bucket are billed at the 1h rate — see docs/decisions.md #8.
     */
    fun billedAt1hRate() = tokens1h + generic

    companion object {
        /**
         * `cache_creation_input_tokens` is the reported total and does not necessarily equal
         * `ephemeral_5m_input_tokens + ephemeral_1h_input_tokens`. The unattributed remainder is
         * kept as `generic` — see docs/decisions.md #8. A negative remainder means the provider
         * reported inconsistent numbers and is clamped to zero rather than trusted.
         */
        fun fromReportedTotals(
            reportedTotal: TokenCount,
            tokens5m: TokenCount,
            tokens1h: TokenCount,
        ): IncomingCacheWriteTokens =
            IncomingCacheWriteTokens(
                tokens5m = tokens5m,
                tokens1h = tokens1h,
                generic = TokenCount(max(reportedTotal.value - tokens5m.value - tokens1h.value, 0)),
            )
    }
}

data class IncomingTokenCounts(
    val inputTokens: TokenCount,
    val outputTokens: TokenCount,
    val cacheReadTokens: TokenCount,
    val cacheWriteTokens: IncomingCacheWriteTokens,
)
