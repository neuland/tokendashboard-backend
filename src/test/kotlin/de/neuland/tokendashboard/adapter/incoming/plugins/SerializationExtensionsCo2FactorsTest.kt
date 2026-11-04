package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.Co2Factor
import de.neuland.tokendashboard.domain.model.Co2FactorVersion
import de.neuland.tokendashboard.domain.model.Co2FactorVersions
import de.neuland.tokendashboard.domain.model.Co2Factors
import de.neuland.tokendashboard.domain.model.Day
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class SerializationExtensionsCo2FactorsTest :
    FunSpec({
        val factors =
            Co2Factors(
                inputFactor = Co2Factor(70.0),
                outputFactor = Co2Factor(1400.0),
                cacheReadFactor = Co2Factor(0.7),
                cacheWriteFactor = Co2Factor(87.0),
            )

        test("maps every factor as gram CO2 per million tokens") {
            val dto = factors.toResponse()

            dto.inputGramCo2PerMillion shouldBe 70.0
            dto.outputGramCo2PerMillion shouldBe 1400.0
            dto.cacheReadGramCo2PerMillion shouldBe 0.7
            dto.cacheWriteGramCo2PerMillion shouldBe 87.0
        }

        test("maps a version with its family and ISO validity date") {
            val dto =
                Co2FactorVersions(
                    listOf(
                        Co2FactorVersion(
                            ModelFamily("opus"),
                            Day(LocalDate.of(2026, 1, 1)),
                            factors,
                        ),
                    ),
                ).toResponse().first()

            dto.modelFamily shouldBe "opus"
            dto.validFrom shouldBe "2026-01-01"
            dto.factors shouldBe factors.toResponse()
        }
    })
