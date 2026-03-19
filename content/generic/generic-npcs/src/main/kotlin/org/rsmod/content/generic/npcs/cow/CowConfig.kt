package org.rsmod.content.generic.npcs.cow

import dev.openrune.npc
import dev.openrune.queue

internal typealias cow_npcs = CowNpcs

internal typealias cow_queues = CowQueues

object CowNpcs {
    val gillie_the_milkmaid = npc("gillie_the_milkmaid")
    val cow = npc("cow")
    val cow2 = npc("cow2")
    val cow3 = npc("cow3")
    val cow_beef = npc("cow_beef")
    val cow2_calf = npc("cow2_calf")
    val cow3_calf = npc("cow3_calf")
}

object CowQueues {
    val milk = queue("milk_cow")
}
