package de.neuland.tokendashboard.domain.model

@JvmInline
value class Co2Factor(
    val gramCo2PerMillionToken: Double,
) {
    companion object {
        val ZERO = Co2Factor(0.0)
    }

    init {
        require(gramCo2PerMillionToken >= 0) { "Co2Factor must not be negative, but was $gramCo2PerMillionToken" }
    }
}
