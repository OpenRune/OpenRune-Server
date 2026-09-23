package dev.openrune.pack

import dev.openrune.DirectoryConstants
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.PackWorldMap
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.gamevals.GameValProvider
import dev.openrune.cache.tools.cs2.Cs2Overrides
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.cache.tools.tasks.impl.PackSprites
import io.github.classgraph.ClassGraph
import java.io.File


class PluginPacks(val projectRoot: File, val all: List<PluginPack>) {
    val active: List<PluginPack> = all.filter { it.shouldPack(projectRoot) }

    private var cachedInterfaceTasks: List<PackIfType>? = null

    fun nameOf(pack: PluginPack): String =
        pack::class.java.name.substringAfterLast('.').removeSuffix("PluginPack").lowercase()

    fun configDirectories(): List<File> = all.mapNotNull { it.configDirectory() }

    fun validate() {
        active.forEach { it.validate(projectRoot) }
    }

    // Cached: PackIfType consumes the DSL's inherit/edit/from registrations at construction time, so a
    // second set would be built empty.
    fun interfaceTasks(): List<PackIfType> {
        cachedInterfaceTasks?.let { return it }

        val dslInterfaces = active.flatMap { it.interfaces() }
        val interfaceDirectories = active.mapNotNull { it.interfaceDirectory() }
        val tasks =
            if (dslInterfaces.isEmpty() && interfaceDirectories.isEmpty()) {
                emptyList()
            } else {
                listOf(PackIfType(dslInterfaces, interfaceDirectories, DirectoryConstants.CS2_PATH.toFile()))
            }

        cachedInterfaceTasks = tasks
        return tasks
    }

    fun buildPackTasks(baseTables: List<DBTable>, cs2Overrides: Cs2Overrides = Cs2Overrides()): List<CacheTask> {
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

        tasks += interfaceTasks()

        tasks += UnpackDefaultCs2(DirectoryConstants.CS2_PATH.toFile())
        tasks += PackCs2(DirectoryConstants.CS2_PATH.toFile(), cs2Overrides)

        val tables = baseTables + active.flatMap { it.dbTables() }
        tasks += PackDBTables(tables)

        tasks += PackWorldMap()
        return tasks
    }

    /**
     * Everything the packs contribute to the CS2 build, handed to Neptune in place rather than
     * copied under `custom/`: each pack's script directory and loose `.cs2`
     * files as sources and its `symbols` directory of `.sym` lines keyed by table. Later packs win on a symbol
     * name, and pack symbols win over the gameval-derived ones; all of them win over the dumped
     * `symbols/` files.
     */
    fun cs2Overrides(cs2Root: File, gamevals: GameValProvider? = null): Cs2Overrides {
        cs2Root.mkdirs()
        // leftover from when scripts were copied into the project
        File(cs2Root, "custom").deleteRecursively()

        val sources = mutableListOf<File>()
        val symbols = mutableMapOf<String, LinkedHashMap<String, String>>()
        fun put(table: String, id: String, rest: String) {
            val byName = symbols.getOrPut(table) { LinkedHashMap() }
            byName[symbolName(rest)] = "$id\t$rest"
        }

        gamevals?.let { provider ->
            for ((table, symTable) in SYMBOL_TABLES) {
                val entries = provider.mappings[table] ?: continue
                val maxBaseId = provider.maxBaseID[table] ?: -1
                entries
                    .filterValues { it > maxBaseId }
                    .entries
                    .sortedBy { it.value }
                    .forEach { (key, id) -> put(symTable, symbolId(table, id), key.removePrefix("$table.")) }
            }
        }

        for (pack in active) {
            val dir = pack.cs2Directory() ?: continue
            File(dir, "script").takeIf { it.isDirectory }?.let(sources::add)
            dir.listFiles()?.filter { it.isCs2() }?.forEach(sources::add)
            symbolFiles(dir).forEach { sym ->
                readSymbolLines(sym).forEach { (id, rest) -> put(sym.nameWithoutExtension, id, rest) }
            }
        }

        return Cs2Overrides(sources, symbols.mapValues { (_, byName) -> byName.values.toList() })
    }

    companion object {
        private val SCANNED_PACKAGES = arrayOf("dev.openrune.pack", "org.rsmod.content")

        private val SYMBOL_LINE = Regex("""^\s*(\S+)\s+(.+?)\s*$""")

        /**
         * Gameval table -> Neptune symbol table, for every table whose symbol line is just
         * `id<TAB>name`. These come straight from the gamevals, so packs no longer carry `.sym`
         * copies of them. Left to the packs' own symbol files because the gamevals cannot express
         * them: `varp`, `varc`, `param`, `dbcolumn` and `if_script` (trailing type column),
         * plus `clientscript` and `commands`.
         */
        private val SYMBOL_TABLES = mapOf(
            "area" to "area",
            "bas" to "bas",
            "category" to "category",
            "component" to "component",
            "dbrow" to "dbrow",
            "dbtable" to "dbtable",
            "enum" to "enum",
            "font" to "fontmetrics",
            "headbar" to "headbar",
            "hitmark" to "hitmark",
            "interface" to "interface",
            "inv" to "inv",
            "jingle" to "jingle",
            "loc" to "loc",
            "midi" to "midi",
            "models" to "model",
            "npc" to "npc",
            "obj" to "obj",
            "seq" to "seq",
            "spotanim" to "spotanim",
            "sprites" to "graphic",
            "stat" to "stat",
            "struct" to "struct",
            "synth" to "synth",
            "varbit" to "varbit",
        )

        private fun symbolId(table: String, id: Int): String =
            if (table == "component") "${id ushr 16}:${id and 0xffff}" else id.toString()

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
    }
}
