package org.alter.game.model.queue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.model.entity.Pawn
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.coroutine.suspension.GameCoroutineSimpleCompletion
import java.util.*
import kotlin.coroutines.startCoroutine

private const val MAX_ACTIVE_QUEUES = 2

class GameQueueStack(
    private val pawn: Pawn,
    private var currQueue: GameCoroutine? = null,
    private var currPriority: QueueType = QueueType.Weak,
    private val pendQueue: LinkedList<GameQueueBlock> = LinkedList()
) : QueueStack {

    override val size: Int
        get() = pendQueue.size + (if (currQueue != null) 1 else 0)

    private val idle: Boolean
        get() = currQueue == null

    override fun queue(type: QueueType, block: suspend GameCoroutine.() -> Unit) {
        if (!overtakeQueues(type)) {
            return
        }
        if (size >= MAX_ACTIVE_QUEUES) {
            pendQueue.removeLast()
        }
        val queueBlock = GameQueueBlock(block)
        pendQueue.add(queueBlock)
    }

    override fun cycle() {
        /* flag whether or not a new queue should be polled this cycle */
        val pollQueue = idle
        try {
            processCurrent()
        } catch (t: Throwable) {
            discardCurrent()
            logger.error(t) { "Queue process error ($this)" }
        }
        if (pollQueue) {
            try {
                pollPending()
            } catch (t: Throwable) {
                logger.error(t) { "Queue poll error ($this)" }
            }
        }
    }

    internal fun clear() {
        currQueue = null
        currPriority = QueueType.Weak
        pendQueue.clear()
    }

    private fun processCurrent() {
        val queue = currQueue ?: return
        queue.advance()
        if (queue.isIdle) {
            discardCurrent()
        }
    }

    private fun pollPending() {
        if (currQueue != null) return
        val ctx = pendQueue.poll() ?: return

        val coroutine = GameCoroutine()
        ctx.block.startCoroutine(coroutine, GameCoroutineSimpleCompletion)

        currQueue = coroutine
    }

    fun submitEvent(value: Any) {
        val queue = currQueue ?: return
        queue.resumeWith(value)
    }

    private fun discardCurrent() {
        currQueue = null
        /* only reset priority if no other queue is pending */
        if (pendQueue.isEmpty()) {
            currPriority = QueueType.Weak
        }
    }

    private fun overtakeQueues(priority: QueueType): Boolean {
        if (priority == currPriority) {
            return true
        }
        if (!priority.overtake(currPriority)) {
            return false
        }
        if (priority != currPriority) {
            clear()
            currPriority = priority
        }
        return true
    }

    private fun QueueType.overtake(other: QueueType): Boolean = when (this) {
        QueueType.Normal -> other == QueueType.Weak
        QueueType.Strong -> true
        else -> false
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun submitReturnValue(value: Any) {
        submitEvent(value)
    }

    override fun terminateTasks() {
        clear()
    }
}
