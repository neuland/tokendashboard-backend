package de.neuland.tokendashboard.domain.model

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull

sealed interface UsageResult {
    data object NoUsage : UsageResult

    data class HasUsage(
        val entries: NonEmptyList<ModelUsage>,
    ) : UsageResult
}

/**
 * Turns a possibly empty list into the matching [UsageResult] variant. This is the only place that
 * builds a `HasUsage`, so "has usage but the list is empty" cannot occur.
 */
fun List<ModelUsage>.toUsageResult(): UsageResult =
    if (isEmpty()) {
        UsageResult.NoUsage
    } else {
        UsageResult.HasUsage(toNonEmptyListOrNull()!!) // not null: the isEmpty branch above already returned
    }

fun UsageResult.groupByFamily(): List<ModelFamilyUsage> =
    when (this) {
        is UsageResult.NoUsage -> {
            emptyList()
        }

        is UsageResult.HasUsage -> {
            entries
                .groupByNonEmpty { it.model.family() }
                .map { (family, usages) -> ModelFamilyUsage(family, usages) }
        }
    }

fun <T, K> NonEmptyList<T>.groupByNonEmpty(keySelector: (T) -> K): NonEmptyList<Pair<K, NonEmptyList<T>>> =
    all
        .groupBy(keySelector)
        .map { (key, group) -> key to group.toNonEmptyListOrNull()!! } // will never be empty since source is not empty
        .toNonEmptyListOrNull()!! // will also never be empty
