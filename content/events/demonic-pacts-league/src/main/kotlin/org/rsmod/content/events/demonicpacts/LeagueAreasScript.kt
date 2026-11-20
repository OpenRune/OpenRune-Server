package org.rsmod.content.events.demonicpacts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenMain
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The game's League Tasks and League Areas windows, opened from the league side panel. */
class LeagueAreasScript
@Inject
constructor(private val eventBus: EventBus, private val teleports: ItemTeleportCaster) :
    PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override fun ScriptContext.startup() {
        onIfOverlayButton(SIDE_PANEL_TASKS_BUTTON) { player.openTasks() }
        onIfOverlayButton(SIDE_PANEL_AREAS_BUTTON) { player.openAreas() }

        onIfOpen(TASKS) { player.prepareTasks() }
        onIfOverlayButton(TASKS_CLOSE) { player.ifCloseSub(TASKS, eventBus) }

        onIfOpen(AREAS) { player.prepareAreas() }
        onIfModalButton(AREAS_CLOSE) { player.ifCloseModal(AREAS, eventBus) }
        for ((component, area) in AREA_BUTTONS) {
            onIfModalButton(component) { viewArea(area) }
        }
        onIfModalButton(AREAS_SELECT) { selectArea() }
        onIfModalButton(AREAS_CONFIRM) { confirmArea() }
    }

    /**
     * The tasks window is a draggable floating window that positions itself against the whole
     * screen, so it goes in the floater layer like the pact tree, not the main modal.
     */
    private fun Player.openTasks() {
        syncLeagueClientVars()
        ifOpenOverlay(TASKS, eventBus)
    }

    private fun Player.prepareTasks() {
        syncLeagueClientVars()
        ifSetEvents(TASKS_CLOSE, -1..-1, IfEvent.Op1)
    }

    private fun Player.openAreas() {
        syncLeagueClientVars()
        ifOpenMain(AREAS, eventBus)
    }

    private fun Player.prepareAreas() {
        syncLeagueClientVars()
        VarPlayerIntMapSetter.set(this, AREA_VIEWING, 0)
        ifSetEvents(AREAS_CLOSE, -1..-1, IfEvent.Op1)
        ifSetEvents(AREAS_SELECT, -1..-1, IfEvent.Op1)
        ifSetEvents(AREAS_CONFIRM, -1..-1, IfEvent.Op1)
        for (component in AREA_BUTTONS.keys) {
            ifSetEvents(component, -1..-1, IfEvent.Op1)
        }
    }

    private fun ProtectedAccess.viewArea(area: Int) {
        val (state, reason) = areaState(player, area)
        VarPlayerIntMapSetter.set(player, AREA_VIEWING, area)
        VarPlayerIntMapSetter.set(player, AREA_LAST_VIEWED, area)
        val details = DETAIL_COMPONENTS.map { it.asRSCM(RSCMType.COMPONENT) }
        val confirm = CONFIRM_COMPONENTS.map { it.asRSCM(RSCMType.COMPONENT) }
        val args = listOf<Any>(area) + details + listOf(state) + confirm + listOf(reason)
        player.runClientScript(SHOW_DETAILED, args)
    }

    private suspend fun ProtectedAccess.selectArea() {
        val area = player.vars[AREA_VIEWING]
        val region = DemonicPactsData.regions.firstOrNull { it.area == area } ?: return
        if (!player.hasRegion(region)) {
            return
        }
        val dest = region.teleport
        if (dest.size != 3) {
            mes("There is no teleport for ${region.name}.")
            return
        }
        player.ifCloseModal(AREAS, eventBus)
        with(teleports) { cast(TeleportType.Standard, CoordGrid(dest[0], dest[1], dest[2])) }
    }

    private fun ProtectedAccess.confirmArea() {
        val area = player.vars[AREA_VIEWING]
        val region = DemonicPactsData.regions.firstOrNull { it.area == area } ?: return
        val (state, reason) = areaState(player, area)
        if (state != STATE_UNLOCKABLE) {
            mes("<col=ff0000>${reason.ifEmpty { "You can't unlock that area right now." }}")
            return
        }
        player.unlockRegion(region)
        player.syncLeagueClientVars()
        mes("<col=ef1020>${region.name} is now unlocked!")
        LeagueTasks.checkProgress(player)
        player.openAreas()
    }

    /**
     * The client's area states: 0 needs more tasks, 1 unlocked, 2 can be unlocked now, 3 not
     * available (with a reason), 4 no unlocks left.
     */
    private fun areaState(player: Player, area: Int): Pair<Int, String> {
        val region =
            DemonicPactsData.regions.firstOrNull { it.area == area }
                ?: return STATE_UNAVAILABLE to "That area isn't part of this league."
        if (player.hasRegion(region)) {
            return STATE_UNLOCKED to ""
        }
        if (region.key == KARAMJA) {
            val needed = DemonicPactsData.settings.karamjaTasks
            return STATE_NEEDS_TASKS to "Karamja unlocks by itself at $needed tasks."
        }
        if (!region.unlockable) {
            return STATE_UNAVAILABLE to "${region.name} can't be unlocked in this league."
        }
        val chosen = player.chosenRegionCount()
        val thresholds = DemonicPactsData.settings.regionTaskThresholds
        if (chosen >= thresholds.size) {
            return STATE_NONE_LEFT to ""
        }
        if (chosen >= LeagueTasks.regionSlots(player)) {
            return STATE_NEEDS_TASKS to "You need ${thresholds[chosen]} tasks to unlock another area."
        }
        return STATE_UNLOCKABLE to ""
    }

    private companion object {
        const val TASKS = "interface.league_tasks"
        const val TASKS_CLOSE = "component.league_tasks:close_button"
        const val AREAS = "interface.trailblazer_areas"
        const val AREAS_CLOSE = "component.trailblazer_areas:close_button"
        const val AREAS_SELECT = "component.trailblazer_areas:select_button"
        const val AREAS_CONFIRM = "component.trailblazer_areas:confirm_button"
        const val SIDE_PANEL_TASKS_BUTTON = "component.league_side_panel:tasks_button"
        const val SIDE_PANEL_AREAS_BUTTON = "component.league_side_panel:areas_button"
        const val AREA_VIEWING = "varp.dp_area_viewing"
        const val AREA_LAST_VIEWED = "varbit.league_area_last_viewed"
        const val KARAMJA = "karamja"

        /** league_areas_show_detailed: draws one area's details page. */
        const val SHOW_DETAILED = 3668
        const val STATE_NEEDS_TASKS = 0
        const val STATE_UNLOCKED = 1
        const val STATE_UNLOCKABLE = 2
        const val STATE_UNAVAILABLE = 3
        const val STATE_NONE_LEFT = 4

        /** Each map shield and name, and the area id (region_data) it stands for. */
        val AREA_BUTTONS =
            mapOf(
                "component.trailblazer_areas:league_shield_misthalin" to 1,
                "component.trailblazer_areas:league_name_misthalin" to 1,
                "component.trailblazer_areas:league_shield_karamja" to 2,
                "component.trailblazer_areas:league_name_karamja" to 2,
                "component.trailblazer_areas:league_shield_asgarnia" to 3,
                "component.trailblazer_areas:league_name_asgarnia" to 3,
                "component.trailblazer_areas:league_shield_kandarin" to 4,
                "component.trailblazer_areas:league_name_kandarin" to 4,
                "component.trailblazer_areas:league_shield_morytania" to 5,
                "component.trailblazer_areas:league_name_morytania" to 5,
                "component.trailblazer_areas:league_shield_desert" to 6,
                "component.trailblazer_areas:league_name_desert" to 6,
                "component.trailblazer_areas:league_shield_tirannwn" to 7,
                "component.trailblazer_areas:league_name_tirannwn" to 7,
                "component.trailblazer_areas:league_shield_fremennik" to 8,
                "component.trailblazer_areas:league_name_fremennik" to 8,
                "component.trailblazer_areas:league_shield_wilderness" to 11,
                "component.trailblazer_areas:league_name_wilderness" to 11,
                "component.trailblazer_areas:league_shield_kebos_kourend" to 20,
                "component.trailblazer_areas:league_name_kebos_kourend" to 20,
                "component.trailblazer_areas:league_shield_varlamore" to 21,
                "component.trailblazer_areas:league_name_varlamore" to 21,
            )

        /** Arguments 2-11 of league_areas_show_detailed, in order. */
        val DETAIL_COMPONENTS =
            listOf(
                "component.trailblazer_areas:league_maps",
                "component.trailblazer_areas:league_names",
                "component.trailblazer_areas:league_shields",
                "component.trailblazer_areas:league_area_details",
                "component.trailblazer_areas:name_shield",
                "component.trailblazer_areas:name_header",
                "component.trailblazer_areas:description",
                "component.trailblazer_areas:select_button",
                "component.trailblazer_areas:select_back",
                "component.trailblazer_areas:league_area_icon",
            )

        /** Arguments 13-23 of league_areas_show_detailed, in order. */
        val CONFIRM_COMPONENTS =
            listOf(
                "component.trailblazer_areas:header",
                "component.trailblazer_areas:progress_bar",
                "component.trailblazer_areas:confirm",
                "component.trailblazer_areas:confirm_overlay",
                "component.trailblazer_areas:confirm_universe",
                "component.trailblazer_areas:confirm_frame",
                "component.trailblazer_areas:confirm_text",
                "component.trailblazer_areas:confirm_button",
                "component.trailblazer_areas:confirm_cancel",
                "component.trailblazer_areas:loading",
                "component.trailblazer_areas:league_highlight",
            )
    }
}
