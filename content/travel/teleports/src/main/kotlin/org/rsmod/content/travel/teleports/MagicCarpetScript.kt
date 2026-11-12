package org.rsmod.content.travel.teleports

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.BasType
import kotlin.math.atan2
import kotlin.math.roundToInt
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.travel.teleports.RouteTravel.travelFromHub
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Magic carpets across the Kharidian Desert: the `carpet_*` networks of `teleport_destination`,
 * each hub first. The stop comes from the player's position, since the Sophanem and Menaphos
 * merchants turn into the Nardah merchant's form after Icthlarin's Little Helper.
 */
class MagicCarpetScript : PluginScript() {
    private val networks: List<List<TeleportDestination>> by lazy {
        NETWORKS.map(TeleportNetworks::of)
    }

    private val flyingBas: BasType by lazy {
        val flying = FLYING.asRSCM(RSCMType.SEQ)
        BasType(
            readyAnim = flying,
            turnOnSpot = flying,
            walkForward = flying,
            walkBack = flying,
            walkLeft = flying,
            walkRight = flying,
            running = flying,
        )
    }

    override fun ScriptContext.startup() {
        for (merchant in MERCHANTS) {
            onOpNpc3(merchant) { travel() }
        }
    }

    private suspend fun ProtectedAccess.travel() =
        travelFromHub(
            networks,
            MENU_TITLE,
            depart = { "You climb onto the magic carpet and fly to $it." },
            travel = { stop, message -> fly(stop.coords, message) },
        )

    /**
     * Rises on the carpet, glides in a straight line one hop per tick (over cliffs and water, as
     * the live ride does), then lands. The flying render set keeps the carpet under the player
     * between hops; it is cleared however the flight ends.
     */
    private suspend fun ProtectedAccess.fly(dest: CoordGrid, message: String) {
        if (actionDelay > mapClock) {
            return
        }
        ifClose()
        mes(message)
        anim(RAISE)
        delay(RAISE_TICKS)
        val start = coords
        val dx = dest.x - start.x
        val dz = dest.z - start.z
        val hops = maxOf(1, (start.chebyshevDistance(dest) + TILES_PER_HOP - 1) / TILES_PER_HOP)
        val facing = facing(dx, dz)
        try {
            player.bas = flyingBas
            anim(FLYING)
            for (hop in 1..hops) {
                val next =
                    if (hop == hops) dest
                    else
                        CoordGrid(
                            start.x + (dx * hop.toDouble() / hops).roundToInt(),
                            start.z + (dz * hop.toDouble() / hops).roundToInt(),
                            dest.level,
                        )
                exactMove(coords, next, 0, HOP_CYCLES, facing, TeleportType.Exempt)
                delay(1)
            }
        } finally {
            player.bas = null
        }
        anim(LAND)
        delay(LAND_TICKS)
        resetAnim()
    }

    /** Client facing: 0 south, 512 west, 1024 north, 1536 east. */
    private fun facing(dx: Int, dz: Int): Int {
        val angle = atan2(-dx.toDouble(), -dz.toDouble()) / (2 * Math.PI) * FULL_TURN
        return (angle.roundToInt() + FULL_TURN) % FULL_TURN
    }

    private companion object {
        private val NETWORKS = listOf("carpet_shantay", "carpet_pollnivneach")
        private const val MENU_TITLE = "Where would you like to travel to?"
        private val MERCHANTS = (1..8).map { "npc.magic_carpet_seller$it" }

        private const val RAISE = "seq.carpet_raise"
        private const val FLYING = "seq.carpet_flying"
        private const val LAND = "seq.carpet_land"
        private const val RAISE_TICKS = 4
        private const val LAND_TICKS = 4
        private const val TILES_PER_HOP = 3
        private const val HOP_CYCLES = 30
        private const val FULL_TURN = 2048
    }
}
