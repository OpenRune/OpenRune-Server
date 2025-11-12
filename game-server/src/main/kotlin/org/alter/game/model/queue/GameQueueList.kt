package org.alter.game.model.queue

import org.rsmod.coroutine.GameCoroutine
import org.rsmod.coroutine.suspension.GameCoroutineSimpleCompletion
import java.util.*
import kotlin.coroutines.startCoroutine

class GameQueueList internal constructor(
    private val queues: MutableList<GameCoroutine> = mutableListOf(),
    private val pending: Queue<GameQueueBlock> = LinkedList()
) : List<GameCoroutine> by queues {

    internal fun queue(block: suspend GameCoroutine.() -> Unit) {
        val queueBlock = GameQueueBlock(block)
        pending.add(queueBlock)
    }

    internal fun cycle() {
        cycleQueues()
        addPending()
    }

    private fun cycleQueues() {
        queues.forEach { it.advance() }
        queues.removeIf { it.isIdle }
    }

    private fun addPending() {
        while (pending.isNotEmpty()) {
            val ctx = pending.poll() ?: break
            val coroutine = GameCoroutine()
            ctx.block.startCoroutine(coroutine, GameCoroutineSimpleCompletion)
            if(coroutine.isSuspended) {
                queues.add(coroutine)
            }
        }
    }
}
