package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.map.CoordGrid

internal class WardensObeliskBlast(private val room: WardensEncounter) {
    private val deps = room.raid.deps

    private class Scarab(val spot: String, val delay: Int, val height: Int, val x: Int, val z: Int)

    fun detonate(loser: WardenGod) {
        val lane = laneTiles()
        for ((tile, delay) in lane) {
            val coords = room.coords(mirror(tile, loser))
            deps.worldRepo.spotanimMap(spotanim(BLAST), coords, delay = delay)
        }
        for (scarab in SCARABS) {
            val tile = mirror(CoordGrid(scarab.x, scarab.z, 1), loser)
            deps.worldRepo.spotanimMap(
                spotanim(scarab.spot),
                room.coords(tile),
                height = scarab.height,
                delay = scarab.delay,
            )
        }
        for ((x, delay) in SOUND_COLUMNS) {
            val tile = mirror(CoordGrid(x, MIDDLE_Z, 1), loser)
            deps.worldRepo.soundArea(
                room.coords(tile),
                SOUND,
                delay = delay,
                radius = SOUND_RADIUS,
            )
        }
        val hit = lane.map { room.coords(mirror(it.first, loser)) }.toSet()
        for (player in room.targets()) {
            if (player.coords in hit) player.hitTypeless(room.hazardRoll(BLAST_DAMAGE, 0))
        }
    }

    private fun laneTiles(): List<Pair<CoordGrid, Int>> = buildList {
        for (d in 1..LANE_LENGTH) add(CoordGrid(EDGE_X + d, MIDDLE_Z, 1) to d * DELAY_PER_TILE)
        for (z in listOf(MIDDLE_Z - 1, MIDDLE_Z + 1)) {
            add(CoordGrid(EDGE_X + 2, z, 1) to 2 * DELAY_PER_TILE)
        }
        for (d in 3..LANE_LENGTH) {
            add(CoordGrid(EDGE_X + d, MIDDLE_Z - 2, 1) to d * DELAY_PER_TILE)
            add(CoordGrid(EDGE_X + d, MIDDLE_Z + 2, 1) to d * DELAY_PER_TILE)
        }
    }

    private fun mirror(tile: CoordGrid, loser: WardenGod): CoordGrid =
        if (loser.orbSide > 0) tile else CoordGrid(MIRROR_SUM - tile.x, tile.z, tile.level)

    private companion object {
        const val BLAST = "spotanim.obelisk_explode_energy"
        const val SOUND = "synth.toa_wardens_obelisk_lane_explode"
        const val SOUND_RADIUS = 15
        const val BLAST_DAMAGE = 11
        const val EDGE_X = 3810
        const val MIDDLE_Z = 5154
        const val LANE_LENGTH = 8
        const val DELAY_PER_TILE = 6
        const val MIRROR_SUM = 7616

        val SOUND_COLUMNS =
            (3811..3821).map { it to (if (it <= 3814) 0 else if (it <= 3819) 1 else 2) }

        val SCARABS =
            listOf(
                Scarab("spotanim.vfx_scarab_explosion01", 54, 25, 3819, 5152),
                Scarab("spotanim.vfx_scarab_explosion03", 54, 25, 3819, 5154),
                Scarab("spotanim.vfx_scarab_explosion01", 54, 25, 3819, 5156),
                Scarab("spotanim.vfx_scarab_explosion03", 60, 105, 3820, 5153),
                Scarab("spotanim.vfx_scarab_explosion03", 60, 176, 3820, 5154),
                Scarab("spotanim.vfx_scarab_explosion01", 60, 159, 3820, 5155),
                Scarab("spotanim.vfx_scarab_explosion04", 66, 105, 3821, 5154),
            )
    }
}
