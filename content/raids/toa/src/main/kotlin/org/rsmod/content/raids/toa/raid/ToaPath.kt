package org.rsmod.content.raids.toa.raid

import org.rsmod.game.loc.LocAngle
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

/**
 * The four paths, and their doors in the nexus (main hall).
 *
 * Source: Offline_Scape TOAPathType, corrected where it disagrees with the cache or capture:
 * - [hudPath]: `toa_client_current_path` is 1 Scabaras, 2 Het, 3 Apmeken, 4 Crondis (client
 *   script `toa_hud_draw`; capture: Scabaras sent 1). Offline_Scape used `ordinal + 1`.
 * - [levelVarbit]: the named varbits. Offline_Scape's `14376 + ordinal` wrote Apmeken's level
 *   into Crondis's varbit.
 * - Scabaras starts at its puzzle (capture), not straight at Kephri as in Offline_Scape.
 * - Door positions and angles are the capture's loc adds (angle 0 West, 1 North, 3 South).
 *
 * Each door has three variants: [doorOpen] (can be chosen), [doorUnselected] (the leader picked
 * another path) and [doorClosed] (completed, no ops).
 *
 * @property returnTile where you arrive in the nexus after beating this path's boss.
 */
enum class ToaPath(
    val pathName: String,
    val hudPath: Int,
    val levelVarbit: String,
    val door: CoordGrid,
    val doorAngle: LocAngle,
    val doorOpen: String,
    val doorUnselected: String,
    val doorClosed: String,
    val puzzle: ToaRoom,
    val boss: ToaRoom,
    val returnTile: CoordGrid,
    val returnSpreadZ: Int,
    val returnFacing: Direction,
) {
    SCABARAS(
        pathName = "Scabaras",
        hudPath = 1,
        levelVarbit = "varbit.toa_client_scabaras_level",
        door = CoordGrid(3559, 5155, 0),
        doorAngle = LocAngle.West,
        doorOpen = "loc.toa_nexus_scabaras_door",
        doorUnselected = "loc.toa_nexus_scabaras_door_unselected",
        doorClosed = "loc.toa_nexus_scabaras_door_closed",
        puzzle = ToaRoom.SCABARAS_PUZZLE,
        boss = ToaRoom.SCABARAS_BOSS,
        returnTile = CoordGrid(3558, 5154, 0),
        returnSpreadZ = 0,
        returnFacing = Direction.SouthWest,
    ),
    HET(
        pathName = "Het",
        hudPath = 2,
        levelVarbit = "varbit.toa_client_het_level",
        door = CoordGrid(3539, 5146, 0),
        doorAngle = LocAngle.South,
        doorOpen = "loc.toa_nexus_het_door",
        doorUnselected = "loc.toa_nexus_het_door_unselected",
        doorClosed = "loc.toa_nexus_het_door_closed",
        puzzle = ToaRoom.HET_PUZZLE,
        boss = ToaRoom.HET_BOSS,
        returnTile = CoordGrid(3541, 5146, 0),
        returnSpreadZ = 2,
        returnFacing = Direction.East,
    ),
    APMEKEN(
        pathName = "Apmeken",
        hudPath = 3,
        levelVarbit = "varbit.toa_client_apmeken_level",
        door = CoordGrid(3562, 5146, 0),
        doorAngle = LocAngle.North,
        doorOpen = "loc.toa_nexus_apmeken_door",
        doorUnselected = "loc.toa_nexus_apmeken_door_unselected",
        doorClosed = "loc.toa_nexus_apmeken_door_closed",
        puzzle = ToaRoom.APMEKEN_PUZZLE,
        boss = ToaRoom.APMEKEN_BOSS,
        returnTile = CoordGrid(3561, 5146, 0),
        returnSpreadZ = 2,
        returnFacing = Direction.West,
    ),
    CRONDIS(
        pathName = "Crondis",
        hudPath = 4,
        levelVarbit = "varbit.toa_client_crondis_level",
        door = CoordGrid(3541, 5155, 0),
        doorAngle = LocAngle.South,
        doorOpen = "loc.toa_nexus_crondis_door",
        doorUnselected = "loc.toa_nexus_crondis_door_unselected",
        doorClosed = "loc.toa_nexus_crondis_door_closed",
        puzzle = ToaRoom.CRONDIS_PUZZLE,
        boss = ToaRoom.CRONDIS_BOSS,
        returnTile = CoordGrid(3544, 5154, 0),
        returnSpreadZ = 0,
        returnFacing = Direction.SouthEast,
    );

    companion object {
        /** The path whose puzzle or boss is [room], or `null`. */
        fun of(room: ToaRoom): ToaPath? = entries.firstOrNull { it.puzzle == room || it.boss == room }
    }
}
