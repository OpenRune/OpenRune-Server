package org.rsmod.content.bosses.moons.neypotzli

import jakarta.inject.Inject
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.hook.NeypotzliPassage
import org.rsmod.api.player.hook.NeypotzliPassageKind
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NeypotzliPassages
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>) : PluginScript() {

    override fun ScriptContext.startup() {
        for (loc in PASSAGE_LOCS) {
            onOpLoc1(loc) { passThrough(it.loc.coords) }
        }
    }

    private suspend fun ProtectedAccess.passThrough(clicked: CoordGrid) {
        val origin = hooks.fold(clicked) { coords, hook -> hook.normalCoords(player, coords) }
        val passage = PASSAGES[origin.x to origin.z] ?: return
        for (hook in hooks) {
            if (!with(hook) { mayPass(passage) }) return
        }
        val tunnel = passage.kind in TUNNEL_KINDS
        if (tunnel) {
            fadeOverlay(0, 255, 0, 0, FADE_DURATION)
            delay(FADE_TICKS)
            spam("You follow a long and winding tunnel before emerging on the other side.")
        } else {
            arriveDelay()
        }
        var moved = false
        for (hook in hooks) {
            if (with(hook) { arrive(passage) }) {
                moved = true
                break
            }
        }
        if (!moved) telejump(passage.destination, TeleportType.Exempt)
        if (tunnel) {
            fadeOverlay(0, 0, 0, 255, FADE_DURATION)
            delay(FADE_TICKS)
            closeFadeOverlayNow()
        }
    }

    companion object {
        private const val FADE_DURATION = 50
        private const val FADE_TICKS = 2

        private val TUNNEL_KINDS =
            setOf(
                NeypotzliPassageKind.Cavern,
                NeypotzliPassageKind.MoonChamber,
                NeypotzliPassageKind.Vault,
            )

        private val PASSAGE_LOCS =
            listOf(
                "loc.pmoon_telebox",
                "loc.pmoon_telebox_3x3",
                "loc.pmoon_telebox_diagonal",
                "loc.pmoon_telebox_cave",
            )

        // UNVERIFIED: the Vault pair and the return side of each one-way capture are inferred
        // from the layout; only the outward trips have been seen.
        private val ROUTES =
            listOf(
                route(1435, 3129, CoordGrid(1439, 9509, 1), NeypotzliPassageKind.CityGate),
                route(1439, 9507, CoordGrid(1435, 3128, 0), NeypotzliPassageKind.CityExit),
                route(1439, 9600, CoordGrid(1440, 9615, 1), NeypotzliPassageKind.RuinsEntrance),
                route(1439, 9612, CoordGrid(1439, 9599, 1), NeypotzliPassageKind.RuinsExit),
                route(1421, 9613, CoordGrid(1388, 9575, 0), NeypotzliPassageKind.Cavern),
                route(1388, 9576, CoordGrid(1423, 9615, 1), NeypotzliPassageKind.Cavern),
                route(1421, 9650, CoordGrid(1403, 9717, 0), NeypotzliPassageKind.Cavern),
                route(1404, 9716, CoordGrid(1423, 9649, 1), NeypotzliPassageKind.Cavern),
                route(1458, 9650, CoordGrid(1481, 9669, 0), NeypotzliPassageKind.Cavern),
                route(1480, 9667, CoordGrid(1457, 9649, 1), NeypotzliPassageKind.Cavern),
                route(1521, 9720, CoordGrid(1390, 9676, 0), NeypotzliPassageKind.Cavern),
                route(1389, 9674, CoordGrid(1522, 9719, 0), NeypotzliPassageKind.Cavern),
                route(1345, 9590, CoordGrid(1374, 9667, 0), NeypotzliPassageKind.Cavern),
                route(1373, 9664, CoordGrid(1347, 9590, 0), NeypotzliPassageKind.Cavern),
                route(1527, 9670, CoordGrid(1513, 9596, 0), NeypotzliPassageKind.Cavern),
                route(1512, 9597, CoordGrid(1526, 9672, 0), NeypotzliPassageKind.Cavern),
                route(1512, 9560, CoordGrid(1355, 9538, 0), NeypotzliPassageKind.Cavern),
                route(1354, 9536, CoordGrid(1512, 9563, 0), NeypotzliPassageKind.Cavern),
                route(1356, 9536, CoordGrid(1512, 9563, 0), NeypotzliPassageKind.Cavern),
                route(1388, 9589, CoordGrid(1418, 9632, 0), NeypotzliPassageKind.MoonChamber),
                route(1388, 9591, CoordGrid(1418, 9632, 0), NeypotzliPassageKind.MoonChamber),
                route(1419, 9631, CoordGrid(1387, 9589, 0), NeypotzliPassageKind.Cavern),
                route(1404, 9703, CoordGrid(1440, 9653, 0), NeypotzliPassageKind.MoonChamber),
                route(1439, 9650, CoordGrid(1403, 9705, 0), NeypotzliPassageKind.Cavern),
                route(1509, 9673, CoordGrid(1461, 9632, 0), NeypotzliPassageKind.MoonChamber),
                route(1458, 9631, CoordGrid(1510, 9675, 0), NeypotzliPassageKind.Cavern),
                route(1458, 9613, CoordGrid(1496, 9580, 0), NeypotzliPassageKind.Vault),
                route(1493, 9579, CoordGrid(1457, 9615, 1), NeypotzliPassageKind.Vault),
            )

        val PASSAGES: Map<Pair<Int, Int>, NeypotzliPassage> =
            ROUTES.associateBy { it.origin.x to it.origin.z }

        private fun route(x: Int, z: Int, destination: CoordGrid, kind: NeypotzliPassageKind) =
            NeypotzliPassage(CoordGrid(x, z), destination, kind)
    }
}
