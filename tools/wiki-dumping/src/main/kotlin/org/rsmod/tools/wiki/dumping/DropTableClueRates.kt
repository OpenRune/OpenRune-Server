package org.rsmod.tools.wiki.dumping

import kotlin.math.floor

internal fun combatAchievementClueDenominator(baseDenominator: Int): Int =
    floor(baseDenominator - baseDenominator * 0.05).toInt()

internal data class RingOfWealthClueRate(
    val wealthDenominator: Int,
    val requiresWilderness: Boolean,
)

internal data class ClueTertiaryRollModifier(
    val requireRingOfWealth: Boolean = false,
    val excludeRingOfWealth: Boolean = false,
    val requireWilderness: Boolean = false,
)

internal data class ClueTertiaryRollExport(
    val denominator: Int,
    val modifier: ClueTertiaryRollModifier = ClueTertiaryRollModifier(),
)

private val RING_OF_WEALTH_NOTE = Regex("""ring\s*of\s*wealth""", RegexOption.IGNORE_CASE)
private val WEALTH_CLUE_RATE =
    Regex(
        """(?:increases?\s*to|(?:is\s*)?(?:increased|changed)\s*to|(?:drop\s*rate\s*(?:of|is(?:\s*increased\s*to)?)|rarity\s*changes?\s*to)|have\s+a\s+drop\s+rate\s+of)\s*(?:1\s*/\s*)?(\d+)""",
        RegexOption.IGNORE_CASE,
    )

internal fun parseRingOfWealthClueRate(note: String): RingOfWealthClueRate? {
    if (!RING_OF_WEALTH_NOTE.containsMatchIn(note)) {
        return null
    }
    val denominator = WEALTH_CLUE_RATE.find(note)?.groupValues?.get(1)?.toIntOrNull() ?: return null
    return RingOfWealthClueRate(
        wealthDenominator = denominator,
        requiresWilderness = note.contains("wilderness", ignoreCase = true),
    )
}

private val EXACT_RARITY_NOTE = Regex("""exact\s+rarity\s+is""", RegexOption.IGNORE_CASE)

internal fun isHandledTransformRateNote(note: String): Boolean =
    note.contains("Combat Achievements", ignoreCase = true) ||
        parseRingOfWealthClueRate(note) != null ||
        EXACT_RARITY_NOTE.containsMatchIn(note)

internal fun ResolvedDropEntry.hasCombatAchievementClueRateNote(): Boolean =
    wikiNotes.transformRate.any { it.contains("Combat Achievements", ignoreCase = true) }

internal fun ResolvedDropEntry.ringOfWealthClueRate(): RingOfWealthClueRate? =
    wikiNotes.transformRate.firstNotNullOfOrNull(::parseRingOfWealthClueRate)

internal fun ResolvedDropEntry.exportClueDenominator(baseDenominator: Int): Int {
    if (!hasCombatAchievementClueRateNote()) {
        return baseDenominator
    }
    val normalized = obj.removePrefix("obj.").lowercase()
    if ("beginner" in normalized) {
        return baseDenominator
    }
    return combatAchievementClueDenominator(baseDenominator)
}

/** Transform-rate wiki footnotes that still need manual implementation in Kotlin output. */
internal fun ResolvedDropEntry.unhandledTransformRateNotes(): List<String> =
    wikiNotes.transformRate.filterNot(::isHandledTransformRateNote)

internal fun ResolvedDropEntry.expandClueTertiaryExports(): List<ClueTertiaryRollExport> {
    val outOf = outOf ?: rollDenominator ?: return emptyList()
    val baseDenominator = exportClueDenominator(outOf)
    val wealth = ringOfWealthClueRate() ?: return listOf(ClueTertiaryRollExport(baseDenominator))
    return listOf(
        ClueTertiaryRollExport(
            denominator = baseDenominator,
            modifier = ClueTertiaryRollModifier(excludeRingOfWealth = true),
        ),
        ClueTertiaryRollExport(
            denominator = wealth.wealthDenominator,
            modifier =
                ClueTertiaryRollModifier(
                    requireRingOfWealth = true,
                    requireWilderness = wealth.requiresWilderness,
                ),
        ),
    )
}
