package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerAreaEvents
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AreaTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByArea = progress.perLeague { it.rowsBy(LeagueTaskTrigger.Enter) { row -> row.areas } }

    override fun ScriptContext.startup() {
        onEvent<PlayerAreaEvents.Entered> {
            val rows = rowsByArea[player]?.get(area) ?: return@onEvent
            progress.record(player, rows)
        }
    }
}
