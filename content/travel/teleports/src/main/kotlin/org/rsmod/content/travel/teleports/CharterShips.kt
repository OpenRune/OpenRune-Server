package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.table.CharteringDestinationsRow
import org.rsmod.content.travel.teleports.TravelRequirements.checkReach
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Charter ships, on the live interfaces: `Charter` opens the sea map (`sailing_menu`) with its
 * port list (`chartering_menu_side`) beside it. Their client scripts draw one marker and one list
 * row per row of Jagex's `chartering_destinations`, hiding the player's own port and those the
 * player cannot reach yet, so a click arrives as `sailing_menu:content` sub 2k+1 or
 * `chartering_menu_side:list_content` sub 2k for row k. The voyage follows live captures: the map
 * closes, the sailing jingle plays and the screen fades, the minimap goes a tick later and the
 * player lands on the third tick, with the fade lifting on the fourth.
 *
 * Each port still needs its `charter` row of `teleport_destination`, which holds the quests it
 * needs and the dock to land on (the ship's deck is not used, since gangplanks are not usable
 * yet). `Charter-to` sails straight back to `varbit.chartering_previous_destination`.
 */
object CharterShips {
    private const val NETWORK = "charter"
    private const val MAP = "interface.sailing_menu"
    private const val SIDE = "interface.chartering_menu_side"
    const val MAP_MARKERS = "component.sailing_menu:content"
    const val SIDE_LIST = "component.chartering_menu_side:list_content"
    private const val SAILING_CONTENT = "varbit.content_restrict_sailing_serverside"
    private const val PREVIOUS = "varbit.chartering_previous_destination"
    private const val JINGLE = "jingle.sailing_journey_short"
    private const val FADE_CYCLES = 50
    private const val MINIMAP_TICKS = 1
    private const val LAND_TICKS = 2
    private const val CLOSE_TICKS = 2
    private const val OKAY = 1
    private const val AGAIN = 2
    private const val CANCEL = 3

    private val rows: List<CharteringDestinationsRow> by lazy { CharteringDestinationsRow.all() }
    private val ports: Map<String, TeleportDestination> by lazy {
        TeleportNetworks.of(NETWORK).associateBy { it.name }
    }

    fun ProtectedAccess.chooseCharter() {
        vars[SAILING_CONTENT] = 1
        ifOpenMainSidePair(MAP, SIDE)
        val slots = 0 until rows.size * 2
        ifSetEvents(MAP_MARKERS, slots, IfEvent.Op1)
        ifSetEvents(SIDE_LIST, slots, IfEvent.Op1)
    }

    /** Map markers are sub 2k+1 and list rows sub 2k for row k, so both halve to the row. */
    suspend fun ProtectedAccess.pickSlot(sub: Int) = pick(rows.getOrNull(sub / 2))

    suspend fun ProtectedAccess.charterToPrevious() {
        pick(rows.firstOrNull { it.charteringDestinationId == vars[PREVIOUS] })
    }

    private suspend fun ProtectedAccess.pick(row: CharteringDestinationsRow?) {
        ifClose()
        val port = row?.let { ports[it.charteringDestinationName] }
        if (row == null || port == null) {
            mes("The crew won't sail there.")
            return
        }
        if (isAt(row)) {
            mes("You are already at ${row.charteringDestinationName}.")
            return
        }
        if (!checkReach(port)) {
            return
        }
        val name = row.charteringDestinationInlineName ?: row.charteringDestinationName
        val choice =
            choice3("Okay", OKAY, "Choose again", AGAIN, "Cancel", CANCEL, title = "Sail to $name?")
        when (choice) {
            OKAY -> sail(row, port.coords, name)
            AGAIN -> chooseCharter()
        }
    }

    private fun ProtectedAccess.isAt(row: CharteringDestinationsRow): Boolean {
        val zone = row.charteringDestinationInzone
        if (zone.size != 2) {
            return false
        }
        val (low, high) = zone
        return coords.level == low.level && coords.x in low.x..high.x && coords.z in low.z..high.z
    }

    private suspend fun ProtectedAccess.sail(
        row: CharteringDestinationsRow,
        dock: CoordGrid,
        name: String,
    ) {
        if (actionDelay > mapClock) {
            return
        }
        actionDelay = mapClock + MINIMAP_TICKS + LAND_TICKS + CLOSE_TICKS
        vars[PREVIOUS] = row.charteringDestinationId
        midiJingle(JINGLE)
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(MINIMAP_TICKS)
        minimapHideMap()
        delay(LAND_TICKS)
        // UNVERIFIED: live says "You pay N coins and sail to X."; charters are free here
        mes("You sail to $name.")
        telejump(dock)
        delay(1)
        minimapReset()
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = CLOSE_TICKS)
    }

    /** Every npc type with an op matching [matches], paired with the op's slot (1-5). */
    fun crewsWithOp(matches: (String) -> Boolean): List<Pair<String, Int>> =
        ServerCacheManager.getNpcs().values.mapNotNull { type ->
            val slot =
                (1..5).firstOrNull { type.actions.getOpOrNull(it - 1)?.let(matches) == true }
            slot?.let { RSCM.getReverseMapping(RSCMType.NPC, type.id) to it }
        }
}

class CharterShipScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((crew, slot) in CharterShips.crewsWithOp { it == "Charter" }) {
            onNpcOp(crew, slot) { with(CharterShips) { chooseCharter() } }
        }
        for ((crew, slot) in CharterShips.crewsWithOp { it.startsWith("Charter-to") }) {
            onNpcOp(crew, slot) { with(CharterShips) { charterToPrevious() } }
        }
        onIfModalButton(CharterShips.MAP_MARKERS) { with(CharterShips) { pickSlot(it.comsub) } }
        onIfModalButton(CharterShips.SIDE_LIST) { with(CharterShips) { pickSlot(it.comsub) } }
    }

    private fun ScriptContext.onNpcOp(
        npc: String,
        slot: Int,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        when (slot) {
            1 -> onOpNpc1(npc) { action() }
            2 -> onOpNpc2(npc) { action() }
            3 -> onOpNpc3(npc) { action() }
            4 -> onOpNpc4(npc) { action() }
            5 -> onOpNpc5(npc) { action() }
        }
    }
}
