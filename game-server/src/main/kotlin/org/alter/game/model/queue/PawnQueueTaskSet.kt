package org.alter.game.model.queue

import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.rsmod.coroutine.GameCoroutine

class PawnQueueTaskSet(val pawn: Pawn) : GameQueueSet() {

    override fun processScript(task: GameCoroutine): Boolean {

        if (currPriority == QueueType.Normal && pawn.hasMenuOpen()) {
            return true
        }

        task.advance()

        return task.isSuspended
    }

    override fun terminateTasks() {
        terminate(pawn)
    }

    private fun Pawn.hasMenuOpen(): Boolean = this is Player && world.plugins.isMenuOpened(this)


}