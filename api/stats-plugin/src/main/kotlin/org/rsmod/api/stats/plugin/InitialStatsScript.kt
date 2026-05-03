package org.rsmod.api.stats.plugin

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onPlayerInit
import org.rsmod.game.entity.Player
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InitialStatsScript() : PluginScript() {
    private val hitpointsStartLvl by lazy { ServerCacheManager.getStats("stat.hitpoints".asRSCM(RSCMType.STAT),)!!.minLevel }
    private val hitpointsStartFineXp by lazy { getFineXp(hitpointsStartLvl) }

    private val Player.newAccount by boolVarBit("varbit.new_player_account")

    override fun ScriptContext.startup() {
        onPlayerInit { player.setInitialStats() }
    }

    private fun Player.setInitialStats() {
        if (!newAccount) {
            return
        }
        if (baseHitpointsLvl < hitpointsStartLvl) {
            statMap.setFineXP("stat.hitpoints", hitpointsStartFineXp)
            statMap.setCurrentLevel("stat.hitpoints", hitpointsStartLvl.toByte())
            statMap.setBaseLevel("stat.hitpoints", hitpointsStartLvl.toByte())
            appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
        }
    }

    private fun getFineXp(level: Int): Int = PlayerSkillXPTable.getFineXPFromLevel(level)
}
