package org.rsmod.tools.wiki.dumping.wiki

/** Inner roll when accessing the herb drop table (1, 2, or 3 independent herb rolls). */
data class HerbRollVariant(
    val herbCount: Int,
    val numerator: Int,
    val denominator: Int,
) {
    init {
        require(herbCount in 1..3) { "herbCount must be 1..3" }
        require(denominator > 0) { "denominator must be positive" }
        require(numerator in 0..denominator) { "numerator must be within 0..denominator" }
    }
}

object HerbDropTableParser {
    val standardMultiRollVariants: List<HerbRollVariant> =
        listOf(
            HerbRollVariant(herbCount = 1, numerator = 11, denominator = 26),
            HerbRollVariant(herbCount = 2, numerator = 11, denominator = 26),
            HerbRollVariant(herbCount = 3, numerator = 4, denominator = 26),
        )

    fun parseMainAccess(text: String): Pair<Int, Int>? {
        val rollingTable =
            Regex(
                    """(\d+)\s*/\s*(\d+)\s+chance of rolling (?:the\s+)?(?:\[\[)?(?:[^|\]]+\|)?herb drop table""",
                    RegexOption.IGNORE_CASE,
                )
                .find(text)
                ?.destructured
                ?.let { (num, den) ->
                    val numerator = num.toIntOrNull() ?: return@let null
                    val denominator = den.toIntOrNull() ?: return@let null
                    numerator to denominator
                }
        if (rollingTable != null) {
            return rollingTable
        }
        return parseFraction(text)
    }

    fun parseRollVariants(text: String): List<HerbRollVariant>? {
        val variants =
            Regex(
                    """(\d+)\s*/\s*(\d+)\s+chance of dropping (\d+) herbs?""",
                    RegexOption.IGNORE_CASE,
                )
                .findAll(text)
                .mapNotNull { match ->
                    val numerator = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val denominator = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    val herbCount = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                    if (herbCount !in 1..3) {
                        return@mapNotNull null
                    }
                    HerbRollVariant(herbCount, numerator, denominator)
                }
                .sortedBy { it.herbCount }
                .toList()

        return variants.ifEmpty { null }
    }

    fun isVariableQuantityRange(quantity: String): Boolean {
        val trimmed = quantity.trim()
        if (!trimmed.contains('-')) {
            return false
        }
        val parts = trimmed.split('-', limit = 2)
        if (parts.size != 2) {
            return false
        }
        val min = parts[0].trim().toIntOrNull() ?: return false
        val max = parts[1].trim().toIntOrNull() ?: return false
        return min >= 1 && max > min
    }

    fun isStandardMultiRoll(variants: List<HerbRollVariant>): Boolean =
        variants == standardMultiRollVariants

    private fun parseFraction(text: String): Pair<Int, Int>? {
        val fraction = Regex("""(\d+)\s*/\s*(\d+)""").find(text.trim()) ?: return null
        val numerator = fraction.groupValues[1].toIntOrNull() ?: return null
        val denominator = fraction.groupValues[2].toIntOrNull() ?: return null
        return numerator to denominator
    }
}
