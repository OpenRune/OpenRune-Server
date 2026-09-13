package org.rsmod.api.death

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

class NpcDeathKillContextTest {
    @Test
    fun `boss duration is explicit while normal kills retain the player preference`() {
        val player = Player().apply { lootDropDuration = 400 }
        val npc = Npc(NpcServerType(id = 1), CoordGrid(3200, 3200, 0))
        assertEquals(400, NpcDeathKillContext(player, npc, 1).dropDuration)
        assertEquals(18000, NpcDeathKillContext(player, npc, 1, dropDuration = 18000).dropDuration)
    }

    @Test
    fun `ordinary kills retain the NPC tile`() {
        val npc = Npc(NpcServerType(id = 1), CoordGrid(3200, 3200, 0))
        val context = NpcDeathKillContext(Player(), npc, 1)
        assertEquals(npc.coords, context.dropCoords)
    }

    @Test
    fun `boss loot uses an explicit tile without moving the boss`() {
        val swamp = CoordGrid(2266, 3072, 0)
        val island = CoordGrid(2263, 3075, 0)
        val npc = Npc(NpcServerType(id = 1), swamp)
        val context = NpcDeathKillContext(Player(), npc, 1, island)
        assertEquals(island, context.dropCoords)
        assertEquals(swamp, npc.coords)
    }
}
