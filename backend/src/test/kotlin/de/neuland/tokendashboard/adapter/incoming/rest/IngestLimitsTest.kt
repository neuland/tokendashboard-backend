package de.neuland.tokendashboard.adapter.incoming.rest

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain

class IngestLimitsTest :
    FunSpec({
        test("accepts a body below the limit") {
            // when / then
            shouldNotThrowAny { requireBodyWithinLimit(1_024) }
        }

        test("accepts a body of exactly the limit") {
            // when / then
            shouldNotThrowAny { requireBodyWithinLimit(MAX_REQUEST_BODY_BYTES) }
        }

        test("rejects a body above the limit") {
            // when / then
            val exception = shouldThrow<RequestBodyTooLargeException> { requireBodyWithinLimit(MAX_REQUEST_BODY_BYTES + 1) }
            exception.message shouldContain "exceeds the limit"
        }

        test("rejects a request without a declared content length") {
            // when / then
            val exception = shouldThrow<MissingContentLengthException> { requireBodyWithinLimit(null) }
            exception.message shouldContain "Content-Length"
        }
    })
