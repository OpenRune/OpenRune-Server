package org.rsmod.content.generic.npcs.person

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType

internal typealias person_npcs = PersonNpcs

internal object PersonNpcs : NpcReferences() {
    val man = npc("man")
    val man2 = npc("man2")
    val man3 = npc("man3")
    val man_indoor = npc("man_indoor")
    val woman = npc("woman")
    val woman2 = npc("woman2")
    val woman3 = npc("woman3")
}

internal object PersonNpcEdits : NpcEditor() {
    init {
        val people =
            setOf(
                person_npcs.man,
                person_npcs.man2,
                person_npcs.man3,
                person_npcs.man_indoor,
                person_npcs.woman,
                person_npcs.woman2,
                person_npcs.woman3,
            )
        people.forEach(::person)
    }

    private fun person(type: NpcType) {
        edit(type) { contentGroup = content.person }
    }
}
