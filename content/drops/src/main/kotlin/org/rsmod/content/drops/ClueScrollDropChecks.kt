package org.rsmod.content.drops

import org.rsmod.game.entity.Player
import kotlin.math.floor

public const val X_MARKS_THE_SPOT_QUEST: String = "quest_xmarksthespot"

public enum class ClueScrollTier {
    Beginner,
    Easy,
    Medium,
    Hard,
    Elite,
    Master,
}

/** X Marks the Spot — clue scrolls become scroll boxes after completion. Stub defaults to false. */
public fun Player.hasCompletedXMarksTheSpot(): Boolean = hasCompletedQuest(X_MARKS_THE_SPOT_QUEST)

/** Easy Combat Achievements reward tier unlocked (improves clue drop rate). Stub defaults to true. */
public fun Player.hasUnlockedEasyCombat(): Boolean = true

public fun Player.hasUnlockedMediumCombat(): Boolean = true

public fun Player.hasUnlockedHardCombat(): Boolean = true

public fun Player.hasUnlockedEliteCombat(): Boolean = true

public fun Player.hasUnlockedMasterCombat(): Boolean = true

public fun combatAchievementClueDenominator(baseDenominator: Int): Int =
    floor(baseDenominator - baseDenominator * 0.05).toInt()

public fun Player.easyClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedEasyCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.mediumClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedMediumCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.hardClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedHardCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.eliteClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedEliteCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.masterClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedMasterCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun clueScrollTierForObj(obj: String): ClueScrollTier? {
    val normalized = obj.removePrefix("obj.").lowercase()
    return when {
        "beginner" in normalized -> ClueScrollTier.Beginner
        "easy" in normalized -> ClueScrollTier.Easy
        "medium" in normalized || normalized.startsWith("trail_medium") -> ClueScrollTier.Medium
        "hard" in normalized -> ClueScrollTier.Hard
        "elite" in normalized || normalized.startsWith("trail_elite") -> ClueScrollTier.Elite
        "master" in normalized -> ClueScrollTier.Master
        else -> null
    }
}

public fun clueScrollBoxObj(tier: ClueScrollTier): String = "obj.league_clue_box_${tier.name.lowercase()}"

/**
 * For [transformObj] on clue scroll drops: returns the scroll box obj when X Marks the Spot is
 * complete, otherwise null to keep the default scroll item.
 */
public fun Player.clueScrollTransformObj(scrollObj: String): String? {
    if (!hasCompletedXMarksTheSpot()) {
        return null
    }
    val tier = clueScrollTierForObj(scrollObj) ?: return null
    return clueScrollBoxObj(tier)
}
