package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.vars.PlayerVarChangedEvent
import org.rsmod.api.player.vars.PlayerVarWatch
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Checks a var task whenever the varp holding its var is written. */
class VarTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByVarp =
        progress.perLeague { index ->
            index.rowsBy(LeagueTaskTrigger.Var) { listOfNotNull(it.baseVarp) }
                .also { rows -> rows.keys.forEach { PlayerVarWatch.watch(it) } }
        }

    override fun ScriptContext.startup() {
        onEvent<PlayerVarChangedEvent> {
            for (row in rowsByVarp[player]?.get(varp).orEmpty()) {
                progress.check(player, row.task)
            }
        }
    }
}
