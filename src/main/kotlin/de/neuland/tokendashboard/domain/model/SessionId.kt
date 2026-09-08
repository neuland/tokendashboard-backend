package de.neuland.tokendashboard.domain.model

@JvmInline
value class SessionId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "SessionId must not be blank" }
        require(value.length <= MAX_LENGTH) { "SessionId must not exceed $MAX_LENGTH characters, but was ${value.length}" }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
