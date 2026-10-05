package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.bosses.runtime.BossEncounter
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc

@Execution(ExecutionMode.SAME_THREAD)
class DoomRotationTest {
    @Test
    fun `level five waits for its first shield instead of the old timed shockwave`() {
        val encounter = encounter(5)
        encounter.npc.vars[DoomVarns.FIGHT_TICKS] = 200
        encounter.npc.vars[DoomVarns.ATTACKS] = 10
        assertNull(encounter.selectPriorityAbility(200, null))
        encounter.npc.hitpoints = 468
        assertEquals("start_shield", encounter.selectPriorityAbility(200, null))
    }

    @Test
    fun `levels one to four keep the timed shockwave`() {
        for (level in 1..4) {
            val encounter = encounter(level)
            encounter.npc.vars[DoomVarns.FIGHT_TICKS] = 200
            assertEquals("volatile_earth", encounter.selectPriorityAbility(200, null))
        }
    }

    @Test
    fun `shield repeats after two post shockwave attacks even above the initial hp threshold`() {
        val encounter = encounter(5)
        repeat(3) { cycle ->
            encounter.transitionTo(FIGHT_PHASE, cycle * 100)
            encounter.npc.vars[DoomVarns.ROTATION_STEP] = DoomCar.POST_SHOCKWAVE
            encounter.npc.vars[DoomVarns.STEP_ATTACKS] = 0
            assertNull(encounter.selectPriorityAbility(cycle * 100, null))
            encounter.npc.vars[DoomVarns.STEP_ATTACKS] = 1
            assertEquals("standard", encounter.selectPriorityAbility(cycle * 100 + 10, null))
            encounter.npc.vars[DoomVarns.STEP_ATTACKS] = 2
            assertEquals("start_shield", encounter.selectPriorityAbility(cycle * 100 + 15, null))
            encounter.transitionTo(DoomShield.PHASE, cycle * 100 + 15)
            assertNull(encounter.selectPriorityAbility(cycle * 100 + 20, null))
        }
    }

    @Test
    fun `low hp after emergence does not skip straight into another shield`() {
        val encounter = encounter(5)
        encounter.npc.hitpoints = 100
        encounter.npc.vars[DoomVarns.ATTACKS] = 20
        encounter.npc.vars[DoomVarns.ROTATION_STEP] = DoomCar.POST_EMERGE
        assertNull(encounter.selectPriorityAbility(200, null))
    }

    private fun encounter(level: Int): BossEncounter {
        val delve = DoomDelve.of(level)
        val type = NpcServerType(id = 1, name = "Doom", size = 5, hitpoints = delve.hitpoints)
        return BossEncounter(Npc(type, DoomArena.BOSS_SPAWN), doomSpec(delve), MapClock()) { null }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
