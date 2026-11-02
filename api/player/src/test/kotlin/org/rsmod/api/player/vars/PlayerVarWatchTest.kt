package org.rsmod.api.player.vars

import dev.openrune.types.varp.VarpServerType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.utils.bits.withBits

/** The watch list and bus handle are global, so these cannot run in parallel. */
@Execution(ExecutionMode.SAME_THREAD)
class PlayerVarWatchTest {
    private val events = mutableListOf<PlayerVarChangedEvent>()
    private val player = Player().apply { processedMapClock = 1 }

    @BeforeEach
    fun setUp() {
        val bus = EventBus()
        bus.subscribeUnbound(PlayerVarChangedEvent::class.java) { events += this }
        PlayerVarWatch.eventBus = bus
    }

    @AfterEach
    fun tearDown() {
        PlayerVarWatch.eventBus = null
    }

    @Test
    fun `a change to a watched varp is published with both values`() {
        PlayerVarWatch.watch(WATCHED)
        VarPlayerIntMapSetter.set(player, varp(WATCHED), 3)
        VarPlayerIntMapSetter.set(player, varp(WATCHED), 7)

        assertEquals(listOf(0 to 3, 3 to 7), events.map { it.previous to it.value })
        assertTrue(events.all { it.player === player && it.varp == WATCHED })
    }

    @Test
    fun `the delegate write path publishes too`() {
        PlayerVarWatch.watch(WATCHED)
        VarPlayerIntMapDelegate.from(player)[varp(WATCHED)] = 5

        assertEquals(5, events.single().value)
    }

    @Test
    fun `an unwatched varp publishes nothing`() {
        VarPlayerIntMapSetter.set(player, varp(UNWATCHED), 3)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `writing the same value publishes nothing`() {
        PlayerVarWatch.watch(WATCHED)
        VarPlayerIntMapSetter.set(player, varp(WATCHED), 0)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a write before the first processed tick publishes nothing`() {
        PlayerVarWatch.watch(WATCHED)
        player.processedMapClock = 0
        VarPlayerIntMapSetter.set(player, varp(WATCHED), 3)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `a varbit watch reports only its own bits`() {
        val watch = PlayerVarWatch.watch(WATCHED, BITS)
        val packed = 0.withBits(BITS, 2).withBits(OTHER_BITS, 1)
        VarPlayerIntMapSetter.set(player, varp(WATCHED), packed)

        val seen = checkNotNull(watch.matching(events.single()))
        assertEquals(0, seen.previous)
        assertEquals(2, seen.value)
    }

    @Test
    fun `a varbit watch ignores a change to other bits of its varp`() {
        val watch = PlayerVarWatch.watch(WATCHED, BITS)
        VarPlayerIntMapSetter.set(player, varp(WATCHED), 0.withBits(OTHER_BITS, 1))

        assertNull(watch.matching(events.single()))
    }

    @Test
    fun `a watch ignores another varp`() {
        val watch = PlayerVarWatch.watch(WATCHED)
        val event = PlayerVarChangedEvent(player, UNWATCHED, 0, 1)

        assertNull(watch.matching(event))
    }

    private fun varp(id: Int) = VarpServerType(id)

    private companion object {
        const val WATCHED = 60_001
        const val UNWATCHED = 60_002
        val BITS = 0..3
        val OTHER_BITS = 4..7
    }
}
