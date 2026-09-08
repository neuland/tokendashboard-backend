package de.neuland.tokendashboard.application

import de.neuland.tokendashboard.application.port.incoming.IngestClaudeUsagePort
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ClaudeUsageRecord
import de.neuland.tokendashboard.domain.model.Co2Gram
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.DollarCent
import de.neuland.tokendashboard.domain.model.IncomingTokenCounts
import de.neuland.tokendashboard.domain.model.Provider.CLAUDE
import org.slf4j.LoggerFactory

class ClaudeUsageIngestService(
    private val usageRepository: ClaudeUsageRepositoryPort,
    private val userRepository: UserRepositoryPort,
    private val co2FactorRepository: Co2FactorRepositoryPort,
    private val priceRepository: ClaudePriceRepositoryPort,
) : IngestClaudeUsagePort {
    private val logger = LoggerFactory.getLogger(ClaudeUsageIngestService::class.java)

    override fun ingest(command: IngestClaudeUsageRecordCommand) {
        logger.info("Ingesting {} Claude prompts", command.prompts.size)
        userRepository.upsert(command.userId, CLAUDE, command.pluginVersion)

        val co2FactorLookup = Co2FactorLookup(co2FactorRepository)
        val priceLookup = PriceLookup(priceRepository)

        val records =
            command
                .nonSyntheticPrompts()
                .map { usage ->
                    val day = Day.atUtc(usage.timestamp)
                    val family = usage.model.family()
                    val factor = co2FactorLookup.factorFor(family, day)
                    val price = priceLookup.priceFor(family, day)
                    with(usage) {
                        ClaudeUsageRecord(
                            model = model,
                            timestamp = timestamp,
                            sessionId = sessionId,
                            promptId = promptId,
                            tokens = tokenCounts,
                            costUsdCent = calculateCost(price, tokenCounts),
                            co2Gram = factor?.calculateCo2Gram(tokenCounts) ?: Co2Gram(0.0),
                            pluginVersion = command.pluginVersion,
                        )
                    }
                }

        usageRepository.insertAll(records)
    }

    private fun calculateCost(
        price: ClaudePrices?,
        tokenCounts: IncomingTokenCounts,
    ): DollarCent = price?.calculateCost(tokenCounts) ?: DollarCent(0)
}
