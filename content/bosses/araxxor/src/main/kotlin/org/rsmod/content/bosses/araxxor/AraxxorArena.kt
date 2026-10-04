package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceSettings
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.map.CoordGrid

internal object AraxxorArena {
    const val KEY = "araxxor"
    val arrival = CoordGrid(3645, 9816, 0)
    val bossSpawn = CoordGrid(3630, 9813, 0)
    val outside = CoordGrid(3658, 9816, 0)
    val exitTunnel = CoordGrid(3648, 9814, 0)
    val regions = listOf(14489, 14745)
    val eggs = listOf(
        CoordGrid(3643, 9807), CoordGrid(3637, 9803), CoordGrid(3631, 9803),
        CoordGrid(3625, 9808), CoordGrid(3623, 9815), CoordGrid(3625, 9822),
        CoordGrid(3629, 9827), CoordGrid(3637, 9827), CoordGrid(3643, 9823),
    )

    fun spec(returnTo: CoordGrid): InstanceSpec = InstanceSettings(
        maxPlayers = 1,
        reclaimTicks = 0,
        graceTicks = 0,
        destroyWhenEmpty = true,
        bossName = "Araxxor",
        bossNpc = listOf(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM()))),
    ).withArea(
        InstanceArea.copyRegions(
            regionIds = regions,
            enterCoord = RegionLocal(arrival.level, arrival.mx, arrival.mz, arrival.lx, arrival.lz),
            exitCoord = returnTo,
        ),
        settingsRowId = -1,
    )
}
