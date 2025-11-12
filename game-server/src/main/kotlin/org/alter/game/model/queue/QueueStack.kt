package org.alter.game.model.queue

import org.rsmod.coroutine.GameCoroutine

interface QueueStack {
    val size: Int

    fun cycle()
    fun queue(priority: QueueType, block: suspend GameCoroutine.() -> Unit)
    fun submitReturnValue(value: Any)
    fun terminateTasks()
}