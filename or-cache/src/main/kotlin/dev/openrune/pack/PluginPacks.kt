package dev.openrune.pack

import dev.openrune.DirectoryConstants
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.gamevals.GameValProvider
import dev.openrune.cache.tools.cs2.SymbolsCustomConflictStrip
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.cache.tools.tasks.impl.PackSprites
import io.github.classgraph.ClassGraph
import java.io.File


class PluginPacks(val projectRoot: File, val all: List<PluginPack>) {
    val active: List<PluginPack> = all.filter { it.shouldPack(projectRoot) }

    fun nameOf(pack: PluginPack): String =
        pack::class.java.name.substringAfterLast('.').removeSuffix("PluginPack").lowercase()

    fun configDirectories(): List<File> = all.mapNotNull { it.configDirectory() }

    fun validate() {
        active.forEach { it.validate(projectRoot) }
    }

    fun buildPackTasks(baseTables: List<DBTable>): List<CacheTask> {
        val tasks = mutableListOf(
            PackModels(File("../.data/raw-cache/models")),
            PackConfig(File("../.data/raw-cache/")),
        )

        configDirectories().forEach { tasks += PackConfig(it) }
        active.mapNotNull { it.modelDirectory() }.forEach { tasks += PackModels(it) }

        val legacySprites = File("../.data/raw-cache/sprites")
        if (legacySprites.isDirectory) {
            tasks += PackSprites(legacySprites)
        }
        active.mapNotNull { it.spriteDirectory() }.forEach { tasks += PackSprites(it) }

        tasks += active.flatMap { it.extraTasks() }


        val interfaces = active.flatMap { it.interfaces() }
        if (interfaces.isNotEmpty()) {
            tasks += PackIfType(interfaces)
        }

        tasks += UnpackDefaultCs2(DirectoryConstants.CS2_PATH.toFile())
        tasks += PackCs2(DirectoryConstants.CS2_PATH.toFile())

        val tables = baseTables + active.flatMap { it.dbTables() }
        tasks += PackDBTables(tables)

        return tasks
    }

    fun syncCs2(cs2Root: File, gamevals: GameValProvider? = null) {
        if (gamevals == null && all.none { it.cs2Directory() != null }) {
            return
        }

        cs2Root.mkdirs()

        val customRoot = File(cs2Root, "custom")
        val symbolsCustom = File(cs2Root, "symbols_custom")
        customRoot.deleteRecursively()
        symbolsCustom.deleteRecursively()
        customRoot.mkdirs()
        symbolsCustom.mkdirs()

        // Emit first so hand-written pack symbol files take precedence on overlap.
        gamevals?.let { writeCustomGamevalSymbols(symbolsCustom, it) }

        for (pack in active) {
            val source = pack.cs2Directory() ?: continue
            val scriptDest = File(customRoot, nameOf(pack)).also { it.mkdirs() }

            copyScripts(source, scriptDest)
            symbolFiles(source).forEach { sym ->
                mergeSymbolLines(File(symbolsCustom, sym.name), readSymbolLines(sym))
            }
        }

        SymbolsCustomConflictStrip.strip(cs2Root)
    }

    /**
     * Custom gamevals (ids above the OSRS cache max for their table) are owned by this project, not
     * by the cache. `SymDumper` re-emits them into `symbols/` on every build and only ever appends
     * to some of those files, so a renumbered gameval leaves the previous id behind and Neptune
     * fails with a duplicate symbol name. Staging them under `symbols_custom/` - which is wiped and
     * rebuilt each run - gives them a single authoritative home, and lets
     * [SymbolsCustomConflictStrip] drop every stale copy from `symbols/`.
     */
    private fun writeCustomGamevalSymbols(symbolsCustom: File, gamevals: GameValProvider) {
        for ((table, symbolFile) in SYMBOL_FILES) {
            val entries = gamevals.mappings[table] ?: continue
            val maxBaseId = gamevals.maxBaseID[table] ?: -1

            val custom =
                entries
                    .filterValues { it > maxBaseId }
                    .map { (key, id) -> id.toString() to key.removePrefix("$table.") }
                    .sortedBy { it.first.toIntOrNull() ?: 0 }

            mergeSymbolLines(File(symbolsCustom, symbolFile), custom)
        }
    }

    companion object {
        private val SCANNED_PACKAGES = arrayOf("dev.openrune.pack", "org.rsmod.content")

        private val SYMBOL_LINE = Regex("""^\s*(\S+)\s+(.+?)\s*$""")

        /**
         * Gameval table -> Neptune symbol file. Only tables whose symbol lines are plain
         * `id<tab>name` with a plain integer id are listed. Left to their existing sources:
         * `clientscript` (needs the `[trigger,name]` form and is supplied by packs), `dbcol` and
         * `param` (trailing type column), and `component` (packed `iface:comp` ids on both fields).
         */
        private val SYMBOL_FILES = mapOf(
            "dbrow" to "dbrow.sym",
            "dbtable" to "dbtable.sym",
            "interface" to "interface.sym",
            "inv" to "inv.sym",
            "loc" to "loc.sym",
            "npc" to "npc.sym",
            "obj" to "obj.sym",
            "seq" to "seq.sym",
            "varbit" to "varbit.sym",
            "varp" to "varp.sym",
        )

        fun discover(projectRoot: File): PluginPacks = PluginPacks(projectRoot, loadPacks())

        private fun loadPacks(): List<PluginPack> =
            ClassGraph()
                .ignoreClassVisibility()
                .enableClassInfo()
                .disableNestedJarScanning()
                .disableModuleScanning()
                .acceptPackages(*SCANNED_PACKAGES)
                .scan()
                .use { result ->
                    result.getSubclasses(PluginPack::class.java).directOnly().map { info ->
                        info.loadClass(PluginPack::class.java).getConstructor().newInstance()
                    }
                }

        private fun copyScripts(dir: File, dest: File) {
            val scripts = File(dir, "script")
            if (scripts.isDirectory) {
                scripts.walkTopDown().filter { it.isCs2() }.forEach {
                    it.copyTo(File(dest, it.name), overwrite = true)
                }
            }
            dir.listFiles()?.filter { it.isCs2() }?.forEach {
                it.copyTo(File(dest, it.name), overwrite = true)
            }
        }

        private fun symbolFiles(dir: File): List<File> {
            val symbols = File(dir, "symbols")
            if (!symbols.isDirectory) {
                return emptyList()
            }
            return symbols.listFiles()?.filter { it.isFile && it.extension.equals("sym", true) }
                .orEmpty()
        }

        private fun File.isCs2(): Boolean = isFile && extension.equals("cs2", true)

        private fun readSymbolLines(file: File): List<Pair<String, String>> =
            file.readLines().mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
                val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@mapNotNull null
                match.groupValues[1] to match.groupValues[2].trim()
            }

        private fun symbolName(rest: String): String =
            rest.substringBefore('\t').substringBefore(' ').trim()

        private fun stripSymbolLines(target: File, owned: List<Pair<String, String>>) {
            if (!target.exists() || owned.isEmpty()) return

            val ownedIds = owned.map { it.first }.toSet()
            val ownedNames = owned.map { symbolName(it.second) }.toSet()
            val kept =
                target.readLines().filter { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) return@filter false
                    val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@filter true
                    match.groupValues[1] !in ownedIds && symbolName(match.groupValues[2]) !in ownedNames
                }

            if (kept.isEmpty()) {
                target.delete()
            } else {
                target.writeText(kept.joinToString("\n", postfix = "\n"))
            }
        }

        private fun mergeSymbolLines(target: File, lines: List<Pair<String, String>>) {
            if (lines.isEmpty()) return

            stripSymbolLines(target, lines)
            val existing = if (target.exists()) target.readText().trimEnd() else ""
            target.parentFile?.mkdirs()
            target.writeText(
                buildString {
                    if (existing.isNotBlank()) {
                        appendLine(existing)
                    }
                    appendLine(lines.joinToString("\n") { (id, rest) -> "$id\t$rest" })
                },
            )
        }
    }
}
