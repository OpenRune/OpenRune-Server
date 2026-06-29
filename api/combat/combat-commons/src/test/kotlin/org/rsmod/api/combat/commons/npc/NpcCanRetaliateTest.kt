package org.rsmod.api.combat.commons.npc

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

class NpcCanRetaliateTest {
    /**
     * [Npc.ignoreCombatInteractions] is the gate that lets a scripted sequence (e.g. Scurrius
     * walking to a cheese pile at a hp threshold) take full control of the npc: while it is set,
     * [canRetaliate] must always return `false`, so an attacking player can never make the npc
     * retaliate and re-route toward them, clobbering the scripted route.
     */
    @Test
    fun `ignoreCombatInteractions forces canRetaliate to be false`() {
        val npc = newNpc()

        // Baseline: a freshly spawned npc would retaliate when attacked.
        assertTrue(npc.canRetaliate())

        // Handing control to a scripted sequence suppresses retaliation entirely.
        npc.ignoreCombatInteractions = true
        assertFalse(npc.canRetaliate())
    }

    /**
     * The gate must short-circuit even the strongest "should retaliate" path: when enough time has
     * elapsed since the last action ([Npc.actionDelay] well in the past), [canRetaliate] would
     * otherwise return `true` unconditionally. The flag must still win.
     */
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
