package org.rsmod.api.player.stat

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.utils.skills.CombatLevel
import org.rsmod.game.entity.Player
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.stat.PlayerStatMap

public object PlayerSkillXP {
    public fun internalAddXP(
        player: Player,
        stat: String,
        xp: Double,
        rate: Double,
        source: XpSource = XpSource.Natural,
    ): Int = player.addXP(stat, xp, rate, source)

    private fun Player.addXP(stat: String, xp: Double, rate: Double, source: XpSource): Int {
        val fineXp = PlayerStatMap.toFineXP(xp * rate)
        if (fineXp.isInfinite()) {
            throw IllegalArgumentException("Total XP being added is too high! (xp=$xp, rate=$rate)")
        }
        val addedFineXp = statMap.addXP(stat, fineXp)
        if (addedFineXp == 0) {
            // UpdateStat packet is sent even if stat is maxed.
            updateStat(stat)
            return 0
        }
        checkLevelUp(stat, source)
        updateStat(stat)
        return PlayerStatMap.normalizeFineXP(addedFineXp)
    }

    private fun PlayerStatMap.addXP(stat: String, fineXp: Double): Int {
        val currXp = getFineXP(stat)
        val sumXp = min(PlayerStatMap.MAX_FINE_XP.toDouble(), currXp + fineXp).toInt()
        val addedXp = sumXp - currXp
        if (addedXp > 0) {
            setFineXP(stat, sumXp)
        }
        return addedXp
    }

    @OptIn(InternalApi::class)
    private fun Player.checkLevelUp(internal: String, source: XpSource) {
        val baseLevel = statBase(internal)

        val stat = ServerCacheManager.getStats(internal.asRSCM(RSCMType.STAT))
            ?: error("No stat found for $internal")

        if (baseLevel >= stat.maxLevel) {
            return
        }
        val nextLevelXp = PlayerSkillXPTable.getXPFromLevel(baseLevel + 1)
        val currentXp = statMap.getXP(internal)
        if (currentXp >= nextLevelXp) {
            val newLevel = min(stat.maxLevel, PlayerSkillXPTable.getLevelFromXP(currentXp))
            statMap.setBaseLevel(internal, newLevel.toByte())

            val setCurrLevel = stat(internal) == baseLevel
            if (setCurrLevel) {
                statMap.setCurrentLevel(internal, newLevel.toByte())
            }

            engineQueueChangeStat(stat)
            if (source == XpSource.Natural) {
                engineQueueAdvanceStat(stat, previousLevel = baseLevel)
            }
        }

        val combatLevel = calculateCombatLevel(this)
        if (combatLevel != this.combatLevel) {
            val advanced = combatLevel > this.combatLevel
            appearance.combatLevel = combatLevel
            // TODO: Should this update the entire combat tab or just the combat level vars?
            PlayerInterfaceUpdates.updateCombatLevel(this)
            if (advanced && source == XpSource.Natural) {
                engineQueueAdvanceCombat(combatLevel)
            }
        }
    }

    public fun calculateCombatLevel(player: Player): Int =
        CombatLevel.calculate(
            attack = player.baseAttackLvl,
            strength = player.baseStrengthLvl,
            defence = player.baseDefenceLvl,
            hitpoints = player.baseHitpointsLvl,
            ranged = player.baseRangedLvl,
            magic = player.baseMagicLvl,
            prayer = player.basePrayerLvl,
        )
}
