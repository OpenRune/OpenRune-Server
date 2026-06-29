package org.rsmod.api.combat.commons.npc

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class NpcCanRetaliateTest {
    @Test
    fun `ignoreCombatInteractions forces canRetaliate to be false`() {
        val npc = newNpc()

        // Baseline: a freshly spawned npc would retaliate when attacked.
        assertTrue(npc.canRetaliate())

        // Handing control to a scripted sequence suppresses retaliation entirely.
        npc.ignoreCombatInteractions = true
        assertFalse(npc.canRetaliate())
    }

    @Test
    fun `ignoreCombatInteractions overrides the active-combat-delay shortcut`() {
        val npc = newNpc()
        // Force the `actionDelay + activecombat_delay < currentMapClock` branch to be true.
        npc.currentMapClock = 100
        npc.actionDelay = 0

        assertTrue(npc.canRetaliate())

        npc.ignoreCombatInteractions = true
        assertFalse(npc.canRetaliate())
    }

    private fun newNpc(): Npc {
        val type = NpcServerType(id = 1, name = "Man", size = 1, hitpoints = 50)
        return Npc(type, CoordGrid(0, 1, 1, 0, 0))
    }
}
