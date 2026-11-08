package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.vars.PlayerVarChangedEvent
import org.rsmod.api.player.vars.PlayerVarWatch
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Checks a var task whenever the varp holding its var is written. */
class VarTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByVarp: Map<Int, List<LeagueTaskRequirement>> =
        progress.rows(LeagueTaskTrigger.Var).groupBy { checkNotNull(it.baseVarp) }

    override fun ScriptContext.startup() {
        for (varp in rowsByVarp.keys) {
            PlayerVarWatch.watch(varp)
        }
        onEvent<PlayerVarChangedEvent> {
            for (row in rowsByVarp[varp].orEmpty()) {
                progress.check(player, row.task)
            }
        }
    }
}
