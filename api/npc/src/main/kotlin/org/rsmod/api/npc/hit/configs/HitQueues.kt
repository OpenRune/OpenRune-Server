package org.rsmod.api.npc.hit.configs

import dev.openrune.queue

internal typealias hit_queues = HitQueues

internal object HitQueues {
    val standard = queue("hit")
}
