package de.neuland.tokendashboard.di

import de.neuland.tokendashboard.adapter.outgoing.plugins.DatabaseHealthCheck
import de.neuland.tokendashboard.adapter.outgoing.repository.ClaudePriceRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.ClaudePriceRepositoryAdapter
import de.neuland.tokendashboard.adapter.outgoing.repository.ClaudeUsageRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.ClaudeUsageRepositoryAdapter
import de.neuland.tokendashboard.adapter.outgoing.repository.Co2FactorRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.Co2FactorRepositoryAdapter
import de.neuland.tokendashboard.adapter.outgoing.repository.CopilotUsageRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.CopilotUsageRepositoryAdapter
import de.neuland.tokendashboard.adapter.outgoing.repository.OpenCodeUsageRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.OpenCodeUsageRepositoryAdapter
import de.neuland.tokendashboard.adapter.outgoing.repository.UserRepository
import de.neuland.tokendashboard.adapter.outgoing.repository.UserRepositoryAdapter
import de.neuland.tokendashboard.application.AllUsageQueryService
import de.neuland.tokendashboard.application.AllUsageSeriesQueryService
import de.neuland.tokendashboard.application.ClaudeUsageIngestService
import de.neuland.tokendashboard.application.CopilotUsageIngestService
import de.neuland.tokendashboard.application.CurrentPricesQueryService
import de.neuland.tokendashboard.application.OpenCodeUsageIngestService
import de.neuland.tokendashboard.application.ProviderUsageQueryService
import de.neuland.tokendashboard.application.ProviderUsageSeriesQueryService
import de.neuland.tokendashboard.application.port.incoming.IngestClaudeUsagePort
import de.neuland.tokendashboard.application.port.incoming.IngestCopilotUsagePort
import de.neuland.tokendashboard.application.port.incoming.IngestOpenCodeUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryAllUsageSeriesPort
import de.neuland.tokendashboard.application.port.incoming.QueryCurrentPricesPort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsagePort
import de.neuland.tokendashboard.application.port.incoming.QueryProviderUsageSeriesPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudePriceRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.ClaudeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.Co2FactorRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.CopilotUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.OpenCodeUsageRepositoryPort
import de.neuland.tokendashboard.application.port.outgoing.UserRepositoryPort
import org.jdbi.v3.core.Jdbi
import org.koin.dsl.module

val repositoryModule =
    module {
        single<ClaudeUsageRepositoryPort> {
            ClaudeUsageRepositoryAdapter(get<Jdbi>().onDemand(ClaudeUsageRepository::class.java))
        }
        single<UserRepositoryPort> {
            UserRepositoryAdapter(get<Jdbi>().onDemand(UserRepository::class.java), get())
        }
        single<Co2FactorRepositoryPort> {
            Co2FactorRepositoryAdapter(get<Jdbi>().onDemand(Co2FactorRepository::class.java))
        }
        single<ClaudePriceRepositoryPort> {
            ClaudePriceRepositoryAdapter(get<Jdbi>().onDemand(ClaudePriceRepository::class.java))
        }
        single<CopilotUsageRepositoryPort> {
            CopilotUsageRepositoryAdapter(get<Jdbi>().onDemand(CopilotUsageRepository::class.java))
        }
        single<OpenCodeUsageRepositoryPort> {
            OpenCodeUsageRepositoryAdapter(get<Jdbi>().onDemand(OpenCodeUsageRepository::class.java))
        }
        single { DatabaseHealthCheck(get()) }
    }

val serviceModule =
    module {
        single<IngestClaudeUsagePort> { ClaudeUsageIngestService(get(), get(), get(), get()) }
        single<IngestCopilotUsagePort> { CopilotUsageIngestService(get(), get(), get()) }
        single<IngestOpenCodeUsagePort> { OpenCodeUsageIngestService(get(), get(), get()) }
        single<QueryProviderUsagePort> { ProviderUsageQueryService(get(), get(), get(), get()) }
        single<QueryProviderUsageSeriesPort> { ProviderUsageSeriesQueryService(get(), get(), get(), get()) }
        single<QueryAllUsagePort> { AllUsageQueryService(get(), get(), get()) }
        single<QueryAllUsageSeriesPort> { AllUsageSeriesQueryService(get(), get(), get(), get()) }
        single<QueryCurrentPricesPort> { CurrentPricesQueryService(get(), get()) }
    }
