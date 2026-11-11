package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.ObservedAction
import org.rsmod.api.player.events.interact.ObservedTarget
import org.rsmod.api.player.events.interact.heldCount
import org.rsmod.api.script.onPlayerInteractionFinished
import org.rsmod.content.events.leaguetasks.LeagueTaskIndex
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Counts an npc or loc option, or an item used on one, once its handler has run to the end,
 * dialogue included. A use only counts when the handler took or changed the used item, so a refusal
 * does not.
 */
class InteractTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val options = progress.perLeague { TargetRows(it, LeagueTaskTrigger.Interact) }
    private val uses = progress.perLeague { TargetRows(it, LeagueTaskTrigger.UseOn) }

    override fun ScriptContext.startup() {
        onPlayerInteractionFinished {
            if (!completed) {
                return@onPlayerInteractionFinished
            }
            when (val action = started.action) {
                is ObservedAction.Option -> {
                    val targets = options[player] ?: return@onPlayerInteractionFinished
                    val rows =
                        targets.of(started.target).filter {
                            it.option == 0 || it.option == action.slot
                        }
                    progress.record(player, rows)
                }
                is ObservedAction.Use -> {
                    val item = action.item ?: return@onPlayerInteractionFinished
                    if (player.inv.heldCount(item, action.comsub) >= action.count) {
                        return@onPlayerInteractionFinished
                    }
                    val targets = uses[player] ?: return@onPlayerInteractionFinished
                    val rows =
                        targets.of(started.target).filter {
                            (it.objs.isEmpty() || item.id in it.objs) &&
                                (it.categories.isEmpty() || item.category in it.categories)
                        }
                    progress.record(player, rows)
                }
            }
        }
    }

    private class TargetRows(index: LeagueTaskIndex, trigger: LeagueTaskTrigger) {
        private val byNpc: Map<Int, List<LeagueTaskRequirement>> = index.rowsBy(trigger) { it.npcs }

        private val byLoc: Map<Int, List<LeagueTaskRequirement>> = index.rowsBy(trigger) { it.locs }

        fun of(target: ObservedTarget): List<LeagueTaskRequirement> =
            when (target) {
                is ObservedTarget.Npc -> byNpc[target.type.id]
                is ObservedTarget.Loc -> byLoc[target.type.id]
                else -> null
            }.orEmpty()
    }
}
