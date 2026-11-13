package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.events.PlayerAreaEvents
import org.rsmod.api.player.events.PlayerLevelEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.EngineQueueList
import org.rsmod.game.queue.EngineQueueType
import org.rsmod.game.stat.StatAdvance

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PlayerEngineQueueObserversTest {
    private val player = Player()
    private val bus = EventBus()
    private val attack: StatType
        get() = checkNotNull(ServerCacheManager.getStats("stat.attack".asRSCM(RSCMType.STAT)))

    @Test
    fun `a stat advance reports the previous and new base level`() {
        val seen = mutableListOf<PlayerLevelEvents.Advanced>()
        bus.subscribeUnbound(PlayerLevelEvents.Advanced::class.java) { seen += this }

        observe(EngineQueueType.AdvanceStat, StatAdvance(attack, 0), attack.id)

        val event = seen.single()
        assertSame(player, event.player)
        assertEquals(attack.id, event.stat.id)
        assertEquals(0, event.previousLevel)
        assertEquals(1, event.level)
    }

    @Test
    fun `a combat advance reports the level`() {
        val seen = mutableListOf<Int>()
        bus.subscribeUnbound(PlayerLevelEvents.CombatAdvanced::class.java) { seen += level }

        observe(EngineQueueType.AdvanceCombat, 42, 42)

        assertEquals(listOf(42), seen)
    }

    @Test
    fun `a stat change reports the stat`() {
        val seen = mutableListOf<Int>()
        bus.subscribeUnbound(PlayerLevelEvents.StatChanged::class.java) { seen += stat.id }

        observe(EngineQueueType.ChangeStat, attack, attack.id)

        assertEquals(listOf(attack.id), seen)
    }

    @Test
    fun `area queues report entering and leaving by label`() {
        val entered = mutableListOf<Int>()
        val exited = mutableListOf<Int>()
        bus.subscribeUnbound(PlayerAreaEvents.Entered::class.java) { entered += area }
        bus.subscribeUnbound(PlayerAreaEvents.Exited::class.java) { exited += area }

        observe(EngineQueueType.Area, null, 7)
        observe(EngineQueueType.AreaExit, null, 9)

        assertEquals(listOf(7), entered)
        assertEquals(listOf(9), exited)
    }

    @Test
    fun `other queue types publish nothing`() {
        val seen = mutableListOf<Any>()
        bus.subscribeUnbound(PlayerAreaEvents.Entered::class.java) { seen += this }
        bus.subscribeUnbound(PlayerLevelEvents.StatChanged::class.java) { seen += this }

        observe(EngineQueueType.Zone, null, 1)

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `no subscribers is not an error`() {
        observe(EngineQueueType.AdvanceStat, StatAdvance(attack, 0), attack.id)
        observe(EngineQueueType.Area, null, 7)
    }

    @Test
    fun `the processor publishes a queued area with no script bound`() {
        val entered = mutableListOf<Int>()
        bus.subscribeUnbound(PlayerAreaEvents.Entered::class.java) { entered += area }
        val processor = PlayerEngineQueueProcessor(bus, EngineQueueCache(), mockk())

        player.engineQueueArea(7)
        processor.process(player)

        assertEquals(listOf(7), entered)
    }

    private fun observe(type: EngineQueueType, args: Any?, label: Int) =
        bus.observeEngineQueue(player, EngineQueueList.Queue(type.id, args, label))

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
