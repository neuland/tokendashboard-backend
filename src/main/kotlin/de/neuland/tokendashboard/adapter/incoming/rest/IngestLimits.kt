package de.neuland.tokendashboard.adapter.incoming.rest

const val MAX_PROMPTS_PER_REPORT = 5_000

/**
 * Upper bound for an ingest request body. [MAX_PROMPTS_PER_REPORT] alone is not enough:
 * it can only be checked once the whole body has been deserialized into memory, so the
 * body size has to be rejected before that happens.
 *
 * 8 MiB is roughly an order of magnitude above a full report of
 * [MAX_PROMPTS_PER_REPORT] prompts.
 */
const val MAX_REQUEST_BODY_BYTES = 8L * 1024 * 1024

class RequestBodyTooLargeException(
    contentLength: Long,
) : Exception("request body of $contentLength bytes exceeds the limit of $MAX_REQUEST_BODY_BYTES bytes")

class MissingContentLengthException : Exception("request must declare a Content-Length; chunked bodies are not accepted")

/**
 * Rejects an ingest request before its body is read. Throws instead of returning a
 * result because both failures are protocol-level and are mapped to a status code in
 * `StatusPages`.
 */
fun requireBodyWithinLimit(contentLength: Long?) {
    if (contentLength == null) throw MissingContentLengthException()
    if (contentLength > MAX_REQUEST_BODY_BYTES) throw RequestBodyTooLargeException(contentLength)
}
