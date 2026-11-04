package de.neuland.tokendashboard.domain.model

@JvmInline
value class UserCount(
    val value: Int,
) {
    init {
        require(value >= 0) { "UserCount must not be negative, but was $value" }
    }
}
