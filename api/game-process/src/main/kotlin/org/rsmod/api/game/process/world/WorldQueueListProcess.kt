package org.rsmod.api.game.process.world

import jakarta.inject.Inject
import org.rsmod.game.queue.WorldQueueList

public class WorldQueueListProcess @Inject constructor(private val queues: WorldQueueList) {
    public fun process() {
        if (queues.isNotEmpty) {
            queues.process()
        }
    }

    private fun WorldQueueList.process() {
        decrementDelays()
        fireExpired()
    }

    private fun WorldQueueList.decrementDelays() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            iterator.next().remainingCycles--
        }
        iterator.cleanUp()
    }

    private fun WorldQueueList.fireExpired() {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val queue = iterator.next()
            if (queue.remainingCycles <= 0) {
                iterator.remove()
                queue.action()
            }
        }
        iterator.cleanUp()
    }
}
