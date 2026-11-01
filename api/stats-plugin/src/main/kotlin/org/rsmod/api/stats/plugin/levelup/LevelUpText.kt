package org.rsmod.api.stats.plugin.levelup

internal object LevelUpText {
    fun title(label: String): String =
        "Congratulations, you've just advanced ${article(label)} $label level."

    fun level(prefix: String, level: Int): String = "$prefix $level."

    fun message(label: String, level: Int, maxed: Boolean): String =
        if (maxed) {
            "Congratulations, you've reached the highest possible $label level of $level."
        } else {
            "Congratulations, you've just advanced your $label level. You are now level $level."
        }

    fun guideUnlocks(guide: Int, label: String): String =
        "$guide|$GUIDE to see what your new $label level unlocks!"

    fun guideMoreUnlocks(guide: Int, label: String): String =
        "$guide|$GUIDE to see what else your new $label level unlocks!"

    fun guideInformation(guide: Int, label: String): String =
        "$guide|$GUIDE for more information about $label."

    private fun article(word: String): String =
        if (word.first().lowercaseChar() in VOWELS) "an" else "a"

    private const val VOWELS = "aeiou"
    private const val GUIDE = "Check the <col=ef1020>skill guide</col>"
}
