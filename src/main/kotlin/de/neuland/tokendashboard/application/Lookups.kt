package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import org.slf4j.LoggerFactory

/**
 * Looks up the CO2 factor for a model/day, caching results for the lifetime of this instance.
 * Create one per ingest run so the cache doesn't grow unbounded across calls.
 */
class Co2FactorLookup(
    private val co2FactorRepository: Co2FactorRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(Co2FactorLookup::class.java)
    private val cache = mutableMapOf<Pair<ModelFamily, Day>, CacheEntry<Co2Factors?>>()

    fun factorFor(
        family: ModelFamily,
        day: Day,
    ): Co2Factors? =
        cache
            .getOrPut(family to day) {
                val factor = co2FactorRepository.co2FactorsFor(family, day)
                if (factor == null) {
                    logger.warn("No carbon factor for model family={} date={}, CO2 defaulting to 0", family.value, day)
                }
                CacheEntry(factor)
            }.value
}

/**
 * Looks up the Claude price for a model/day, caching results for the lifetime of this instance.
 * Create one per ingest run so the cache doesn't grow unbounded across calls.
 */
class PriceLookup(
    private val priceRepository: ClaudePriceRepositoryPort,
) {
    private val logger = LoggerFactory.getLogger(PriceLookup::class.java)
    private val cache = mutableMapOf<Pair<ModelFamily, Day>, CacheEntry<ClaudePrices?>>()

    fun priceFor(
        family: ModelFamily,
        day: Day,
    ): ClaudePrices? =
        cache
            .getOrPut(family to day) {
                val price = priceRepository.priceFor(family, day)
                if (price == null) {
                    logger.warn("No price for model family={} date={}, cost defaulting to 0", family.value, day)
                }
                CacheEntry(price)
            }.value
}

/**
 * Wraps a possibly-null cached value so `getOrPut` can distinguish "not yet cached" from
 * "cached as null" — `getOrPut` only checks whether the stored value itself is null.
 */
data class CacheEntry<T>(
    val value: T,
)
