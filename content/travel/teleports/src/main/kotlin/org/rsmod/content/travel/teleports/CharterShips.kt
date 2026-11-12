package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Charter ships. The ports are the `charter` rows of `teleport_destination`, landing on the dock
 * rather than the ship's deck, since gangplanks are not usable yet.
 *
 * Every NPC form whose cache ops include `Charter` (the trader crews at each port, 168 forms) gets
 * that op bound here; dialogue scripts call [chooseCharter] for their own "charter a ship" line.
 */
object CharterShips {
    private const val NETWORK = "charter"
    private const val MENU_TITLE = "Where would you like to sail to?"
    private const val CANCEL = "Cancel"
    private const val NEARBY = 40
    private const val FADE_CYCLES = 50
    private const val VOYAGE_TICKS = 4

    private val ports: List<TeleportDestination> by lazy { TeleportNetworks.of(NETWORK) }

    suspend fun ProtectedAccess.chooseCharter() {
        val here =
            ports.indices.firstOrNull { ports[it].coords.chebyshevDistance(coords) <= NEARBY }
        val choices = ports.indices.filter { it != here }
        val labels = choices.map { ports[it].name } + CANCEL
        val picked = menu(MENU_TITLE, hotkeys = false, choices = labels)
        val port = choices.getOrNull(picked)?.let(ports::get) ?: return
        sail(port)
    }

    private suspend fun ProtectedAccess.sail(port: TeleportDestination) {
        ifClose()
        mes("You board the ship and sail to ${port.name}.")
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(1)
        minimapHideMap()
        delay(VOYAGE_TICKS)
        telejump(port.coords)
        delay(1)
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        minimapReset()
        closeFadeOverlay(cycles = 2)
    }

    /** Every npc type with a `Charter` op, paired with the op's slot (1-5). */
    fun charterCrews(): List<Pair<String, Int>> =
        ServerCacheManager.getNpcs().values.mapNotNull { type ->
            val slot = (1..5).firstOrNull { type.actions.getOpOrNull(it - 1) == "Charter" }
            slot?.let { RSCM.getReverseMapping(RSCMType.NPC, type.id) to it }
        }
}

class CharterShipScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((crew, slot) in CharterShips.charterCrews()) {
            when (slot) {
                1 -> onOpNpc1(crew) { charter() }
                2 -> onOpNpc2(crew) { charter() }
                3 -> onOpNpc3(crew) { charter() }
                4 -> onOpNpc4(crew) { charter() }
                5 -> onOpNpc5(crew) { charter() }
            }
        }
    }

    private suspend fun ProtectedAccess.charter() = with(CharterShips) { chooseCharter() }
}
