package org.rsmod.api.hit.plugin

import dev.openrune.queue
import dev.openrune.types.aconverted.QueueType

public typealias hit_queues = HitQueues

public object HitQueues {
    public val standard: QueueType = queue("hit")
    public val impact: QueueType = queue("impact_hit")
}
