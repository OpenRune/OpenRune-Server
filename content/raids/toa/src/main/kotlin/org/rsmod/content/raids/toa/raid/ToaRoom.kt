package org.rsmod.content.raids.toa.raid

import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid

/**
 * The raid's rooms and their static data. Each room is one map square (8x8 zones) of the static
 * map, copied on all 4 levels into its own small instance region.
 *
 * Source: Offline_Scape EncounterType (zone coords, spawns, challenge areas, Osmumten tile).
 * Deviations from it are marked "Capture:".
 *
 * All coordinates here are STATIC. Convert with [ToaEncounter.coords] before using them in an
 * instance.
 *
 * @property spawn where players arrive, randomised by up to [spawnSpreadX] / [spawnSpreadZ].
 * @property challengeSpawn where the teleport crystal puts you (inside the challenge area).
 * @property challengeMin south-west corner of the challenge area (x/z only, inclusive).
 * @property challengeMax north-east corner of the challenge area (x/z only, inclusive).
 * @property osmumtenTile where Osmumten appears when a boss is beaten.
 * @property challengeName used in "Challenge started/complete: ..." messages.
 */
enum class ToaRoom(
    val zoneX: Int,
    val zoneZ: Int,
    val kind: Kind,
    val spawn: CoordGrid,
    val spawnSpreadX: Int = 0,
    val spawnSpreadZ: Int = 0,
    val challengeSpawn: CoordGrid? = null,
    val challengeMin: CoordGrid? = null,
    val challengeMax: CoordGrid? = null,
    val osmumtenTile: CoordGrid? = null,
    val challengeName: String? = null,
) {
    MAIN_HALL(440, 640, Kind.MAIN_HALL, CoordGrid(3550, 5161, 0), spawnSpreadX = 2),

    REWARD_ROOM(
        456, 640, Kind.REWARD,
        spawn = CoordGrid(3680, 5170, 0),
        osmumtenTile = CoordGrid(3680, 5143, 0),
    ),

    WARDENS_FIRST_ROOM(
        472, 640, Kind.WARDENS,
        spawn = CoordGrid(3807, 5176, 1), spawnSpreadX = 2,
        challengeSpawn = CoordGrid(3808, 5166, 1),
        challengeMin = CoordGrid(3792, 5137, 1),
        challengeMax = CoordGrid(3825, 5171, 1),
        osmumtenTile = CoordGrid(3808, 5158, 1),
        challengeName = "The Wardens",
    ),

    WARDENS_SECOND_ROOM(
        488, 640, Kind.WARDENS,
        spawn = CoordGrid(3935, 5168, 1), spawnSpreadX = 2,
        challengeSpawn = CoordGrid(3936, 5157, 1),
        challengeMin = CoordGrid(3924, 5151, 1),
        challengeMax = CoordGrid(3947, 5166, 1),
        challengeName = "The Wardens",
    ),

    // Capture: the Scabaras path landed on (3523, 5280). Offline_Scape has x = 3522.
    SCABARAS_PUZZLE(
        440, 656, Kind.PUZZLE,
        spawn = CoordGrid(3523, 5279, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3575, 5280, 0),
        challengeMin = CoordGrid(3533, 5268, 0),
        challengeMax = CoordGrid(3574, 5292, 0),
        challengeName = "Path of Scabaras",
    ),

    HET_PUZZLE(
        456, 656, Kind.PUZZLE,
        spawn = CoordGrid(3698, 5279, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3667, 5280, 0),
        challengeMin = CoordGrid(3670, 5267, 0),
        challengeMax = CoordGrid(3690, 5293, 0),
        challengeName = "Path of Het",
    ),

    APMEKEN_PUZZLE(
        472, 656, Kind.PUZZLE,
        spawn = CoordGrid(3792, 5279, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3814, 5280, 0),
        challengeMin = CoordGrid(3797, 5267, 0),
        challengeMax = CoordGrid(3819, 5293, 0),
        challengeName = "Path of Apmeken",
    ),

    CRONDIS_PUZZLE(
        488, 656, Kind.PUZZLE,
        spawn = CoordGrid(3954, 5279, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3943, 5280, 0),
        challengeMin = CoordGrid(3923, 5250, 0),
        challengeMax = CoordGrid(3949, 5311, 0),
        challengeName = "Path of Crondis",
    ),

    SCABARAS_BOSS(
        440, 672, Kind.BOSS,
        spawn = CoordGrid(3535, 5408, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3544, 5408, 0),
        challengeMin = CoordGrid(3543, 5400, 0),
        challengeMax = CoordGrid(3559, 5416, 0),
        osmumtenTile = CoordGrid(3558, 5408, 0),
        challengeName = "Kephri",
    ),

    HET_BOSS(
        456, 672, Kind.BOSS,
        spawn = CoordGrid(3698, 5406, 1), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3689, 5408, 1),
        challengeMin = CoordGrid(3670, 5395, 1),
        challengeMax = CoordGrid(3691, 5419, 1),
        osmumtenTile = CoordGrid(3673, 5407, 1),
        challengeName = "Akkha",
    ),

    APMEKEN_BOSS(
        472, 672, Kind.BOSS,
        spawn = CoordGrid(3790, 5407, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3800, 5408, 0),
        challengeMin = CoordGrid(3796, 5399, 0),
        challengeMax = CoordGrid(3823, 5418, 0),
        osmumtenTile = CoordGrid(3817, 5408, 0),
        challengeName = "Ba-Ba",
    ),

    CRONDIS_BOSS(
        488, 672, Kind.BOSS,
        spawn = CoordGrid(3958, 5407, 0), spawnSpreadZ = 2,
        challengeSpawn = CoordGrid(3941, 5408, 0),
        challengeMin = CoordGrid(3904, 5387, 0),
        challengeMax = CoordGrid(3962, 5429, 0),
        osmumtenTile = CoordGrid(3928, 5408, 0),
        challengeName = "Zebak",
    ) {
        /** Offline_Scape: the entrance corridor inside the bounding box doesn't count. */
        override fun inChallengeArea(static: CoordGrid): Boolean {
            if (static.inside(3957, 5404, 3960, 5414)) return false
            if (static.inside(3952, 5406, 3957, 5410)) return false
            return super.inChallengeArea(static)
        }
    };

    enum class Kind {
        MAIN_HALL,
        PUZZLE,
        BOSS,
        WARDENS,
        REWARD,
    }

    /** The path this room belongs to, for puzzle and boss rooms. */
    val path: ToaPath?
        get() = ToaPath.of(this)

    /**
     * Where the room's way forward leads: puzzle -> its boss, boss -> back to the nexus,
     * Wardens -> second Wardens room -> reward room. Offline_Scape used `ordinal + 1`, which
     * only worked because of its enum order.
     */
    val next: ToaRoom?
        get() =
            when (this) {
                SCABARAS_PUZZLE,
                HET_PUZZLE,
                APMEKEN_PUZZLE,
                CRONDIS_PUZZLE -> path?.boss
                SCABARAS_BOSS,
                HET_BOSS,
                APMEKEN_BOSS,
                CRONDIS_BOSS -> MAIN_HALL
                WARDENS_FIRST_ROOM -> WARDENS_SECOND_ROOM
                WARDENS_SECOND_ROOM -> REWARD_ROOM
                MAIN_HALL,
                REWARD_ROOM -> null
            }

    /**
     * Value of `toa_client_current_path` while in this room. From the client script
     * `toa_hud_draw`: 0 = nexus (all four levels shown), 1..4 = one path, 5/6 = no path panel.
     * Reward room: Offline_Scape leaves it at 5; 6 is unconfirmed.
     */
    val hudPath: Int
        get() =
            when (kind) {
                Kind.MAIN_HALL -> 0
                Kind.PUZZLE,
                Kind.BOSS -> path?.hudPath ?: 0
                Kind.WARDENS,
                Kind.REWARD -> HUD_PATH_WARDENS
            }

    /** Whether the static coordinate [static] is inside this room's challenge area. */
    open fun inChallengeArea(static: CoordGrid): Boolean {
        val min = challengeMin ?: return false
        val max = challengeMax ?: return false
        return static.inside(min.x, min.z, max.x, max.z)
    }

    /** A random arrival tile (static coords). */
    fun randomSpawn(random: GameRandom): CoordGrid =
        spawn.translate(random.of(0, spawnSpreadX), random.of(0, spawnSpreadZ))

    /**
     * The room's instance template: its map square on every level, placed at zone (4, 4) of a
     * small (16x16-zone) region so the room is surrounded by void, like vanilla.
     *
     * TODO: Offline_Scape copies only levels 0-1 for the second Wardens room. Check a capture.
     */
    val template: RegionStaticTemplate by lazy {
        RegionTemplate.create {
            copyAllLevels(zoneX, zoneZ) {
                regionZoneX = 4
                regionZoneZ = 4
                zoneWidth = ROOM_ZONE_LENGTH
                zoneLength = ROOM_ZONE_LENGTH
            }
        }
    }

    private companion object {
        /** One map square = 8 zones = 64 tiles. */
        const val ROOM_ZONE_LENGTH = 8

        const val HUD_PATH_WARDENS = 5

        fun CoordGrid.inside(minX: Int, minZ: Int, maxX: Int, maxZ: Int): Boolean =
            x in minX..maxX && z in minZ..maxZ
    }
}
