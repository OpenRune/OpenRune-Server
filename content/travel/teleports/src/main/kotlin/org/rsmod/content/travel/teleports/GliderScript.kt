package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The gnome glider network. Its stops are the `glider` rows of `teleport_destination`, numbered
 * the way the cache numbers them: `varp.pilot_journey` holds the trip as a coord whose x is the
 * origin and z the destination (0 written as 9, 6 as 8), which `glidermap_fly` reads to animate
 * the glider across the map, and `varbit.pilot_previous_destination` picks which form each pilot
 * shows.
 *
 * Every pilot form with a `Glider` op opens the map. The Feldip Hills and Ape Atoll buttons are
 * unhidden on open, since their quests are not checked here (see docs/quirks.md).
 */
class GliderScript : PluginScript() {
    private val stops: List<TeleportDestination> by lazy { TeleportNetworks.of(NETWORK) }

    private var Player.previousDestination by intVarBit(PREVIOUS)
    private var Player.questPilotForm by intVarBit("varbit.pilot_multinpc_var")

    override fun ScriptContext.startup() {
        onPlayerLogin { player.showQuestPilots() }
        for (pilot in PILOTS) {
            onOpNpc1(pilot) { openMap() }
            onOpNpc4(pilot) { flyAgain() }
        }
        onOpNpc3(DIGSITE_PILOT) { openMap() }
        for ((index, button) in BUTTONS.withIndex()) {
            onIfModalButton("component.glidermap:$button") { fly(index) }
        }
    }

    private suspend fun ProtectedAccess.openMap() {
        vars[JOURNEY] = NO_JOURNEY
        ifOpenMainModal(MAP)
        // The map's onload script hides these after any same-tick change, so unhide them a tick on.
        delay(1)
        ifSetHide("component.glidermap:ogrearea_container", false)
        ifSetHide("component.glidermap:apeatoll_container", false)
    }

    /** `Glider-to`: the pilot forms named after a stop fly straight back to it. */
    private suspend fun ProtectedAccess.flyAgain() {
        vars[JOURNEY] = NO_JOURNEY
        ifOpenMainModal(MAP)
        fly(player.previousDestination)
    }

    private suspend fun ProtectedAccess.fly(destination: Int) {
        val origin = stops.indices.minByOrNull { stops[it].coords.chebyshevDistance(coords) }
        if (origin == null || stops[origin].coords.chebyshevDistance(coords) > NEARBY) {
            ifClose()
            return
        }
        if (origin == destination) {
            mes("You are already at ${stops[destination].name}.")
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        actionDelay = mapClock + FLIGHT_TICKS + FADE_TICKS
        vars[JOURNEY] = CoordGrid(journeyIndex(origin), journeyIndex(destination), 0).packed
        delay(FLIGHT_TICKS)
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(FADE_TICKS)
        ifClose()
        vars[JOURNEY] = NO_JOURNEY
        player.previousDestination = destination
        player.showQuestPilots()
        telejump(stops[destination].coords)
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = FADE_TICKS)
        mes("You arrive at ${stops[destination].name}.")
    }

    /**
     * The Feldip Hills and Ape Atoll pilots only take their `Glider` forms once this var is at
     * least [QUEST_PILOT_BASE], where it counts up from there by the last destination.
     */
    private fun Player.showQuestPilots() {
        questPilotForm = QUEST_PILOT_BASE + previousDestination
    }

    private fun journeyIndex(stop: Int): Int =
        when (stop) {
            0 -> 9
            6 -> 8
            else -> stop
        }

    private companion object {
        private const val NETWORK = "glider"
        private const val MAP = "interface.glidermap"
        private const val JOURNEY = "varp.pilot_journey"
        private const val PREVIOUS = "varbit.pilot_previous_destination"
        private const val NO_JOURNEY = -1
        private const val QUEST_PILOT_BASE = 16
        private const val DIGSITE_PILOT = "npc.pilot_digsite"
        private const val NEARBY = 20
        private const val FLIGHT_TICKS = 8
        private const val FADE_TICKS = 2
        private const val FADE_CYCLES = 50

        /** The map's buttons, in the same order as the network's stops. */
        private val BUTTONS =
            listOf(
                "grandtree_button",
                "karamja_button",
                "alkharid_button",
                "varrock_button",
                "whitewolfmountain_button",
                "ogrearea_button",
                "apeatoll_button",
            )

        /** Every pilot form that has a `Glider` op. */
        private val PILOTS: List<String> =
            listOf(
                "npc.pilot_apeatoll_model",
                "npc.gnormadium_avlafrim_glider",
                "npc.gnormadium_avlafrim_grandtree",
                "npc.gnormadium_avlafrim_karamja",
                "npc.gnormadium_avlafrim_al_kharid",
                "npc.gnormadium_avlafrim_varrock",
                "npc.gnormadium_avlafrim_whitewolf",
                "npc.gnormadium_avlafrim_ape",
                "npc.pilot_al_kharid_base",
                "npc.pilot_al_kharid_grandtree",
                "npc.pilot_al_kharid_karamja",
                "npc.pilot_al_kharid_varrock",
                "npc.pilot_al_kharid_whitewolf",
                "npc.pilot_al_kharid_ogre",
                "npc.pilot_al_kharid_ape",
                "npc.pilot_white_wolf_base",
                "npc.pilot_white_wolf_grandtree",
                "npc.pilot_white_wolf_karamja",
                "npc.pilot_white_wolf_al_kharid",
                "npc.pilot_white_wolf_varrock",
                "npc.pilot_white_wolf_ogre",
                "npc.pilot_white_wolf_ape",
                "npc.pilot_grand_tree_base",
                "npc.pilot_grand_tree_karamja",
                "npc.pilot_grand_tree_al_kharid",
                "npc.pilot_grand_tree_varrock",
                "npc.pilot_grand_tree_whitewolf",
                "npc.pilot_grand_tree_ogre",
                "npc.pilot_grand_tree_ape",
                "npc.pilot_karamja_base",
                "npc.pilot_karamja_grandtree",
                "npc.pilot_karamja_al_kharid",
                "npc.pilot_karamja_varrock",
                "npc.pilot_karamja_whitewolf",
                "npc.pilot_karamja_ogre",
                "npc.pilot_karamja_ape",
                "npc.pilot_apeatoll_grandtree",
                "npc.pilot_apeatoll_karamja",
                "npc.pilot_apeatoll_al_kharid",
                "npc.pilot_apeatoll_varrock",
                "npc.pilot_apeatoll_whitewolf",
                "npc.pilot_apeatoll_ogre",
            )
    }
}
