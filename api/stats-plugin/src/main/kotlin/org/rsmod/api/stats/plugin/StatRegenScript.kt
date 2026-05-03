package org.rsmod.api.stats.plugin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import org.rsmod.api.config.constants
import org.rsmod.api.player.hands
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
    private val regenStats by lazy { ServerCacheManager.getStats().values.toRegenStats() }

    override fun ScriptContext.startup() {
        onPlayerLogin { player.initRegenTimers() }

        onPlayerSoftTimer("timer.stat_regen") { player.statRegen() }
        onPlayerSoftTimer("timer.stat_boost_restore") { player.statBoostRestore() }
        onPlayerSoftTimer("timer.health_regen") { player.healthRegen() }

        onPlayerSoftTimer("timer.rapidrestore_regen") { player.statRegen() }
    }

    private fun Player.initRegenTimers() {
        softTimer("timer.stat_regen", constants.stat_regen_interval)
        softTimer("timer.stat_boost_restore", constants.stat_boost_restore_interval)
        softTimer("timer.health_regen", constants.health_regen_interval)
    }

    private fun Player.statRegen() {
        for (stat in regenStats) {
            val statInternal = RSCM.getReverseMapping(RSCMType.STAT,stat.id)

            val base = statBase(statInternal)
            val current = stat(statInternal)
            if (current < base) {
                statAdd(statInternal, constant = 1, percent = 0)
            }
        }
    }

    private fun Player.statBoostRestore() {
        for (stat in regenStats) {
            val statInternal = RSCM.getReverseMapping(RSCMType.STAT,stat.id)

            val base = statBase(statInternal)
            val current = stat(statInternal)
            if (current > base) {
                statSub(statInternal, constant = 1, percent = 0)
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

    private fun Collection<StatType>.toRegenStats(): List<StatType> {
        return filter { !it.isType("stat.prayer") && !it.isType("stat.hitpoints") }
    }
}
