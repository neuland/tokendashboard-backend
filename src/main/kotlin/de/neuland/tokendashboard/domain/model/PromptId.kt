package de.neuland.tokendashboard.domain.model

@JvmInline
value class PromptId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "PromptId must not be blank" }
        require(value.length <= MAX_LENGTH) { "PromptId must not exceed $MAX_LENGTH characters, but was ${value.length}" }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
