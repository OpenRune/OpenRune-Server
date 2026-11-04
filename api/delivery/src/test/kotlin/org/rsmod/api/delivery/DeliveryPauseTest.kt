package org.rsmod.api.delivery

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player

class DeliveryPauseTest {
    @Test
    fun `only an opted in stopped conveyor freezes its saved remaining time`() {
        var stopped = true
        val registry = DeliveryLineRegistry()
        val clock = DeliveryClock()
        val manager = DeliveryManager(registry, clock)
        val player = Player()
        val physical = DeliveryLine("physical", 13, { 100 }, { _, _ -> },
            frozen = { stopped }, pauseWhileFrozen = true)
        val ordinary = DeliveryLine("ordinary", 13, { 100 }, { _, _ -> }, frozen = { stopped })
        registry.register(physical)
        registry.register(ordinary)
        manager.enqueue(player, physical, 453)
        manager.enqueue(player, ordinary, 444)
        val state = player.attr[DeliveryAttributes.LIVE]!!
        assertTrue(state.lines.getValue("physical").pausedAtSave)
        assertFalse(state.lines.getValue("ordinary").pausedAtSave)
        repeat(5) { clock.tick(); manager.drain(player) }
        assertEquals(13, state.lines.getValue("physical").pending.single().dueCycle - clock.cycle)
        stopped = false
        manager.flushForSave(player)
        assertFalse(state.lines.getValue("physical").pausedAtSave)
        repeat(13) { clock.tick(); manager.drain(player) }
        assertEquals(1, manager.storedCount(player, physical))
        assertEquals(0, manager.pendingCount(player, physical))
    }

    @Test
    fun `a stopped conveyor preserves travel remaining for every batch`() {
        val line = DeliveryLineState()
        line.pending += PendingDelivery(453, 1, 13)
        line.pending += PendingDelivery(444, 3, 18)
        line.recomputeNextDue()

        line.pausePending(20)

        assertEquals(listOf(33, 38), line.pending.map { it.dueCycle })
        assertEquals(33, line.nextDueCycle)
        assertEquals(4, line.pendingTotal())
        assertEquals(0, line.storedTotal())
    }

    @Test
    fun `saving paused parcels preserves their remaining travel`() {
        val state = DeliveryState()
        val line = state.line("conveyor")
        line.pending += PendingDelivery(453, 2, 13)
        line.pausePending(20)
        state.recomputeNextDue()

        val saved = DeliveryCodec.encode(state, cycle = 25, nowMillis = 1_000_000L)
        val restored = DeliveryCodec.decode(saved, cycle = 100, nowMillis = 1_000_000L)

        assertEquals(108, restored.lines.getValue("conveyor").pending.single().dueCycle)
    }
}
