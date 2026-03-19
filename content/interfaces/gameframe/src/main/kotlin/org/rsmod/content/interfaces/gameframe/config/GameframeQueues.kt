package org.rsmod.content.interfaces.gameframe.config

import dev.openrune.queue

typealias gameframe_queues = GameframeQueues

object GameframeQueues {
    val client_mode = queue("client_mode")
}
