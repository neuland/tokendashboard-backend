package de.neuland.tokendashboard.adapter.outgoing.repository

import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily

class ClaudePriceRepositoryAdapter(
    private val jdbi: ClaudePriceRepository,
) : ClaudePriceRepositoryPort {
    override fun priceFor(
        family: ModelFamily,
        date: Day,
    ): ClaudePrices? = jdbi.findLatestPriceFor(family.value, date.value)?.toDomain()

    override fun currentPricesPerFamily(date: Day): Map<ModelFamily, ClaudePrices> =
        jdbi.findAllLatestPrices(date.value).associate { ModelFamily(it.model) to it.toDomain() }
}
