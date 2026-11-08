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

/**
 * Counts an npc or loc option, or an item used on one, once its handler has run to the end,
 * dialogue included.
 */
class InteractTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val options = TargetRows(LeagueTaskTrigger.Interact)
    private val uses = TargetRows(LeagueTaskTrigger.UseOn)

    override fun ScriptContext.startup() {
        onPlayerInteractionFinished {
            if (!completed) {
                return@onPlayerInteractionFinished
            }
            when (val action = started.action) {
                is ObservedAction.Option -> {
                    val rows =
                        options.of(started.target).filter {
                            it.option == 0 || it.option == action.slot
                        }
                    progress.record(player, rows)
                }
                is ObservedAction.Use -> {
                    val item = action.item ?: return@onPlayerInteractionFinished
                    val rows =
                        uses.of(started.target).filter { it.objs.isEmpty() || item.id in it.objs }
                    progress.record(player, rows)
                }
            }
        }
    }

    private inner class TargetRows(trigger: LeagueTaskTrigger) {
        private val rows = progress.rows(trigger)

        private val byNpc: Map<Int, List<LeagueTaskRequirement>> =
            rows.flatMap { row -> row.npcs.map { it to row } }.groupBy({ it.first }, { it.second })

        private val byLoc: Map<Int, List<LeagueTaskRequirement>> =
            rows.flatMap { row -> row.locs.map { it to row } }.groupBy({ it.first }, { it.second })

        fun of(target: ObservedTarget): List<LeagueTaskRequirement> =
            when (target) {
                is ObservedTarget.Npc -> byNpc[target.type.id]
                is ObservedTarget.Loc -> byLoc[target.type.id]
                else -> null
            }.orEmpty()
    }
}
