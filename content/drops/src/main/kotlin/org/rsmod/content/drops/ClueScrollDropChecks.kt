package org.rsmod.content.drops

import kotlin.math.floor
import org.rsmod.game.entity.Player

/** Easy Combat Achievements reward tier unlocked (improves clue drop rate). Stub defaults to true. */
public fun Player.hasUnlockedEasyCombat(): Boolean = true

public fun Player.hasUnlockedMediumCombat(): Boolean = true

public fun Player.hasUnlockedHardCombat(): Boolean = true

public fun Player.hasUnlockedEliteCombat(): Boolean = true

public fun Player.hasUnlockedMasterCombat(): Boolean = true

public fun combatAchievementClueDenominator(baseDenominator: Int): Int =
    floor(baseDenominator - baseDenominator * 0.05).toInt()

public fun Player.clueDropDenominator(baseDenominator: Int, boostPercent: Int = 0): Int {
    var denom = baseDenominator
    if (boostPercent > 0) {
        denom = floor(denom * 100.0 / (100.0 + boostPercent)).toInt().coerceAtLeast(1)
    }
    return denom
}

public fun Player.easyClueDropDenominator(baseDenominator: Int): Int {
    val base =
        if (hasUnlockedEasyCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator
    return clueDropDenominator(base)
}

public fun Player.mediumClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedMediumCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.hardClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedHardCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.eliteClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedEliteCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator

public fun Player.masterClueDropDenominator(baseDenominator: Int): Int =
    if (hasUnlockedMasterCombat()) combatAchievementClueDenominator(baseDenominator) else baseDenominator