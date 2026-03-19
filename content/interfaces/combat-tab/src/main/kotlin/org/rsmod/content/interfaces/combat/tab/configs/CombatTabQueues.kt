package org.rsmod.content.interfaces.combat.tab.configs

import dev.openrune.queue

typealias combat_queues = CombatTabQueues

object CombatTabQueues {
    val sa_instant_spec = queue("sa_instant_spec")
    val attackstyle_change = queue("attackstyle_change")
}
