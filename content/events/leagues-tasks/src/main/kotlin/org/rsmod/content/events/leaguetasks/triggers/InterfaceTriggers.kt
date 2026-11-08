package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.ui.PlayerInterfaceEvents
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class InterfaceTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByInterface: Map<Int, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Interface).groupBy { checkNotNull(it.interf) }

    override fun ScriptContext.startup() {
        onEvent<PlayerInterfaceEvents.Opened> {
            val rows = rowsByInterface[interf.id] ?: return@onEvent
            progress.record(player, rows)
        }
    }
}
