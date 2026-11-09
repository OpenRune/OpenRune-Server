package org.rsmod.content.events.leaguetasks.regions

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.api.table.RegionDataRow
import org.rsmod.content.events.leaguetasks.LeagueTaskCompletedEvent
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.content.events.leaguetasks.tutorial.LeagueMenu
import org.rsmod.content.events.leaguetasks.tutorial.LeagueTutorial
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The area menu (`trailblazer_areas`) as captured in `rsprox-1098` (Karamja at 80 tasks) and
 * `rsprox-1102` (Tirannwn): the milestone announcement, the per-region view, "Teleport to" for an
 * unlocked region and the unlock itself. The map, shields and confirm panel are client-side; the
 * server only gets shield clicks, `select_button` and `confirm_button`.
 */
class LeagueAreasScript
@Inject
constructor(
    private val regions: LeagueRegions,
    private val tasks: LeagueTasks,
    private val homeTeleport: LeagueHomeTeleport,
    private val tutorial: LeagueTutorial,
    private val eventBus: EventBus,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.league_side_panel:areas_button") {
            if (!tutorial.onMenuButton(player, LeagueMenu.Areas)) {
                player.openAreas()
            }
        }

        onEvent<LeagueTaskCompletedEvent> { announceNewArea(player) }

        onPlayerSoftTimer("timer.league_area_notification") {
            player.clearSoftTimer("timer.league_area_notification")
            val league = tasks.leagueOf(player) ?: return@onPlayerSoftTimer
            ClientScripts.notificationDisplay(
                player,
                "New Area Available!",
                "You've completed enough tasks to unlock a new area!",
                league.notificationColour,
            )
            player.midiJingle("jingle.dt2_horn_part_6")
        }

        for (region in regions.selectable) {
            val shield = region.shieldComponent ?: continue
            onProtectedEvent<IfModalButton>(shield.packed) { viewRegion(region) }
        }

        onIfModalButton("component.trailblazer_areas:select_button") { teleportToViewed() }
        onIfModalButton("component.trailblazer_areas:confirm_button") { unlockViewed() }
    }

    private fun Player.openAreas() {
        ifOpenMainModal("interface.trailblazer_areas", eventBus, colour = -1, transparency = -3)
        ifSetEvents("component.trailblazer_areas:league_menu_frame", 10..18, IfEvent.Op1)
    }

    private fun announceNewArea(player: Player) {
        if (player.vars["varbit.league_area_available"] != 0) {
            return
        }
        if (regions.pendingSlot(player) == null) {
            return
        }
        VarPlayerIntMapSetter.set(player, "varbit.league_area_available", 1)
        player.mes("<col=229628>You've completed enough tasks to unlock a new area!</col>")
        player.softTimer("timer.league_area_notification", cycles = NEW_AREA_NOTIFICATION_DELAY)
    }

    private fun ProtectedAccess.viewRegion(region: RegionDataRow) {
        vars["varbit.league_area_last_viewed"] = region.regionId
        player.runClientScript(
            "clientscript.[clientscript,league_areas_show_detailed]".asRSCM(RSCMType.CLIENTSCRIPT),
            region.regionId,
            component("league_maps"),
            component("league_shields"),
            component("league_names"),
            component("league_area_details"),
            component("name_shield"),
            component("name_header"),
            component("description"),
            component("select_button"),
            component("select_back"),
            component("league_area_icon"),
            viewState(player, region.regionId),
            component("league_tab_info_scrollbar"),
            component("league_tab_info_content"),
            component("confirm"),
            component("confirm_overlay"),
            component("confirm_universe"),
            component("confirm_frame"),
            component("confirm_text"),
            component("confirm_button"),
            component("confirm_cancel"),
            component("loading"),
            component("header"),
            // UNVERIFIED: no capture of a state-3 view; empty falls back to the client's
            // "That area is unavailable."
            "",
        )
    }

    /** The select button's mode in `[proc,league_areas_show_detailed]`. */
    private fun viewState(player: Player, region: Int): Int {
        if (regions.isUnlocked(player, region)) {
            return VIEW_UNLOCKED
        }
        if (regions.isHidden(player, region)) {
            return VIEW_UNAVAILABLE
        }
        val slot =
            regions.pendingSlot(player)
                ?: return if (regions.allSlotsUsed(player)) VIEW_ALL_USED else VIEW_LOCKED
        val forced = regions.forcedRegion(player, slot)
        return if (forced != null && forced != region) VIEW_UNAVAILABLE else VIEW_AVAILABLE
    }

    private fun ProtectedAccess.teleportToViewed() {
        val region = regions.region(vars["varbit.league_area_last_viewed"]) ?: return
        if (!regions.isUnlocked(player, region.regionId)) {
            return
        }
        val dest = region.areaTeleportCoord ?: return
        player.ifCloseModal("interface.trailblazer_areas", eventBus)
        homeTeleport.start(this, dest, AREA_TELEPORT_RADIUS)
    }

    private suspend fun ProtectedAccess.unlockViewed() {
        val region = regions.region(vars["varbit.league_area_last_viewed"]) ?: return
        val league = tasks.leagueOf(player) ?: return
        if (viewState(player, region.regionId) != VIEW_AVAILABLE) {
            return
        }
        player.ifCloseModal("interface.trailblazer_areas", eventBus)
        vars[BUSY] = 1
        anim("seq.trailblazer_league_area_unlock_emote")
        spotanim(regions.unlockSpotanim(region.regionId))
        soundSynth("synth.trailblazer_unlock_map", delay = 24)
        soundSynth("synth.trailblazer_unlock_power", delay = 116)
        soundSynth("synth.trailblazer_unlock_raise", delay = 241)
        soundSynth("synth.trailblazer_unlock_twunkles", delay = 273)
        delay(UNLOCK_DELAY)

        val slot = regions.pendingSlot(player)
        if (slot == null || regions.isUnlocked(player, region.regionId)) {
            vars[BUSY] = 0
            return
        }
        regions.select(player, slot, region.regionId)
        // UNVERIFIED: the captures never show this reset; it re-arms the next milestone message.
        vars["varbit.league_area_available"] = 0
        // TODO: quests and diary tasks the area auto-completes (rsprox-1098 line 364661 onward).
        val name = region.name
        mes("Congratulations, you've unlocked a new area: ${league.messageColour}$name</col>.")
        tasks.updateProgressBar(player)
        player.openAreas()
        ClientScripts.notificationDisplay(
            player,
            "Area Unlocked!",
            "You have unlocked the <col=ffffff>$name</col> area.",
            league.notificationColour,
        )
        player.midiJingle("jingle.dt2_sucellus")
        vars[BUSY] = 0
    }

    private fun component(name: String): Int =
        "component.trailblazer_areas:$name".asRSCM(RSCMType.COMPONENT)

    private companion object {
        const val VIEW_LOCKED = 0
        const val VIEW_UNLOCKED = 1
        const val VIEW_AVAILABLE = 2
        const val VIEW_UNAVAILABLE = 3
        const val VIEW_ALL_USED = 4
        const val NEW_AREA_NOTIFICATION_DELAY = 13
        const val UNLOCK_DELAY = 14
        const val AREA_TELEPORT_RADIUS = 1
        const val BUSY = "varbit.busy"
    }
}
