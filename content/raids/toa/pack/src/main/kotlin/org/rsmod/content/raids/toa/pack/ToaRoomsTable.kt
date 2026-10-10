package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object ToaRoomsTable {

    const val ZONE_X = 0
    const val ZONE_Z = 1
    const val KIND = 2
    const val SPAWN = 3
    const val SPAWN_SPREAD_X = 4
    const val SPAWN_SPREAD_Z = 5
    const val CHALLENGE_SPAWN = 6
    const val CHALLENGE_MIN = 7
    const val CHALLENGE_MAX = 8
    const val OSMUMTEN_TILE = 9
    const val DROP_TILE = 10
    const val CHALLENGE_NAME = 11
    const val MIN_LEVEL = 12
    const val MAX_LEVEL = 13
    const val NEXT = 14

    const val EXCLUSION_ROOM = 0
    const val EXCLUSION_MIN = 1
    const val EXCLUSION_MAX = 2

    private class Challenge(
        val spawn: CoordGrid,
        val min: CoordGrid,
        val max: CoordGrid,
        val name: String,
    )

    fun rooms(): DBTable =
        dbTable("dbtable.toa_rooms", serverOnly = true) {
            column("zone_x", ZONE_X, VarType.INT)
            column("zone_z", ZONE_Z, VarType.INT)
            column("kind", KIND, VarType.STRING)
            column("spawn", SPAWN, VarType.COORDGRID)
            column("spawn_spread_x", SPAWN_SPREAD_X, VarType.INT)
            column("spawn_spread_z", SPAWN_SPREAD_Z, VarType.INT)
            column("challenge_spawn", CHALLENGE_SPAWN, VarType.COORDGRID)
            column("challenge_min", CHALLENGE_MIN, VarType.COORDGRID)
            column("challenge_max", CHALLENGE_MAX, VarType.COORDGRID)
            column("osmumten_tile", OSMUMTEN_TILE, VarType.COORDGRID)
            column("drop_tile", DROP_TILE, VarType.COORDGRID)
            column("challenge_name", CHALLENGE_NAME, VarType.STRING)
            column("min_level", MIN_LEVEL, VarType.INT)
            column("max_level", MAX_LEVEL, VarType.INT)
            column("next", NEXT, VarType.DBROW)

            fun room(
                row: String,
                zoneX: Int,
                zoneZ: Int,
                kind: String,
                spawn: CoordGrid,
                spreadX: Int = 0,
                spreadZ: Int = 0,
                challenge: Challenge? = null,
                osmumten: CoordGrid? = null,
                drop: CoordGrid? = null,
                maxLevel: Int = 3,
                next: String? = null,
            ) {
                row(row) {
                    column(ZONE_X, zoneX)
                    column(ZONE_Z, zoneZ)
                    column(KIND, kind)
                    columnCoord(SPAWN, spawn)
                    column(SPAWN_SPREAD_X, spreadX)
                    column(SPAWN_SPREAD_Z, spreadZ)
                    if (challenge != null) {
                        columnCoord(CHALLENGE_SPAWN, challenge.spawn)
                        columnCoord(CHALLENGE_MIN, challenge.min)
                        columnCoord(CHALLENGE_MAX, challenge.max)
                        column(CHALLENGE_NAME, challenge.name)
                    }
                    if (osmumten != null) columnCoord(OSMUMTEN_TILE, osmumten)
                    if (drop != null) columnCoord(DROP_TILE, drop)
                    column(MIN_LEVEL, 0)
                    column(MAX_LEVEL, maxLevel)
                    if (next != null) columnRSCM(NEXT, next)
                }
            }

            room(
                "dbrow.toa_room_main_hall",
                440,
                640,
                "MAIN_HALL",
                CoordGrid(3550, 5161, 0),
                spreadX = 2,
            )
            room(
                "dbrow.toa_room_reward",
                456,
                640,
                "REWARD",
                CoordGrid(3680, 5170, 0),
                osmumten = CoordGrid(3680, 5143, 0),
            )
            room(
                "dbrow.toa_room_wardens_first",
                472,
                640,
                "WARDENS",
                CoordGrid(3807, 5176, 1),
                spreadX = 2,
                challenge =
                    Challenge(
                        CoordGrid(3808, 5166, 1),
                        CoordGrid(3792, 5137, 1),
                        CoordGrid(3825, 5171, 1),
                        "The Wardens",
                    ),
                osmumten = CoordGrid(3808, 5158, 1),
                next = "dbrow.toa_room_wardens_second",
            )
            room(
                "dbrow.toa_room_wardens_second",
                488,
                640,
                "WARDENS",
                CoordGrid(3935, 5168, 1),
                spreadX = 2,
                challenge =
                    Challenge(
                        CoordGrid(3936, 5157, 1),
                        CoordGrid(3924, 5151, 1),
                        CoordGrid(3947, 5166, 1),
                        "The Wardens",
                    ),
                maxLevel = 1,
                next = "dbrow.toa_room_reward",
            )
            room(
                "dbrow.toa_room_scabaras_puzzle",
                440,
                656,
                "PUZZLE",
                CoordGrid(3523, 5279, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3575, 5280, 0),
                        CoordGrid(3533, 5268, 0),
                        CoordGrid(3574, 5292, 0),
                        "Path of Scabaras",
                    ),
                next = "dbrow.toa_room_scabaras_boss",
            )
            room(
                "dbrow.toa_room_het_puzzle",
                456,
                656,
                "PUZZLE",
                CoordGrid(3698, 5279, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3667, 5280, 0),
                        CoordGrid(3670, 5267, 0),
                        CoordGrid(3690, 5293, 0),
                        "Path of Het",
                    ),
                next = "dbrow.toa_room_het_boss",
            )
            room(
                "dbrow.toa_room_apmeken_puzzle",
                472,
                656,
                "PUZZLE",
                CoordGrid(3792, 5279, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3814, 5280, 0),
                        CoordGrid(3797, 5267, 0),
                        CoordGrid(3819, 5293, 0),
                        "Path of Apmeken",
                    ),
                next = "dbrow.toa_room_apmeken_boss",
            )
            room(
                "dbrow.toa_room_crondis_puzzle",
                488,
                656,
                "PUZZLE",
                CoordGrid(3954, 5279, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3943, 5280, 0),
                        CoordGrid(3923, 5250, 0),
                        CoordGrid(3949, 5311, 0),
                        "Path of Crondis",
                    ),
                next = "dbrow.toa_room_crondis_boss",
            )
            room(
                "dbrow.toa_room_scabaras_boss",
                440,
                672,
                "BOSS",
                CoordGrid(3535, 5408, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3544, 5408, 0),
                        CoordGrid(3543, 5400, 0),
                        CoordGrid(3559, 5416, 0),
                        "Kephri",
                    ),
                osmumten = CoordGrid(3558, 5408, 0),
                next = "dbrow.toa_room_main_hall",
            )
            room(
                "dbrow.toa_room_het_boss",
                456,
                672,
                "BOSS",
                CoordGrid(3698, 5406, 1),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3689, 5408, 1),
                        CoordGrid(3670, 5395, 1),
                        CoordGrid(3691, 5419, 1),
                        "Akkha",
                    ),
                osmumten = CoordGrid(3673, 5407, 1),
                next = "dbrow.toa_room_main_hall",
            )
            room(
                "dbrow.toa_room_apmeken_boss",
                472,
                672,
                "BOSS",
                CoordGrid(3790, 5407, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3800, 5408, 0),
                        CoordGrid(3796, 5399, 0),
                        CoordGrid(3823, 5418, 0),
                        "Ba-Ba",
                    ),
                osmumten = CoordGrid(3817, 5408, 0),
                next = "dbrow.toa_room_main_hall",
            )
            room(
                "dbrow.toa_room_crondis_boss",
                488,
                672,
                "BOSS",
                CoordGrid(3958, 5407, 0),
                spreadZ = 2,
                challenge =
                    Challenge(
                        CoordGrid(3941, 5408, 0),
                        CoordGrid(3904, 5387, 0),
                        CoordGrid(3962, 5429, 0),
                        "Zebak",
                    ),
                osmumten = CoordGrid(3928, 5408, 0),
                drop = CoordGrid(3927, 5408, 0),
                next = "dbrow.toa_room_main_hall",
            )
        }

    fun exclusions(): DBTable =
        dbTable("dbtable.toa_room_exclusions", serverOnly = true) {
            column("room", EXCLUSION_ROOM, VarType.DBROW)
            column("min", EXCLUSION_MIN, VarType.COORDGRID)
            column("max", EXCLUSION_MAX, VarType.COORDGRID)

            row("dbrow.toa_room_exclusion_zebak_a") {
                columnRSCM(EXCLUSION_ROOM, "dbrow.toa_room_crondis_boss")
                columnCoord(EXCLUSION_MIN, CoordGrid(3957, 5404, 0))
                columnCoord(EXCLUSION_MAX, CoordGrid(3960, 5414, 0))
            }
            row("dbrow.toa_room_exclusion_zebak_b") {
                columnRSCM(EXCLUSION_ROOM, "dbrow.toa_room_crondis_boss")
                columnCoord(EXCLUSION_MIN, CoordGrid(3952, 5406, 0))
                columnCoord(EXCLUSION_MAX, CoordGrid(3957, 5410, 0))
            }
        }
}
