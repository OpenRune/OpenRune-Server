package org.rsmod.content.other.pouches.lootingbag

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.area.MapAreaDecoder
import dev.openrune.map.area.MapAreaDefinitions
import dev.openrune.map.util.InlineByteBuf
import org.mockito.Mockito.mock
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.area.AreaIndex
import org.rsmod.map.square.MapSquareKey

internal object LootingBagAreaTestData {
    lateinit var areas: LootingBagAreas
        private set
    fun initialize() {
        val index = AreaIndex()
        val cache = ServerCacheManager.init(240)
        try {
            for (square in listOf(MapSquareKey(48, 55), MapSquareKey(48, 56), MapSquareKey(49, 56))) {
                val group = square.x shl 8 or square.z
                MapAreaDefinitions.putAreas(index, square, MapAreaDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 7)))))
            }
        } finally { cache.close() }
        areas = LootingBagAreas(AreaChecker(mock(RegionRegistry::class.java), index))
    }
}
