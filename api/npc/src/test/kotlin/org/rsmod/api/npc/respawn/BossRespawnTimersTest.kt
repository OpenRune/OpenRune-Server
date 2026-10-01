package org.rsmod.api.npc.respawn

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity

class BossRespawnTimersTest {
    @Test
    fun `all four GWD bosses use sixty seconds and ordinary bosses round up twenty seconds`() {
        for (god in listOf("bandos", "armadyl", "saradomin", "zamorak")) {
            assertEquals(100, BossRespawnPolicy.ticksForSymbol("npc.godwars_${god}_avatar"))
        }
        assertEquals(100, BossRespawnPolicy.ticksForSymbol("npc.nex"))
        for (boss in listOf("king_dragon", "muspah", "whisperer_spawn", "rat_boss_instance",
            "snakeboss_boss_ranged", "duke_sucellus_asleep", "hydraboss", "vorkath")) {
            assertEquals(34, BossRespawnPolicy.ticksForSymbol("npc.$boss"))
        }
        assertEquals(60_000, BossRespawnPolicy.GOD_WARS_TICKS * BossRespawnPolicy.TICK_MILLIS)
        assertEquals(20_400, BossRespawnPolicy.OTHER_BOSS_TICKS * BossRespawnPolicy.TICK_MILLIS)
    }

    @Test
    fun `minions normal monsters pets and progression encounters are not automatic bosses`() {
        for (npc in listOf("npc.goblin", "npc.tormented_demon_1", "npc.gemstone_crab",
            "npc.snakeboss_minion_melee", "npc.amoxliatl_ice_block", "npc.duke_sucellus_eye",
            "npc.barrows_dharok", "npc.bandospet")) {
            assertNull(BossRespawnPolicy.ticksForSymbol(npc), npc)
        }
    }

    @Test
    fun `snapshot follows scheduled game ticks and expires exactly at the due cycle`() {
        val clock = MapClock(1000)
        val timers = BossRespawnTimers(clock)
        val npc = npc(4)
        assertEquals(1034, timers.schedule(npc, 34))
        val first = timers.snapshot().single()
        assertEquals(34, first.remainingTicks)
        clock.cycle = 1033
        assertEquals(1, timers.snapshot().single().remainingTicks)
        clock.cycle = 1034
        assertTrue(timers.snapshot().isEmpty())
    }

    @Test
    fun `instance deletion cancels normal timers and explicit encounter resets survive deletion`() {
        val clock = MapClock(100)
        val timers = BossRespawnTimers(clock)
        val normal = npc(4)
        val zulrah = npc(5)
        timers.schedule(normal, 100)
        timers.schedule(zulrah, 34, retainAfterDelete = true)
        normal.slotId = PathingEntity.INVALID_SLOT
        zulrah.slotId = PathingEntity.INVALID_SLOT
        val reset = timers.snapshot().single()
        assertEquals(5, reset.npcIndex, "Keep the observed pre-deletion client index")
        timers.cancel(zulrah)
        assertTrue(timers.snapshot().isEmpty())
    }

    @Test
    fun `replacement death has a new identity and never accumulates two timers for one NPC`() {
        val timers = BossRespawnTimers(MapClock(100))
        val npc = npc(4)
        timers.schedule(npc, 34)
        val first = timers.snapshot().single().key
        timers.schedule(npc, 100)
        assertNotEquals(first, timers.snapshot().single().key)
        assertEquals(100, timers.snapshot().single().remainingTicks)
    }

    private fun npc(index: Int): Npc =
        Npc(NpcServerType(id = 123, name = "Test boss")).apply { slotId = index }
}
