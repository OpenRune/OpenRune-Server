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
        onOpLoc3("loc.dagexp_bossroomladder_down") { peek(normalLair = true) }

        onOpLoc1("loc.dagexp_bossroomladder_up") { ascendLair() }

        onOpLoc1("loc.dk_peek_crack") { peek(normalLair = false) }

        onOpLoc1("loc.dk_peek_root") { stepOver(it.loc.coords) }
    }

    private suspend fun ProtectedAccess.descendLair(destination: CoordGrid) {
        arriveDelay()
        delay(1)
        telejump(destination)
    }

    private suspend fun ProtectedAccess.ascendLair() {
        arriveDelay()
        delay(1)
        telejump(LAIR_EXIT)
    }

    private fun ProtectedAccess.peek(normalLair: Boolean) {
        val zone = ZoneKey.from(CoordGrid(2900, 4449, 0))
        val count =
            playerRepo
                .findAll(zone, zoneRadius = 2)
                .count {
                    val z = it.coords.z
                    if (normalLair) z in NORMAL_LAIR_Z else z in SLAYER_LAIR_Z
                }
        when (count) {
            0 -> mes("You see nobody in the lair.")
            1 -> mes("You see one adventurer in the lair.")
            else -> mes("You see $count adventurers in the lair.")
        }
    }

    private suspend fun ProtectedAccess.stepOver(root: CoordGrid) {
        arriveDelay()
        val east = root.translateX(1)
        val west = root.translateX(-1)
        val destination = if (coords.x < root.x) east else west
        delay(1)
        telejump(destination)
    }

    private companion object {
        val LAIR_ENTRY = CoordGrid(2900, 4449, 0)
        val SLAYER_LAIR_ENTRY = CoordGrid(2900, 4385, 0)
        val LAIR_EXIT = CoordGrid(1910, 4367, 0)

        val NORMAL_LAIR_Z = 4416..4479
        val SLAYER_LAIR_Z = 4352..4415
    }
}
