package org.rsmod.api.stats.plugin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.config.constants
import org.rsmod.api.player.hands
import org.rsmod.api.player.stat.StatBoostDecayPrevention
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class StatRegenScript : PluginScript() {
    private val regenStats by lazy { statNames().filter(StatDecayRules::regenerates) }

    override fun ScriptContext.startup() {
        onPlayerLogin { player.initRegenTimers() }

        onPlayerSoftTimer("timer.stat_regen") { player.statRegen() }
        onPlayerSoftTimer("timer.stat_boost_restore") { player.decayBoostedStats() }
        onPlayerSoftTimer("timer.health_regen") { player.healthRegen() }

        onPlayerSoftTimer("timer.rapidrestore_regen") { player.statRegen() }
    }

    private fun Player.initRegenTimers() {
        softTimer("timer.stat_regen", constants.stat_regen_interval)
        softTimer("timer.stat_boost_restore", constants.stat_boost_restore_interval)
        softTimer("timer.health_regen", constants.health_regen_interval)
    }

    private fun Player.statRegen() {
        for (statInternal in regenStats) {
            val base = statBase(statInternal)
            val current = stat(statInternal)
            if (current < base) {
                statAdd(statInternal, constant = 1, percent = 0)
            }
        }
    }

    private fun Player.healthRegen() {
        if (hitpoints >= baseHitpointsLvl) {
            return
        }
        val amount = if (hands.isType("obj.jewl_bracelet_regen")) 2 else 1
        statHeal("stat.hitpoints", constant = amount, percent = 0)
    }
}

internal fun statNames(): List<String> =
    ServerCacheManager.getStats().values.map { RSCM.getReverseMapping(RSCMType.STAT, it.id) }

private val boostDecayStats by lazy { statNames().filter(StatDecayRules::boostDecays) }

internal fun Player.decayBoostedStats() {
    for (statInternal in boostDecayStats) {
        val base = statBase(statInternal)
        val current = stat(statInternal)
        if (current > base && !StatBoostDecayPrevention.prevents(player = this, stat = statInternal)) {
            statSub(statInternal, constant = 1, percent = 0)
        }
    }
}

internal object StatDecayRules {
    fun regenerates(stat: String): Boolean = stat != PRAYER && stat != HITPOINTS

    fun boostDecays(stat: String): Boolean = stat != PRAYER

    private const val PRAYER = "stat.prayer"
    private const val HITPOINTS = "stat.hitpoints"
}
