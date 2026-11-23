package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DagannothKingsLair @Inject constructor(private val playerRepo: PlayerRepository) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.dagexp_bossroomladder_down") { descendLair(LAIR_ENTRY) }
        onOpLoc2("loc.dagexp_bossroomladder_down") { descendLair(SLAYER_LAIR_ENTRY) }
        onOpLoc3("loc.dagexp_bossroomladder_down") { peekLairs() }

        onOpLoc1("loc.dagexp_bossroomladder_up") { ascendLair() }

        onOpLoc1("loc.dk_peek_crack") { peekLairs() }

        onOpLoc1("loc.dk_peek_root") { stepOver(it.loc.coords) }
    }

    private suspend fun ProtectedAccess.descendLair(destination: CoordGrid) {
        arriveDelay()
        telejump(destination)
    }

    private suspend fun ProtectedAccess.ascendLair() {
        arriveDelay()
        telejump(LAIR_EXIT)
    }

    private fun ProtectedAccess.peekLairs() {
        mes("Standard cave: ${adventurers(countPlayers(PUBLIC_LAIR_CENTER, NORMAL_LAIR_Z))}")
        mes("Slayer cave: ${adventurers(countPlayers(SLAYER_LAIR_CENTER, SLAYER_LAIR_Z))}")
    }

    private fun countPlayers(center: CoordGrid, zRange: IntRange): Int {
        val zone = ZoneKey.from(center)
        return playerRepo
            .findAll(zone, zoneRadius = 5)
            .count { it.coords.x in LAIR_X && it.coords.z in zRange }
    }

    private fun adventurers(count: Int): String =
        when (count) {
            0 -> "No adventurers."
            1 -> "1 adventurer."
            else -> "$count adventurers."
        }

    private suspend fun ProtectedAccess.stepOver(root: CoordGrid) {
        arriveDelay()
        val destination =
            if (coords.z >= root.z) root.translateZ(-1) else root.translateZ(1)
        anim("seq.human_walk_style")
        delay(2)
        telejump(destination)
        resetAnim()
    }

    private companion object {
        val LAIR_ENTRY = CoordGrid(2900, 4449, 0)
        val SLAYER_LAIR_ENTRY = CoordGrid(2900, 4385, 0)

        // No capture covers the Climb-up ladders; the landing mirrors the entrance side.
        val LAIR_EXIT = CoordGrid(1910, 4367, 0)

        val PUBLIC_LAIR_CENTER = CoordGrid(2900, 4448, 0)
        val SLAYER_LAIR_CENTER = CoordGrid(2900, 4384, 0)

        val LAIR_X = 2880..2943
        val NORMAL_LAIR_Z = 4416..4479
        val SLAYER_LAIR_Z = 4352..4415
    }
}
