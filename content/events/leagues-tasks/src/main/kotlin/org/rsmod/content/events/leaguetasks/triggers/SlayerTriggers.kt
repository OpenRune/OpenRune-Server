package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerSlayerKillPrepare
import org.rsmod.api.script.onPlayerSlayerTaskComplete
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val taskRows = progress.rows(LeagueTaskTrigger.SlayerTask)
    private val killRows = progress.rows(LeagueTaskTrigger.SlayerTaskKill)

    override fun ScriptContext.startup() {
        onPlayerSlayerTaskComplete { progress.record(player, taskRows) }
        onPlayerSlayerKillPrepare {
            val types = setOf(npc.type.id, npc.visType.id)
            val met = killRows.filter { row -> row.npcs.isEmpty() || row.npcs.any { it in types } }
            progress.record(player, met)
        }
    }
}
