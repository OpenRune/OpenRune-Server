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

    const val SKIPPED_MONSTERS_MANIFEST = "_skipped_monsters.txt"
    const val UNKNOWN_DROP_RATES_MANIFEST = "_unknown_drop_rates.txt"
    const val DUPLICATE_DROP_TABLES_MANIFEST = "_duplicate_drop_tables.txt"

    private val LEGACY_MANIFEST_FILES =
        setOf(SKIPPED_MONSTERS_MANIFEST, UNKNOWN_DROP_RATES_MANIFEST, DUPLICATE_DROP_TABLES_MANIFEST)

    fun defaultManifestOutputDir(repoRoot: Path?): Path =
        if (repoRoot != null) {
            repoRoot.resolve("content/drops/src/main/resources/drops/tables")
        } else {
            Path.of("content/drops/src/main/resources/drops/tables")
        }

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

    /** Removes wiki dump manifest files accidentally written under the Kotlin monsters dir. */
    fun cleanupLegacyManifestFiles(kotlinTablesRoot: Path, log: DropDumpLog) {
        val monstersRoot = kotlinTablesRoot.resolve(MONSTERS_DIR)
        if (!Files.isDirectory(monstersRoot)) {
            return
        }

        for (name in LEGACY_MANIFEST_FILES) {
            val file = monstersRoot.resolve(name)
            if (file.deleteIfExists()) {
                log.verbose("removed legacy manifest ${file.fileName} from Kotlin output")
            }
        }
    }
}
