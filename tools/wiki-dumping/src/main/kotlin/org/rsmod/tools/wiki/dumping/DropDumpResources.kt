package org.rsmod.tools.wiki.dumping

/** Preloaded lookups shared across all wiki pages in one dumper run. */
class DropDumpResources private constructor(
    val objLookup: ObjRscmLookup,
    val npcLookup: NpcRscmLookup,
    val remainsLookup: NpcDumpRemainsLookup,
    val dumpIndex: NpcDumpIndex,
) {
    companion object {
        suspend fun load(
            rootDir: String?,
            log: DropDumpLog,
            fetchDumpNpc: Boolean = false,
        ): DropDumpResources {
            log.phase("load GameValProvider") {
                GameValLoader.ensureLoaded(rootDir)
            }

            val objLookup = ObjRscmLookup()

            lateinit var dump: NpcDumpFiles.LoadedDump
            log.phaseAsync("load dump.npc") {
                dump = NpcDumpFiles.readOrFetch(rootDir, fetchDumpNpc)
                val source = dump.source?.toAbsolutePath() ?: "memory"
                val action = if (dump.downloaded) "downloaded to" else "read"
                log.verbose("$action $source (${dump.text.length} bytes)")
            }

            lateinit var dumpIndex: NpcDumpIndex
            log.phase("index dump.npc") {
                dumpIndex = NpcDumpIndex.parse(dump.text, objLookup)
                log.verbose(
                    "dump.npc index: ${dumpIndex.namesById.size} names, " +
                        "${dumpIndex.remainsByNpcId.size} explicit remains",
                )
            }

            val npcLookup = NpcRscmLookup(dumpIndex.namesById)
            val remainsLookup = NpcDumpRemainsLookup(dumpIndex.remainsByNpcId)

            return DropDumpResources(
                objLookup = objLookup,
                npcLookup = npcLookup,
                remainsLookup = remainsLookup,
                dumpIndex = dumpIndex,
            )
        }
    }
}
