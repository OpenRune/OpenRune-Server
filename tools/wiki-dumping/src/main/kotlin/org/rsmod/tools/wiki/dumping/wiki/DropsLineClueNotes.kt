package org.rsmod.tools.wiki.dumping.wiki

import kotlin.math.floor

/**
 * Mirrors [Module:DropsLineClue](https://oldschool.runescape.wiki/w/Module:DropsLineClue) footnotes
 * that are injected at template-render time and absent from raw `{{DropsLineClue}}` wikitext.
 */
object DropsLineClueNotes {
    private const val SCROLL_BOX_NOTE =
        "Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot."

    fun build(
        dropName: String,
        clueType: String,
        rarity: String,
        noteOverride: String? = null,
        rarityNotes: String? = null,
    ): WikiDropNotes {
        val notes = mutableListOf<String>()

        if (!noteOverride.isNullOrBlank()) {
            notes += WikiDropParser.cleanWikiNotes(noteOverride)
        } else {
            if (!clueType.equals("beginner", ignoreCase = true)) {
                combatAchievementRateNote(clueType, rarity)?.let(notes::add)
            }
            notes += SCROLL_BOX_NOTE
        }

        if (!rarityNotes.isNullOrBlank()) {
            notes += WikiDropParser.cleanWikiNotes(rarityNotes)
        }

        return WikiDropNoteClassifier.classifyAll(notes, dropName)
    }

    private fun combatAchievementRateNote(clueType: String, rarity: String): String? {
        val match = Regex("""([\d.]+)/([\d.]+)""").find(rarity.trim()) ?: return null
        val numerator = match.groupValues[1].toDoubleOrNull() ?: return null
        val denominator = match.groupValues[2].toDoubleOrNull() ?: return null
        if (numerator <= 0.0 || denominator <= 0.0) {
            return null
        }

        val adjusted =
            if (numerator > 1.0) {
                val reduced = denominator / numerator
                val adjustedDenom = floor(reduced - (reduced * 0.05)).toInt()
                "1/$adjustedDenom"
            } else {
                val adjustedDenom = floor(denominator - (denominator * 0.05)).toInt()
                "${numerator.toInt()}/$adjustedDenom"
            }

        return "The $clueType clue scroll drop rate increases to $adjusted after unlocking the $clueType Combat Achievements rewards tier."
    }
}
