package org.alter.game.model.queue.impl

import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTaskSet
import org.alter.game.model.queue.TaskPriority
import kotlin.coroutines.resume

/**
 * A [QueueTaskSet] implementation for [org.alter.game.model.entity.Pawn]s.
 * Each [org.alter.game.model.queue.QueueTask] is handled one at a time.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class PawnQueueTaskSet : QueueTaskSet() {
    override fun cycle() {
        // Always process tasks, even if queue appears empty (tasks might be suspended)
        if (queue.isEmpty()) {
            return
        }

        loop@ while (true) {
            val task = queue.peekFirst() ?: break@loop

            if (task.priority == TaskPriority.STANDARD && task.ctx is Player && task.ctx.hasMenuOpen()) {
                break@loop
            }

            if (!task.invoked) {
                task.invoked = true
                task.coroutine.resume(Unit)
            }

            // Always call task.cycle() to check if suspended conditions are met
            task.cycle()

            if (!task.suspended()) {
                /*
                 * Task is no longer in a suspended state, which means its job is
                 * complete.
                 */
                queue.remove(task)
                /*
                 * Since this task is complete, let's handle any upcoming
                 * task now instead of waiting until next cycle.
                 */
                continue@loop
            }
            // Task is still suspended - keep it in queue so it gets checked next cycle
            break@loop
        }
    }

    private fun Player.hasMenuOpen(): Boolean = world.plugins.isMenuOpened(this)
}
