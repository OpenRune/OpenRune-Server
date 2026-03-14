package org.rsmod.content.interfaces.combat.tab.configs

import org.rsmod.api.type.refs.queue.QueueReferences

typealias combat_queues = CombatTabQueues

object CombatTabQueues : QueueReferences() {
    val sa_instant_spec = queue("sa_instant_spec")
    val attackstyle_change = queue("attackstyle_change")
}
