package org.rsmod.tools.wiki.dumping

import org.rsmod.tools.mcp.wiki.GameValTool

/** Wiki npc id → `npc.*` gameval key via loaded [GameValProvider]. */
class NpcRscmLookup private constructor(
    private val gameVals: GameValTool,
) {
    fun toRscm(npcId: Int): String? = gameVals.reverseLookupNpc(npcId).minOrNull()

    fun toRscmList(npcIds: Iterable<Int>): Pair<List<String>, List<Int>> {
        val mapped = mutableListOf<String>()
        val unmapped = mutableListOf<Int>()
        for (id in npcIds) {
            val key = toRscm(id)
            if (key != null) {
                mapped += key
            } else {
                unmapped += id
            }
        }
        return mapped.distinct() to unmapped
    }

    companion object {
        fun load(rootDir: String? = null): NpcRscmLookup = NpcRscmLookup(GameValTool.load(rootDir))
    }
}
