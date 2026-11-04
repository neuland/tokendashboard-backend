package de.neuland.tokendashboard.domain.model

import arrow.core.NonEmptyList
import arrow.core.toNonEmptyListOrNull

sealed interface ModelUsages {
    data object NoUsage : ModelUsages

    data class HasUsage(
        val entries: NonEmptyList<ModelUsage>,
    ) : ModelUsages
}

/**
 * Turns a possibly empty list into the matching [ModelUsages] variant. This is the only place that
 * builds a `HasUsage`, so "has usage but the list is empty" cannot occur.
 */
fun List<ModelUsage>.toModelUsages(): ModelUsages =
    if (isEmpty()) {
        ModelUsages.NoUsage
    } else {
        ModelUsages.HasUsage(toNonEmptyListOrNull()!!) // not null: the isEmpty branch above already returned
    }

fun ModelUsages.totals(): UsageTotals =
    when (this) {
        is ModelUsages.NoUsage -> UsageTotals.zero()
        is ModelUsages.HasUsage -> entries.totals()
    }

fun Iterable<ModelUsage>.totals(): UsageTotals = fold(UsageTotals.zero()) { acc, usage -> acc + usage }

fun ModelUsages.groupByFamily(): List<ModelFamilyUsage> =
    when (this) {
        is ModelUsages.NoUsage -> {
            emptyList()
        }

        is ModelUsages.HasUsage -> {
            entries
                .groupByNonEmpty { it.model.family() }
                .map { (family, usages) -> ModelFamilyUsage(family, usages) }
        }
    }

operator fun ModelUsages.plus(other: ModelUsages): ModelUsages =
    when (this) {
        is ModelUsages.NoUsage -> {
            other
        }

        is ModelUsages.HasUsage -> {
            when (other) {
                is ModelUsages.NoUsage -> this
                is ModelUsages.HasUsage -> sum(this, other)
            }
        }
    }

private fun sum(
    first: ModelUsages.HasUsage,
    second: ModelUsages.HasUsage,
): ModelUsages {
    val firstModelUsages = reduceToModels(first)
    val secondModelUsages = reduceToModels(second)

    val allModels = firstModelUsages.keys + secondModelUsages.keys
    val x: Map<ModelName, ModelUsage> =
        allModels.associateWith { model ->
            val first = firstModelUsages[model]
            val second = secondModelUsages[model]
            when {
                // key was in first or second list
                first == null -> {
                    second!!
                }

                second == null -> {
                    first
                }

                else -> {
                    ModelUsage(
                        model = model,
                        first.tokens + second.tokens,
                        first.costUsdCent + second.costUsdCent,
                        first.estimatedCO2 + second.estimatedCO2,
                    )
                }
            }
        }
    return x.values.toList().toModelUsages()
}

private fun reduceToModels(usages: ModelUsages.HasUsage): Map<ModelName, ModelUsage> =
    usages.entries
        .groupByNonEmpty { it.model }
        .associate { (key, value) ->
            val reduced =
                value.reduce { acc, usage ->
                    ModelUsage(
                        acc.model,
                        acc.tokens + usage.tokens,
                        acc.costUsdCent + usage.costUsdCent,
                        acc.estimatedCO2 + usage.estimatedCO2,
                    )
                }
            key to reduced
        }

fun <T, K> NonEmptyList<T>.groupByNonEmpty(keySelector: (T) -> K): NonEmptyList<Pair<K, NonEmptyList<T>>> =
    all
        .groupBy(keySelector)
        .map { (key, group) -> key to group.toNonEmptyListOrNull()!! } // will never be empty since source is not empty
        .toNonEmptyListOrNull()!! // will also never be empty
