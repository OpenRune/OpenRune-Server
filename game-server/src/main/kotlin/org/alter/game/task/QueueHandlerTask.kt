package org.alter.game.task

import org.alter.game.model.World
import org.alter.game.service.GameService

/**
 * A [GameTask] responsible for going over all the active
 * [org.alter.game.model.queue.QueueTask]s.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class QueueHandlerTask : GameTask {
    override fun execute(
        world: World,
        service: GameService,
    ) {
        val worldQueues: Int = world.queues.size
        world.queues.cycle()

        var playerQueues = 0
        var npcQueues = 0
        world.players.forEach { playerQueues += it.queues.size }
        world.npcs.forEach { npcQueues += it.queues.size }

        service.totalPlayerQueues = playerQueues
        service.totalNpcQueues = npcQueues
        service.totalWorldQueues = worldQueues
    }
}
