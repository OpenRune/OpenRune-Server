package org.rsmod.tools.wiki.dumping

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType

/** Wiki npc id → `npc.*` gameval key via gamevals and Joshua-F [dump.npc] fallback. */
class NpcRscmLookup(
    private val dumpNamesById: Map<Int, String> = emptyMap(),
) {
    fun toRscm(npcId: Int): String? {
        reverseNpc(npcId)?.let { return it }

        val dumpName = dumpNamesById[npcId] ?: return null
        return lookupNpcKey(dumpName) ?: "npc.$dumpName"
    }

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

    private fun reverseNpc(npcId: Int): String? = reverseMapping(RSCMType.NPC, npcId)

    private fun lookupNpcKey(name: String): String? {
        val fullKey = if (name.startsWith("npc.")) name else "npc.$name"
        return if (hasMapping(fullKey)) fullKey else null
    }

    private fun reverseMapping(type: RSCMType, id: Int): String? {
        val mapped =
            runCatching { RSCM.getReverseMapping(type, id) }
                .getOrNull()
                ?.trim()
                .orEmpty()
        if (mapped.isBlank() || mapped == "-1") {
            return null
        }
        return if (mapped.contains('.')) mapped else "${type.prefix}.$mapped"
    }

    private fun hasMapping(fullKey: String): Boolean =
        runCatching {
            RSCM.getRSCM(fullKey)
            true
        }.getOrDefault(false)

    companion object {
        /** Sync load for tools that only need npc RSCM (e.g. slayer dumper). */
        fun load(rootDir: String? = null): NpcRscmLookup {
            GameValLoader.ensureLoaded(rootDir)
            val dump = NpcDumpFiles.requireLocal(rootDir)
            val objLookup = ObjRscmLookup.load(rootDir)
            val dumpIndex = NpcDumpIndex.parse(dump.text, objLookup)
            return NpcRscmLookup(dumpIndex.namesById)
        }
    }
}
