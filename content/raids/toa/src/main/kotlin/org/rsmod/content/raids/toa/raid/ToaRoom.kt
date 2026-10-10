package org.rsmod.content.raids.toa.raid

import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.table.ToaRoomExclusionsRow
import org.rsmod.api.table.ToaRoomsRow
import org.rsmod.map.CoordGrid

class ToaRoom private constructor(private val row: ToaRoomsRow) {
    val kind: Kind = Kind.valueOf(row.kind)
    val spawn: CoordGrid = row.spawn
    val challengeSpawn: CoordGrid? = row.challengeSpawn
    val challengeMin: CoordGrid? = row.challengeMin
    val challengeMax: CoordGrid? = row.challengeMax
    val osmumtenTile: CoordGrid? = row.osmumtenTile
    val dropTile: CoordGrid? = row.dropTile
    val challengeName: String? = row.challengeName

    private val exclusions: List<ToaRoomExclusionsRow> by lazy {
        ToaRoomExclusionsRow.all().filter { it.room.rowId == row.rowId }
    }

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
        get() = row.next?.let(::of)

    val hudPath: Int
        get() =
            when (kind) {
                Kind.MAIN_HALL -> 0
                Kind.PUZZLE,
                Kind.BOSS -> path?.hudPath ?: 0
                Kind.WARDENS,
                Kind.REWARD -> HUD_PATH_WARDENS
            }

    fun inChallengeArea(static: CoordGrid): Boolean {
        val min = challengeMin ?: return false
        val max = challengeMax ?: return false
        if (exclusions.any { static.inside(it.min, it.max) }) return false
        return static.inside(min, max)
    }

    fun randomSpawn(random: GameRandom): CoordGrid =
        spawn.translate(random.of(0, row.spawnSpreadX), random.of(0, row.spawnSpreadZ))

    val template: RegionStaticTemplate by lazy {
        RegionTemplate.create {
            for (level in row.minLevel..row.maxLevel) {
                copy(row.zoneX, row.zoneZ, level) {
                    regionZoneX = 4
                    regionZoneZ = 4
                    zoneWidth = ROOM_ZONE_LENGTH
                    zoneLength = ROOM_ZONE_LENGTH
                }
            }
        }
    }

    companion object {
        private const val ROOM_ZONE_LENGTH = 8
        private const val HUD_PATH_WARDENS = 5

        private val BY_ROW: Map<Int, ToaRoom> by lazy {
            ToaRoomsRow.all().associate { it.rowId to ToaRoom(it) }
        }

        fun of(row: ToaRoomsRow): ToaRoom = BY_ROW.getValue(row.rowId)

        fun of(symbol: String): ToaRoom = of(ToaRoomsRow.getRow(symbol))

        val MAIN_HALL by lazy { of("dbrow.toa_room_main_hall") }
        val REWARD_ROOM by lazy { of("dbrow.toa_room_reward") }
        val WARDENS_FIRST_ROOM by lazy { of("dbrow.toa_room_wardens_first") }
        val WARDENS_SECOND_ROOM by lazy { of("dbrow.toa_room_wardens_second") }
        val CRONDIS_PUZZLE by lazy { of("dbrow.toa_room_crondis_puzzle") }
        val CRONDIS_BOSS by lazy { of("dbrow.toa_room_crondis_boss") }

        private fun CoordGrid.inside(min: CoordGrid, max: CoordGrid): Boolean =
            x in min.x..max.x && z in min.z..max.z
    }
}
