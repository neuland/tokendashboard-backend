package de.neuland.tokendashboard.adapter.incoming.plugins

import de.neuland.tokendashboard.domain.model.CentPerMillionToken
import de.neuland.tokendashboard.domain.model.ClaudePrices
import de.neuland.tokendashboard.domain.model.ModelFamily
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SerializationExtensionsPricesTest :
    FunSpec({
        fun prices(
            input: Long = 10,
            output: Long = 20,
            cacheWrite5m: Long = 30,
            cacheWrite1h: Long = 40,
            cacheRead: Long = 50,
        ) = ClaudePrices(
            inputCentPerMillion = CentPerMillionToken(input),
            outputCentPerMillion = CentPerMillionToken(output),
            cacheWrite5mCentPerMillion = CentPerMillionToken(cacheWrite5m),
            cacheWrite1hCentPerMillion = CentPerMillionToken(cacheWrite1h),
            cacheReadCentPerMillion = CentPerMillionToken(cacheRead),
        )

        test("maps every price field") {
            val dto = prices(input = 10, output = 20, cacheWrite5m = 30, cacheWrite1h = 40, cacheRead = 50).toResponse()

            dto.inputCentPerMillion shouldBe 10
            dto.outputCentPerMillion shouldBe 20
            dto.cacheWrite5mCentPerMillion shouldBe 30
            dto.cacheWrite1hCentPerMillion shouldBe 40
            dto.cacheReadCentPerMillion shouldBe 50
        }

        // Undifferentiated "generic" cache-write tokens are billed at the 1h rate in backend code.
        // See docs/decisions.md #8.
        // Change the test if your system handles it otherwise.
        test("reports the 1h rate as the generic cache write price") {
            val dto = prices(cacheWrite1h = 40).toResponse()

            dto.cacheWriteCentPerMillion shouldBe dto.cacheWrite1hCentPerMillion
        }

        test("maps a map of families keyed by family name") {
            val dtos =
                mapOf(
                    ModelFamily("sonnet") to prices(input = 10),
                    ModelFamily("opus") to prices(input = 20),
                ).toResponse()

            dtos.keys shouldBe setOf("sonnet", "opus")
            dtos.getValue("sonnet").inputCentPerMillion shouldBe 10
            dtos.getValue("opus").inputCentPerMillion shouldBe 20
        }
    })
