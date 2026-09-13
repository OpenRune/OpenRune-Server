package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

@OptIn(org.rsmod.annotations.InternalApi::class)
class ZulAndraNpcTest {
    @Test
    fun `priestess cheray and both bait spot forms are stationary at their village spawns`() {
        val script = ZulAndraNpcScript()
        val collision = org.rsmod.routefinder.collision.CollisionFlagMap()
        val movement = org.rsmod.api.game.process.npc.NpcMovementProcessor(collision,
            org.rsmod.api.route.StepFactory(collision), org.rsmod.events.EventBus())
        for (symbol in listOf("npc.snakeboss_highpriest", "npc.snakeboss_gnome_1",
            "npc.snakeboss_fishingspot", "npc.snakeboss_fishingspot_fake")) {
            val npc = Npc(symbol, CoordGrid(2195, 3055, 0))
            npc.slotId = 1
            script.anchor(npc)
            assertTrue(npc.movementLocked, symbol)
            assertEquals(NpcMode.None, npc.mode, symbol)
            assertEquals(npc.spawnCoords, npc.coords)
            repeat(100) {
                npc.walk(CoordGrid(2198, 3055, 0))
                movement.process(npc)
                assertEquals(npc.spawnCoords, npc.coords)
                assertNull(npc.routeRequest)
                assertTrue(npc.routeDestination.isEmpty())
            }
            npc.resetDefaults()
            script.anchor(npc)
            assertTrue(npc.movementLocked)
            assertEquals(NpcMode.None, npc.mode)
        }
    }

    @Test
    fun `stationary override does not affect other villagers or copies outside Zul Andra`() {
        val script = ZulAndraNpcScript()
        for (npc in listOf(Npc("npc.snakeboss_gnome_1", CoordGrid(3200, 3200, 0)),
            Npc("npc.snakeboss_boss_ranged", CoordGrid(2195, 3055, 0)))) {
            val previousMode = npc.mode
            val previousLock = npc.movementLocked
            script.anchor(npc)
            assertEquals(previousMode, npc.mode)
            assertEquals(previousLock, npc.movementLocked)
        }
    }

    companion object {
        @BeforeAll @JvmStatic fun loadCache() { ServerCacheManager.init(240) }
    }
}
