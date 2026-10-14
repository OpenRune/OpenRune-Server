package org.rsmod.content.raids.toa.raid.encounter.wardens

import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.table.ToaWardensSlamsRow
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.map.CoordGrid

internal enum class WardenSlam(
    val ability: String,
    val row: String,
    val seq: String,
    val fastSeq: String,
) {
    EAST(
        "slam_east",
        "dbrow.toa_wardens_slam_east",
        "seq.npc_wardens_attackleft01",
        "seq.npc_wardens_attackleft02",
    ),
    WEST(
        "slam_west",
        "dbrow.toa_wardens_slam_west",
        "seq.npc_wardens_attackright01",
        "seq.npc_wardens_attackright02",
    ),
    BOTH(
        "slam_both",
        "dbrow.toa_wardens_slam_both",
        "seq.npc_wardens_attackcenter01",
        "seq.npc_wardens_attackcenter02",
    ),
}

internal class WardensSlams(private val room: WardensSecondEncounter) {
    private val deps = room.raid.deps

    private val tiles = HashMap<WardenSlam, List<CoordGrid>>()
    private val variants = HashMap<WardenSlam, List<Int>>()

    fun fire(slam: WardenSlam) {
        val row = tiles.getOrPut(slam) { ToaWardensSlamsRow.getRow(slam.row).tiles }
        val kinds = variants.getOrPut(slam) { ToaWardensSlamsRow.getRow(slam.row).variants }
        val struck = HashMap<Int, MutableSet<CoordGrid>>()
        for ((index, tile) in row.withIndex()) {
            val coords = room.coords(tile)
            val delay = delay(tile)
            deps.worldRepo.spotanimMap(spotanim(VARIANTS[kinds[index]]), coords, delay = delay)
            struck.getOrPut(Math.ceilDiv(delay, CYCLES_PER_TICK)) { HashSet() } += coords
        }
        for ((ticks, set) in struck) room.schedule(ticks) { damage(set) }
    }

    private fun delay(tile: CoordGrid): Int =
        BASE_DELAY + STEP_DELAY * max(abs(tile.x - ORIGIN.x), abs(tile.z - ORIGIN.z))

    private fun damage(tiles: Set<CoordGrid>) {
        for (player in room.targets()) {
            if (player.coords in tiles) room.hazardDamage(player, DAMAGE, DAMAGE_SPREAD)
        }
    }

    private companion object {
        val ORIGIN = CoordGrid(3936, 5157, 1)

        val VARIANTS =
            listOf(
                "spotanim.spotanim_wardens01_undertile01_explode01",
                "spotanim.spotanim_wardens01_undertile01_explode02",
                "spotanim.spotanim_wardens01_undertile01_explode03",
                "spotanim.spotanim_wardens01_undertile01_explode04",
            )

        const val BASE_DELAY = 90
        const val STEP_DELAY = 6
        const val CYCLES_PER_TICK = 30
        const val DAMAGE = 18
        const val DAMAGE_SPREAD = 2
    }
}
