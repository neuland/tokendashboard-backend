package de.neuland.tokendashboard.adapter.outgoing.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DatabaseTest :
    FunSpec({

        test("requireNonDefaultPassword throws on default password when not allowed") {
            shouldThrow<IllegalArgumentException> {
                requireNonDefaultPassword(DEFAULT_DATABASE_PASSWORD, allowDefaultCredentials = false)
            }
        }

        test("requireNonDefaultPassword does not throw on default password when explicitly allowed") {
            requireNonDefaultPassword(DEFAULT_DATABASE_PASSWORD, allowDefaultCredentials = true)
        }

        test("requireNonDefaultPassword does not throw on non-default password") {
            requireNonDefaultPassword("some-secret", allowDefaultCredentials = false)
        }

        test("requireValidTimeZone accepts UTC") {
            requireValidTimeZone("UTC") shouldBe "UTC"
        }

        test("requireValidTimeZone accepts an IANA zone id") {
            requireValidTimeZone("Asia/Tokyo") shouldBe "Asia/Tokyo"
        }

        test("requireValidTimeZone throws on an invalid zone id") {
            shouldThrow<IllegalArgumentException> { requireValidTimeZone("not-a-zone") }
        }
    })
