package org.rsmod.content.areas.city.falador.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Falador's straight staircases put the player down on the tile the *other* end of the flight is
 * approached from, so each model has one fixed offset that only needs rotating to match the
 * instance. [FaladorStairs] holds those offsets as measured on an unrotated instance.
 */
private enum class FaladorStairs(val loc: String, val dx: Int, val dz: Int, val dLevel: Int) {
    StairsUp("loc.fai_falador_stairs", 1, 3, 1),
    StairsDown("loc.fai_falador_stairstop", 1, -2, -1),
    CastleStairsUp("loc.fai_falador_castle_stairs", 1, 3, 1),
    CastleStairsDown("loc.fai_falador_castle_stairstop", 1, -2, -1),
    OutdoorStairsUp("loc.fai_falador_outdoorstairs_bottom", -1, 0, 1),
    OutdoorStairsDown("loc.fai_falador_outdoorstairs_top", 0, 3, -1),
}

/**
 * The party room stairwells are mirrored rather than rotated, so each one carries its own
 * destination instead of sharing a rotated offset.
 */
private enum class PartyRoomStairs(
    val loc: String,
    val up: CoordGrid?,
    val down: CoordGrid?,
) {
    WestBalcony(
        "loc.fai_falador_party_room_spiralstairs",
        up = CoordGrid(3039, 3383, 1),
        down = CoordGrid(3014, 9951, 0),
    ),
    EastBalcony(
        "loc.fai_falador_party_room_spiralstairs_mirror",
        up = CoordGrid(3052, 3383, 1),
        down = CoordGrid(3065, 9951, 0),
    ),
    WestBalconyDown(
        "loc.fai_falador_party_room_spiralstairs_top",
        up = null,
        down = CoordGrid(3037, 3382, 0),
    ),
    EastBalconyDown(
        "loc.fai_falador_party_room_spiralstairs_top_mirror",
        up = null,
        down = CoordGrid(3054, 3382, 0),
    ),
    WestUpper(
        "loc.fai_falador_party_room_spiralstairs_small",
        up = CoordGrid(3038, 3374, 2),
        down = null,
    ),
    EastUpper(
        "loc.fai_falador_party_room_spiralstairs_mirror_small",
        up = CoordGrid(3053, 3374, 2),
        down = null,
    ),
    WestUpperDown(
        "loc.fai_falador_party_room_spiralstairs_top_small",
        up = null,
        down = CoordGrid(3039, 3372, 1),
    ),
    EastUpperDown(
        "loc.fai_falador_party_room_spiralstairs_top_small_mirror",
        up = null,
        down = CoordGrid(3052, 3372, 1),
    ),
}

class FaladorStairScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stairs in FaladorStairs.entries) {
            onOpLoc1(stairs.loc) { climb(it.loc, stairs) }
        }

        for (stairs in PartyRoomStairs.entries) {
            val up = stairs.up
            val down = stairs.down
            if (up != null && down != null) {
                onOpLoc1(stairs.loc) { chooseDirection(up, down) }
                onOpLoc2(stairs.loc) { useStairs(up) }
                onOpLoc3(stairs.loc) { useStairs(down) }
            } else {
                val dest = up ?: down ?: continue
                onOpLoc1(stairs.loc) { useStairs(dest) }
            }
        }

        onOpLoc1("loc.osb5_passageway_leave") { leavePassageway(it.loc) }
    }

    private suspend fun ProtectedAccess.climb(loc: BoundLocInfo, stairs: FaladorStairs) {
        arriveDelay()
        telejump(loc.rotate(stairs.dx, stairs.dz).translateLevel(stairs.dLevel))
    }

    private suspend fun ProtectedAccess.useStairs(dest: CoordGrid) {
        arriveDelay()
        telejump(dest)
    }

    private suspend fun ProtectedAccess.chooseDirection(up: CoordGrid, down: CoordGrid) {
        arriveDelay()
        startDialogue {
            val dest = choice2("Climb up", up, "Climb down", down, title = "Select an option")
            telejump(dest)
        }
    }

    private suspend fun ProtectedAccess.leavePassageway(loc: BoundLocInfo) {
        arriveDelay()
        telejump(if (loc.coords.x < PASSAGEWAY_SPLIT_X) WEST_PASSAGEWAY_EXIT else EAST_PASSAGEWAY_EXIT)
    }

    private fun BoundLocInfo.rotate(dx: Int, dz: Int): CoordGrid =
        when (angle) {
            LocAngle.West -> coords.translate(dx, dz)
            LocAngle.North -> coords.translate(dz, width - 1 - dx)
            LocAngle.East -> coords.translate(width - 1 - dx, length - 1 - dz)
            LocAngle.South -> coords.translate(length - 1 - dz, dx)
        }

    private companion object {
        const val PASSAGEWAY_SPLIT_X = 3040
        val WEST_PASSAGEWAY_EXIT = CoordGrid(3038, 3382, 0)
        val EAST_PASSAGEWAY_EXIT = CoordGrid(3053, 3382, 0)
    }
}
