package org.rsmod.content.events.leaguetasks

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.events.leaguetasks.tutorial.LeagueMenu
import org.rsmod.content.events.leaguetasks.tutorial.LeagueTutorial
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The league side panel in the journal tab and the task menu it opens. The list, its colours,
 * counts and search are drawn by the client from the cache; the server only keeps the filter and
 * search varbits, as in the Demonic Pacts captures.
 */
class LeagueMenuScript
@Inject
constructor(
    private val tasks: LeagueTasks,
    private val tutorial: LeagueTutorial,
    private val eventBus: EventBus,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onIfOpen("interface.league_side_panel") {
            tasks.updateProgressBar(player)
            tutorial.onSidePanelOpened(player)
        }

        onIfOverlayButton("component.league_side_panel:tasks_button") {
            if (!tutorial.onMenuButton(player, LeagueMenu.Tasks)) {
                player.ifOpenOverlay("interface.league_tasks", eventBus)
            }
        }

        onIfOpen("interface.league_tasks") {
            if (!tutorial.isActive(player)) {
                player.setTaskMenuEvents()
            }
        }

        for ((dropdown, filter) in FILTERS) {
            onIfOverlayButton(dropdown) {
                VarPlayerIntMapSetter.set(player, filter, (it.comsub - 1).coerceAtLeast(0))
            }
        }

        onIfOverlayButton("component.league_tasks:searchbar_image") { player.setSearching(true) }

        onIfOverlayButton("component.league_tasks:close_button") {
            player.ifCloseOverlay("interface.league_tasks", eventBus)
        }

        onIfClose("interface.league_tasks") { player.setSearching(false) }
    }

    private fun Player.setTaskMenuEvents() {
        ifSetEvents("component.league_tasks:dropdown_tier", 1..7, IfEvent.Op1)
        ifSetEvents("component.league_tasks:dropdown_completed", 1..4, IfEvent.Op1)
        ifSetEvents("component.league_tasks:dropdown_mastery", 1..4, IfEvent.Op1)
        ifSetEvents("component.league_tasks:dropdown_type", 1..8, IfEvent.Op1)
        ifSetEvents("component.league_tasks:dropdown_area", 1..15, IfEvent.Op1)
        ifSetEvents("component.league_tasks:dropdown_skill", 1..7, IfEvent.Op1)
        val lastTask = (tasks.leagueOf(this)?.size ?: 1) - 1
        ifSetEvents("component.league_tasks:tasks_background", 0..lastTask, IfEvent.Op1)
        ifSetEvents("component.league_tasks:league_menu_frame", 10..18, IfEvent.Op1)
    }

    private fun Player.setSearching(searching: Boolean) {
        val value = if (searching) 1 else 0
        VarPlayerIntMapSetter.set(this, "varbit.league_tasks_is_searching", value)
        VarPlayerIntMapSetter.set(this, "varbit.league_tasks_search_listen_for_keyboard", value)
    }

    private companion object {
        /** Each dropdown sets its filter to the chosen entry's index in the filter's enum. */
        val FILTERS =
            mapOf(
                "component.league_tasks:dropdown_tier" to "varbit.league_task_filter_tier",
                "component.league_tasks:dropdown_completed" to
                    "varbit.league_task_filter_completed",
                "component.league_tasks:dropdown_type" to "varbit.league_task_filter_type",
                "component.league_tasks:dropdown_area" to "varbit.league_task_filter_area",
                "component.league_tasks:dropdown_skill" to "varbit.league_task_filter_skill",
                "component.league_tasks:dropdown_mastery" to
                    "varbit.league_task_filter_mastery_point",
            )
    }
}
