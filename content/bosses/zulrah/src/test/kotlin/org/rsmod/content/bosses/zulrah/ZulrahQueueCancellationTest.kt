package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.queue.PlayerQueueList
import org.rsmod.game.queue.QueueCategory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ZulrahQueueCancellationTest {
    @Test
    fun `cancelling encounter hits preserves unrelated hits with equal values`() {
        val queues = PlayerQueueList()
        val encounterHit = Marker(1)
        val unrelatedHit = Marker(1)
        val unrelatedAction = Any()
        queues.add("queue.hit", QueueCategory.Strong, 1, encounterHit)
        queues.add("queue.hit", QueueCategory.Strong, 2, unrelatedHit)
        queues.add("queue.com_retaliate_npc", QueueCategory.Normal, 3, unrelatedAction)

        assertEquals(1, queues.removeIf { it.args === encounterHit })
        assertEquals(2, queues.size)
        assertEquals(1, queues.strongQueues)
        val iterator = requireNotNull(queues.iterator())
        assertSame(unrelatedHit, iterator.next().args)
        assertSame(unrelatedAction, iterator.next().args)
        assertFalse(iterator.hasNext())
        iterator.cleanUp()
    }

    @Test
    fun `cancellation during dispatch skips consecutive removed next nodes`() {
        val queues = PlayerQueueList()
        val dispatched = Any()
        val keep = Any()
        val cancel = Any()
        queues.add("queue.hit", QueueCategory.Strong, 1, dispatched)
        queues.add("queue.hit", QueueCategory.Strong, 1, cancel)
        queues.add("queue.hit", QueueCategory.Strong, 1, cancel)
        queues.add("queue.hit", QueueCategory.Normal, 1, keep)

        val iterator = requireNotNull(queues.iterator())
        assertSame(dispatched, iterator.next().args)
        iterator.remove()
        assertEquals(2, queues.removeIf { it.args === cancel })
        assertSame(keep, iterator.next().args)
        iterator.remove()
        assertFalse(iterator.hasNext())
        assertEquals(0, queues.size)
        assertEquals(0, queues.strongQueues)
        iterator.cleanUp()
    }

    @Test
    fun `removing the current node cannot remove it twice or skip its successor`() {
        val queues = PlayerQueueList()
        val cancel = Any()
        val keep = Any()
        queues.add("queue.hit", QueueCategory.Strong, 1, cancel)
        queues.add("queue.hit", QueueCategory.Strong, 1, keep)

        val iterator = requireNotNull(queues.iterator())
        assertSame(cancel, iterator.next().args)
        assertEquals(1, queues.removeIf { it.args === cancel })
        assertThrows(IllegalStateException::class.java) { iterator.remove() }
        assertSame(keep, iterator.next().args)
        assertEquals(1, queues.size)
        assertEquals(1, queues.strongQueues)
        iterator.cleanUp()
    }

    @Test
    fun `removing all pending queues exhausts an active iterator and permits reuse`() {
        val queues = PlayerQueueList()
        queues.add("queue.hit", QueueCategory.Strong, 1)
        queues.add("queue.hit", QueueCategory.Normal, 1)
        queues.add("queue.hit", QueueCategory.Strong, 1)
        val iterator = requireNotNull(queues.iterator())
        iterator.next()

        assertEquals(3, queues.removeIf { true })
        assertFalse(iterator.hasNext())
        assertEquals(0, queues.size)
        assertEquals(0, queues.strongQueues)
        assertEquals(0, queues.removeIf { true })
        iterator.cleanUp()

        val freshHit = Any()
        queues.add("queue.hit", QueueCategory.Strong, 2, freshHit)
        assertSame(freshHit, requireNotNull(queues.iterator()).next().args)
        assertEquals(1, queues.size)
        assertEquals(1, queues.strongQueues)
    }

    @Test
    fun `removing the tail keeps retained order when another queue is appended`() {
        val queues = PlayerQueueList()
        val first = Any()
        val cancelledTail = Any()
        val appended = Any()
        queues.add("queue.hit", QueueCategory.Normal, 1, first)
        queues.add("queue.hit", QueueCategory.Strong, 1, cancelledTail)

        assertEquals(1, queues.removeIf { it.args === cancelledTail })
        queues.add("queue.hit", QueueCategory.Strong, 1, appended)
        val iterator = requireNotNull(queues.iterator())
        assertSame(first, iterator.next().args)
        assertSame(appended, iterator.next().args)
        assertFalse(iterator.hasNext())
        assertEquals(2, queues.size)
        assertEquals(1, queues.strongQueues)
        iterator.cleanUp()
    }

    private data class Marker(val value: Int)

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
