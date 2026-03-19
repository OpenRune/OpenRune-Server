package org.rsmod.api.player.hit.configs

import dev.openrune.queue

internal typealias hit_queues = HitQueues

internal object HitQueues {
    val standard = queue("hit")
    val impact = queue("impact_hit")
}
