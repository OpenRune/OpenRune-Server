package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.area.MapAreaDecoder
import dev.openrune.map.area.MapAreaDefinitions
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.area.AreaIndex
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

@ResourceLock("ServerCacheManager")
class BimMusicAreaTest {
    @Test
    fun `camdozaal music areas cover the ruins and vault`() {
        val cache = ServerCacheManager.init(240)
        val areas = AreaIndex()
        try {
            for (mx in 45..47) {
                for (mz in 90..92) {
                    val group = (mx shl 8) or mz
                    val data = cache.data(MAPS, group, 7) ?: continue
                    val definition = MapAreaDecoder.decode(InlineByteBuf(data))
                    MapAreaDefinitions.putAreas(areas, MapSquareKey(mx, mz), definition)
                }
            }
        } finally {
            cache.close()
        }

        assertTrue(inArea(areas, CoordGrid(2951, 5761), "area.camdozaal_ruins"))
        assertTrue(inArea(areas, CoordGrid(2953, 5769), "area.camdozaal_ruins"))
        assertTrue(inArea(areas, CoordGrid(2905, 5810), "area.camdozaal_ruins"))
        assertTrue(inArea(areas, CoordGrid(3010, 5800), "area.camdozaal_ruins"))
        assertFalse(inArea(areas, CoordGrid(2969, 5824), "area.camdozaal_ruins"))

        assertTrue(inArea(areas, CoordGrid(2969, 5824), "area.camdozaal_vault"))
        assertTrue(inArea(areas, CoordGrid(2960, 5855), "area.camdozaal_vault"))
        assertTrue(inArea(areas, CoordGrid(2985, 5840), "area.camdozaal_vault"))
        assertFalse(inArea(areas, CoordGrid(2969, 5823), "area.camdozaal_vault"))

        assertFalse(inArea(areas, CoordGrid(3043, 5894), "area.camdozaal_ruins"))
        assertFalse(inArea(areas, CoordGrid(3043, 5894), "area.camdozaal_vault"))
    }

    private fun inArea(areas: AreaIndex, coords: CoordGrid, area: String): Boolean {
        val buffer = ShortArrayList()
        areas.putAreasIncludingParents(coords, buffer)
        return area.asRSCM().toShort() in buffer
    }
}
