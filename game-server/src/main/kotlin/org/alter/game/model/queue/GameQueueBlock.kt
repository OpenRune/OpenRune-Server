package org.alter.game.model.queue

import org.rsmod.coroutine.GameCoroutine

@JvmInline
value class GameQueueBlock(val block: suspend GameCoroutine.() -> Unit)
