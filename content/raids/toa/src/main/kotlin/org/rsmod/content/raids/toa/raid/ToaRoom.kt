package org.rsmod.content.raids.toa.raid

import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid

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
    val levels: IntRange = 0..3,
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
        levels = 0..1,
    ),

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

    val path: ToaPath?
        get() = ToaPath.of(this)

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

    val hudPath: Int
        get() =
            when (kind) {
                Kind.MAIN_HALL -> 0
                Kind.PUZZLE,
                Kind.BOSS -> path?.hudPath ?: 0
                Kind.WARDENS,
                Kind.REWARD -> HUD_PATH_WARDENS
            }

    open fun inChallengeArea(static: CoordGrid): Boolean {
        val min = challengeMin ?: return false
        val max = challengeMax ?: return false
        return static.inside(min.x, min.z, max.x, max.z)
    }

    fun randomSpawn(random: GameRandom): CoordGrid =
        spawn.translate(random.of(0, spawnSpreadX), random.of(0, spawnSpreadZ))

    val template: RegionStaticTemplate by lazy {
        RegionTemplate.create {
            for (level in levels) {
                copy(zoneX, zoneZ, level) {
                    regionZoneX = 4
                    regionZoneZ = 4
                    zoneWidth = ROOM_ZONE_LENGTH
                    zoneLength = ROOM_ZONE_LENGTH
                }
            }
        }
    }

    private companion object {
        const val ROOM_ZONE_LENGTH = 8
        const val HUD_PATH_WARDENS = 5

        fun CoordGrid.inside(minX: Int, minZ: Int, maxX: Int, maxZ: Int): Boolean =
            x in minX..maxX && z in minZ..maxZ
    }
}
