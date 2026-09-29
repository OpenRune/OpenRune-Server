package org.rsmod.api.net.rsprot.player

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class AbandonedInstanceRecoveryTest {
    @Test fun `a disconnected character cannot relog into an abandoned instance without a return marker`() {
        val spawn = CoordGrid(3222, 3218)
        for (area in listOf(RegionRegistry.workingAreaSmall, RegionRegistry.workingAreaLarge)) {
            val player = Player()
            player.coords = area.calculateCoord(0)
            player.recoverAbandonedInstance(spawn)
            assertEquals(spawn, player.coords)
        }
    }

    @Test fun `ordinary world locations remain untouched`() {
        val island = CoordGrid(1637, 4802)
        val player = Player()
        player.coords = island
        player.recoverAbandonedInstance(CoordGrid(3222, 3218))
        assertEquals(island, player.coords)
    }
}