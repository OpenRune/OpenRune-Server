package org.rsmod.content.slayer.requirements

import org.rsmod.api.table.slayer.SlayerMonstersRow
import org.rsmod.game.entity.Npc

internal object SlayerMonsters {
    private val rowsByNpcId: Map<Int, SlayerMonstersRow> by lazy {
        buildMap {
            for (row in SlayerMonstersRow.all()) {
                for (type in row.npcs) {
                    put(type.id, row)
                }
            }
        }
    }

    fun find(npc: Npc): SlayerMonstersRow? = find(npc.visType.id) ?: find(npc.type.id)

    fun find(npcId: Int): SlayerMonstersRow? = rowsByNpcId[npcId]
}
