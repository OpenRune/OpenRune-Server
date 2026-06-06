package org.rsmod.tools.wiki.dumping

/** Parsed Joshua-F [dump.npc] index (npc id → internal name / remains). */
data class NpcDumpIndex(
    val namesById: Map<Int, String>,
    val remainsByNpcId: Map<Int, String>,
) {
    companion object {
        private val npcIdLine = Regex("""^//\s*(\d+)\s*$""")
        private val npcNameLine = Regex("""^\[([^\]]+)]$""")
        private val remainsParam = Regex("""^param=param_46,(.+)$""")

        fun parse(dumpText: String, objLookup: ObjRscmLookup): NpcDumpIndex {
            val names = mutableMapOf<Int, String>()
            val remains = mutableMapOf<Int, String>()
            var currentId: Int? = null
            var currentRemains: String? = null

            fun flushRemains() {
                val id = currentId ?: return
                val raw = currentRemains ?: return
                remains[id] = toObjKey(raw, objLookup)
            }

            for (rawLine in dumpText.lineSequence()) {
                val line = rawLine.trim()
                npcIdLine.matchEntire(line)?.let { match ->
                    flushRemains()
                    currentId = match.groupValues[1].toIntOrNull()
                    currentRemains = null
                    return@let
                }
                npcNameLine.matchEntire(line)?.let { match ->
                    val id = currentId ?: return@let
                    names[id] = match.groupValues[1].trim()
                }
                remainsParam.matchEntire(line)?.let { match ->
                    currentRemains = match.groupValues[1].trim()
                }
            }
            flushRemains()
            return NpcDumpIndex(names, remains)
        }

        private fun toObjKey(dumpName: String, objLookup: ObjRscmLookup): String {
            val normalized = dumpName.lowercase()
            objLookup.resolveByDisplayName(normalized.replace('_', ' '))?.let { return it }
            objLookup.resolveByDisplayName(dumpName)?.let { return it }
            return if (normalized.startsWith("obj.")) normalized else "obj.$normalized"
        }
    }
}
