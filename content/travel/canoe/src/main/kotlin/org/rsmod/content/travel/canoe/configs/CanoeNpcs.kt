package org.rsmod.content.travel.canoe.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType

typealias canoe_npcs = CanoeNpcs

object CanoeNpcs : NpcReferences() {
    val cave_scenery_1 = npc("canoeing_cave_scenery_1")
    val cave_scenery_2 = npc("canoeing_cave_scenery_2")
    val cave_scenery_3 = npc("canoeing_cave_scenery_3")

    val tree_scenery_1 = npc("canoeing_scenery_1")
    val tree_scenery_2 = npc("canoeing_scenery_2")
    val bullrush_scenery_1 = npc("canoeing_bullrush")
    val bullrush_scenery_2 = npc("canoeing_bullrush_leaf")
}

internal object CanoeNpcEditor : NpcEditor() {
    init {
        scenery(canoe_npcs.cave_scenery_1)
        scenery(canoe_npcs.cave_scenery_2)
        scenery(canoe_npcs.cave_scenery_3)

        scenery(canoe_npcs.tree_scenery_1)
        scenery(canoe_npcs.tree_scenery_2)
        scenery(canoe_npcs.bullrush_scenery_1)
        scenery(canoe_npcs.bullrush_scenery_2)
    }

    private fun scenery(npc: NpcType) {
        edit(npc) {
            defaultMode = none
            moveRestrict = passthru
            respawnDir = north
            timer = 1
        }
    }
}
