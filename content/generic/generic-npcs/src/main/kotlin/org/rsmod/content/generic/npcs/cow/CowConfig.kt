package org.rsmod.content.generic.npcs.cow

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.game.type.npc.NpcType

internal typealias cow_npcs = CowNpcs

internal typealias cow_queues = CowQueues

internal typealias cow_locs = CowLocs

object CowNpcs : NpcReferences() {
    val gillie_the_milkmaid = npc("gillie_the_milkmaid")
    val cow = npc("cow")
    val cow2 = npc("cow2")
    val cow3 = npc("cow3")
    val cow_beef = npc("cow_beef")
    val cow2_calf = npc("cow2_calf")
    val cow3_calf = npc("cow3_calf")
}

object CowQueues : QueueReferences() {
    val milk = queue("milk_cow")
}

internal object CowLocs : LocReferences() {
    val fat_cow = loc("fat_cow")
}

internal object CowLocEdits : LocEditor() {
    init {
        edit(cow_locs.fat_cow) { contentGroup = content.dairy_cow }
    }
}

internal object CowNpcEdits : NpcEditor() {
    init {
        cow(cow_npcs.cow)
        cow(cow_npcs.cow2)
        cow(cow_npcs.cow3)
        cow(cow_npcs.cow_beef)
        calf(cow_npcs.cow2_calf)
        calf(cow_npcs.cow3_calf)
    }

    private fun cow(type: NpcType) {
        edit(type) {
            contentGroup = content.cow
            timer = 1
        }
    }

    private fun calf(type: NpcType) {
        edit(type) {
            contentGroup = content.cow_calf
            timer = 1
        }
    }
}
