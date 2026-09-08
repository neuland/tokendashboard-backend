package de.neuland.tokendashboard.domain.model

@JvmInline
value class UserId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank" }
        require(value.length <= MAX_LENGTH) { "UserId must not exceed $MAX_LENGTH characters, but was ${value.length}" }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
