package de.neuland.tokendashboard.domain.model

@JvmInline
value class PluginVersion(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "PluginVersion must not be blank" }
        require(value.length <= MAX_LENGTH) { "PluginVersion must not exceed $MAX_LENGTH characters, but was ${value.length}" }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}
