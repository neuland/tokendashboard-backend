package de.neuland.tokendashboard.domain.model

@JvmInline
value class Co2Factor(
    val gramCo2PerMillionToken: Double,
) {
    init {
        require(gramCo2PerMillionToken >= 0) { "Co2Factor must not be negative, but was $gramCo2PerMillionToken" }
    }
}
