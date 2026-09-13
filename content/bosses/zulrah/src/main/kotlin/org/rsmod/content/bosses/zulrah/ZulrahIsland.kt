package org.rsmod.content.bosses.zulrah

import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

internal object ZulrahIsland {
    const val KEY = "zulrah"
    const val RANGED_FORM = "npc.snakeboss_boss_ranged"
    val zulAndraTeleport = CoordGrid(2196, 3056, 0)
    val arrival = CoordGrid(2268, 3068, 0)
    val openingSpawn = CoordGrid(2266, 3073, 0)

    private val recordedRotations = listOf(
        "32030312", "02201030", "32012101", "22100122", "31200213",
        "11200211", "32012101", "02201030", "32030312",
    )

    data class Chunk(val x: Int, val z: Int, val level: Int, val source: ZoneKey, val rotation: Int)

    // RSProx submission 3276, rebuild tick 8314: retain its actual surrounding-chunk rotations.
    val chunks: List<Chunk> = buildList {
        for (level in 0..1) {
            for (x in 0..8) {
                for (z in 0..7) {
                    val core = x in 3..5 && z in 3..4
                    val source = if (core) ZoneKey(282 + x - 3, 383 + z - 3, level)
                        else ZoneKey(284, 382, level)
                    add(Chunk(x, z, level, source, recordedRotations[x][z].digitToInt()))
                }
            }
        }
    }

    fun spec(returnTo: CoordGrid): InstanceSpec = InstanceSpec(
        fee = 0,
        maxPlayers = 1,
        reclaimTicks = 0,
        graceTicks = 0,
        destroyWhenEmpty = true,
        settingsRowId = -1,
        bossName = "Zulrah",
        area = InstanceArea.template(
            template = RegionTemplate.create {
                for (chunk in chunks) {
                    this[chunk.x, chunk.z, chunk.level] =
                        RegionZoneCopy(chunk.source, chunk.rotation, flag = null)
                }
            },
            enterCoord = RegionLocal(arrival.level, arrival.mx, arrival.mz, arrival.lx, arrival.lz),
            exitCoord = returnTo,
        ),
    )
}
