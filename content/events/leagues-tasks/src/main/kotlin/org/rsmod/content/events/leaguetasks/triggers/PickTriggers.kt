package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerPickEvent
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PickTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByObj = progress.perLeague { it.rowsByObj(LeagueTaskTrigger.Pick) }

    override fun ScriptContext.startup() {
        onEvent<PlayerPickEvent> {
            val rows = rowsByObj[player]?.get(item.id) ?: return@onEvent
            progress.record(player, rows, count)
        }
    }
}
