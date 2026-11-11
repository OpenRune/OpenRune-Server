package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.script.onNpcKilled
import org.rsmod.api.script.onPlayerSlayerKillPrepare
import org.rsmod.api.script.onPlayerSlayerTaskComplete
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SlayerTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val taskRows = progress.perLeague { it.rows(LeagueTaskTrigger.SlayerTaskComplete) }

    private val killRows = progress.perLeague { it.rows(LeagueTaskTrigger.SlayerKill) }

    private val superiorRows = progress.perLeague { it.rows(LeagueTaskTrigger.SuperiorKill) }

    override fun ScriptContext.startup() {
        onPlayerSlayerTaskComplete { progress.record(player, taskRows[player].orEmpty()) }

        onPlayerSlayerKillPrepare {
            val types = setOf(npc.type.id, npc.visType.id)
            val met = killRows[player].orEmpty().filter { row ->
                row.npcs.isEmpty() || row.npcs.any { it in types }
            }
            progress.record(player, met)
        }

        onNpcKilled {
            if (isHero && npc.visType.hasParam(BaseParams.slayer_superior_form)) {
                progress.record(player, superiorRows[player].orEmpty())
            }
        }
    }
}
