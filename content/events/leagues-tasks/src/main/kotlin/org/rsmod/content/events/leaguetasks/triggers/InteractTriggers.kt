package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.ObservedAction
import org.rsmod.api.player.events.interact.ObservedTarget
import org.rsmod.api.script.onPlayerInteractionFinished
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Counts an npc or loc option once its handler has run to the end, dialogue included. */
class InteractTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rows = progress.rows(LeagueTaskTrigger.Interact)

    private val rowsByNpc: Map<Int, List<LeagueTaskRequirement>> =
        rows.flatMap { row -> row.npcs.map { it to row } }.groupBy({ it.first }, { it.second })

    private val rowsByLoc: Map<Int, List<LeagueTaskRequirement>> =
        rows.flatMap { row -> row.locs.map { it to row } }.groupBy({ it.first }, { it.second })

    override fun ScriptContext.startup() {
        onPlayerInteractionFinished {
            val option = (started.action as? ObservedAction.Option)?.slot
            if (!completed || option == null) {
                return@onPlayerInteractionFinished
            }
            val candidates =
                when (val target = started.target) {
                    is ObservedTarget.Npc -> rowsByNpc[target.type.id]
                    is ObservedTarget.Loc -> rowsByLoc[target.type.id]
                    else -> null
                } ?: return@onPlayerInteractionFinished
            progress.record(player, candidates.filter { it.option == 0 || it.option == option })
        }
    }
}
