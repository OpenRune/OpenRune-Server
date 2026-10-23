package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object ToaPathsTable {

    const val NAME = 0
    const val HUD_PATH = 1
    const val LEVEL_VARBIT = 2
    const val DOOR = 3
    const val DOOR_ANGLE = 4
    const val DOOR_OPEN = 5
    const val DOOR_UNSELECTED = 6
    const val DOOR_CLOSED = 7
    const val PUZZLE_ROOM = 8
    const val BOSS_ROOM = 9
    const val RETURN_TILE = 10
    const val RETURN_SPREAD_Z = 11
    const val RETURN_FACING = 12
    const val PRELOAD_SEQS = 13

    private val CRONDIS_PRELOAD_SEQS =
        listOf(
            "seq.npc_zebak01_idle",
            "seq.npc_zebak02_idle",
            "seq.npc_zebak01_attack_melee",
            "seq.npc_zebak02_attack_melee",
            "seq.npc_zebak01_attack_melee_enraged",
            "seq.npc_zebak02_attack_melee_enraged",
            "seq.npc_zebak01_attack_ranged",
            "seq.npc_zebak02_attack_ranged",
            "seq.npc_zebak01_attack_ranged_enraged",
            "seq.npc_zebak02_attack_ranged_enraged",
            "seq.npc_zebak01_attack_roar",
            "seq.npc_zebak02_attack_roar",
            "seq.npc_zebak01_attack_tail",
            "seq.npc_zebak02_attack_tail",
            "seq.npc_zebak01_attack_special",
            "seq.npc_zebak02_attack_special",
            "seq.npc_zebak01_death",
            "seq.npc_zebak02_death",
            "seq.npc_zebak01_dead",
            "seq.npc_zebak02_dead",
            "seq.spotanim_zebak_magic01",
            "seq.projectile_zebak_ranged01",
            "seq.projectile_zebak_ranged01_enraged",
            "seq.spotanim_zebak_ranged01",
            "seq.projectile_zebak_pitcher01",
            "seq.projectile_zebak_pitcher01_enraged",
            "seq.spotanim_zebak_pitcher01",
            "seq.projectile_zebak_shield01",
            "seq.spotanim_zebak_shield01",
            "seq.npc_crondis_idle01",
            "seq.npc_crondis_walk01",
            "seq.npc_crondis_trapped01",
            "seq.vfx_pantheon_trapped01",
        )

    fun paths(): DBTable =
        dbTable("dbtable.toa_paths", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("hud_path", HUD_PATH, VarType.INT)
            column("level_varbit", LEVEL_VARBIT, VarType.INT)
            column("door", DOOR, VarType.COORDGRID)
            column("door_angle", DOOR_ANGLE, VarType.STRING)
            column("door_open", DOOR_OPEN, VarType.LOC)
            column("door_unselected", DOOR_UNSELECTED, VarType.LOC)
            column("door_closed", DOOR_CLOSED, VarType.LOC)
            column("puzzle_room", PUZZLE_ROOM, VarType.DBROW)
            column("boss_room", BOSS_ROOM, VarType.DBROW)
            column("return_tile", RETURN_TILE, VarType.COORDGRID)
            column("return_spread_z", RETURN_SPREAD_Z, VarType.INT)
            column("return_facing", RETURN_FACING, VarType.STRING)
            column("preload_seqs", PRELOAD_SEQS, VarType.SEQ)

            fun path(
                row: String,
                key: String,
                name: String,
                hudPath: Int,
                door: CoordGrid,
                doorAngle: String,
                returnTile: CoordGrid,
                returnSpreadZ: Int,
                returnFacing: String,
                preloadSeqs: List<String> = emptyList(),
            ) {
                row(row) {
                    column(NAME, name)
                    column(HUD_PATH, hudPath)
                    columnRSCM(LEVEL_VARBIT, "varbit.toa_client_${key}_level")
                    columnCoord(DOOR, door)
                    column(DOOR_ANGLE, doorAngle)
                    columnRSCM(DOOR_OPEN, "loc.toa_nexus_${key}_door")
                    columnRSCM(DOOR_UNSELECTED, "loc.toa_nexus_${key}_door_unselected")
                    columnRSCM(DOOR_CLOSED, "loc.toa_nexus_${key}_door_closed")
                    columnRSCM(PUZZLE_ROOM, "dbrow.toa_room_${key}_puzzle")
                    columnRSCM(BOSS_ROOM, "dbrow.toa_room_${key}_boss")
                    columnCoord(RETURN_TILE, returnTile)
                    column(RETURN_SPREAD_Z, returnSpreadZ)
                    column(RETURN_FACING, returnFacing)
                    if (preloadSeqs.isNotEmpty()) columnRSCM(PRELOAD_SEQS, preloadSeqs)
                }
            }

            path(
                "dbrow.toa_path_scabaras",
                "scabaras",
                "Scabaras",
                1,
                CoordGrid(3559, 5155, 0),
                "West",
                CoordGrid(3558, 5154, 0),
                0,
                "SouthWest",
            )
            path(
                "dbrow.toa_path_het",
                "het",
                "Het",
                2,
                CoordGrid(3539, 5146, 0),
                "South",
                CoordGrid(3541, 5146, 0),
                2,
                "East",
            )
            path(
                "dbrow.toa_path_apmeken",
                "apmeken",
                "Apmeken",
                3,
                CoordGrid(3562, 5146, 0),
                "North",
                CoordGrid(3561, 5146, 0),
                2,
                "West",
            )
            path(
                "dbrow.toa_path_crondis",
                "crondis",
                "Crondis",
                4,
                CoordGrid(3541, 5155, 0),
                "South",
                CoordGrid(3544, 5154, 0),
                0,
                "SouthEast",
                CRONDIS_PRELOAD_SEQS,
            )
        }
}
