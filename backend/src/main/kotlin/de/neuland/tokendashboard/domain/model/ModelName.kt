package de.neuland.tokendashboard.domain.model

@JvmInline
value class ModelName private constructor(
    val value: String,
) {
    fun family(): ModelFamily =
        when {
            value.contains(CLAUDE) -> claudeFamily(value)
            value.contains(GPT) -> gptFamily(value)
            else -> ModelFamily(value)
        }

    private fun claudeFamily(value: String): ModelFamily =
        value
            .split("-")
            .firstOrNull { it != CLAUDE && it.all(Char::isLetter) }
            ?.let { ModelFamily(it) } ?: ModelFamily(value)

    private fun gptFamily(value: String): ModelFamily =
        gptPattern
            .find(value)
            ?.let { buildCopilotFamilyFromComponents(it) }
            ?: ModelFamily(value)

    private fun buildCopilotFamilyFromComponents(match: MatchResult): ModelFamily {
        val major = match.groupValues[1]
        val suffix = match.groupValues[2]
        return ModelFamily("gpt-$major$suffix")
    }

    companion object {
        const val MAX_LENGTH = 50
        private const val GPT = "gpt"
        private const val CLAUDE = "claude"
        private val dateSuffixRegex = Regex("-\\d{4}-\\d{2}-\\d{2}$|-\\d{8}$")
        private val gptPattern = Regex("^gpt-(\\d+)(?:\\.\\d+)?(-[a-z]+)?")

        fun restore(modelName: String): ModelName = ModelName(modelName)

        fun from(rawModel: String): ModelName {
            require(rawModel.length <= MAX_LENGTH) { "ModelName must not exceed $MAX_LENGTH characters, but was ${rawModel.length}" }
            return ModelName(rawModel.lowercase().replace(dateSuffixRegex, ""))
        }
    }
}
