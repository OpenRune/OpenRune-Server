package org.rsmod.tools.wiki.dumping

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

/** Output paths for wiki-generated monster drop tables under `tables/monsters/`. */
object DropTableOutputLayout {
    const val MONSTERS_DIR = "monsters"
    const val MONSTERS_PACKAGE = "org.rsmod.content.drops.tables.monsters"

    fun resolveMonsterOutputFile(tablesRoot: Path, wikiPage: String): Path =
        tablesRoot.resolve(MONSTERS_DIR).resolve(outputFileName(wikiPage))

    /** Removes monster drop files not rewritten in the latest bulk dump. */
    fun cleanupStaleMonsterFiles(tablesRoot: Path, writtenFiles: Set<Path>) {
        val monstersRoot = tablesRoot.resolve(MONSTERS_DIR)
        if (!Files.isDirectory(monstersRoot)) {
            return
        }

        monstersRoot.walk().filter { it.isRegularFile() && it.name.endsWith("DropTable.kt") }.forEach { file ->
            if (file !in writtenFiles) {
                file.deleteIfExists()
            }
        }
    }

    /** Removes grouped subdirectories under `monsters/` (legacy layout). */
    fun cleanupGroupedSubdirs(tablesRoot: Path) {
        val monstersRoot = tablesRoot.resolve(MONSTERS_DIR)
        if (!Files.isDirectory(monstersRoot)) {
            return
        }

        monstersRoot
            .toFile()
            .listFiles()
            ?.filter { it.isDirectory }
            ?.forEach { dir ->
                dir.deleteRecursively()
            }
    }

    /** Removes pre-grouping flat files from `tables/` (keeps `shared/` and `monsters/`). */
    fun cleanupLegacyFlatMonsterFiles(tablesRoot: Path) {
        if (!Files.isDirectory(tablesRoot)) {
            return
        }

        tablesRoot
            .toFile()
            .listFiles { file ->
                file.isFile &&
                    file.name.endsWith("DropTable.kt", ignoreCase = true) &&
                    !file.name.endsWith("EchoDropTable.kt", ignoreCase = true)
            }?.forEach { it.delete() }
    }

    fun cleanupStaleAlternateEncounterFiles(tablesRoot: Path, log: DropDumpLog) {
        val monstersRoot = tablesRoot.resolve(MONSTERS_DIR)
        if (!Files.isDirectory(monstersRoot)) {
            return
        }

        monstersRoot.walk().filter { it.isRegularFile() && it.name.endsWith("EchoDropTable.kt", ignoreCase = true) }.forEach { file ->
            if (file.deleteIfExists()) {
                log.verbose("removed stale ${file.name}")
            }
        }
    }
}
