package dev.openrune.pack

import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.PackSprites
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.cache.tools.cs2.SymbolsCustomConflictStrip
import dev.openrune.getCs2Location
import io.github.classgraph.ClassGraph
import java.io.File


class PluginPacks(val projectRoot: File, val all: List<PluginPack>) {
    val active: List<PluginPack> = all.filter { it.shouldPack(projectRoot) }

    fun nameOf(pack: PluginPack): String =
        pack::class.java.name.substringAfterLast('.').removeSuffix("PluginPack").lowercase()

    fun configDirectories(): List<File> = all.mapNotNull { it.configDirectory() }

    fun buildPackTasks(baseTables: List<DBTable>): List<CacheTask> {
        val tasks = mutableListOf(
            PackModels(File("../.data/raw-cache/models")),
            PackConfig(File("../.data/raw-cache/server")),
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

        tasks += UnpackDefaultCs2(getCs2Location(), null, false)
        tasks += PackCs2(getCs2Location())

        val tables = baseTables + active.flatMap { it.dbTables() }
        tasks += PackDBTables(tables)

        return tasks
    }

    fun syncCs2(cs2Root: File) {
        if (all.none { it.cs2Directory() != null }) {
            return
        }

        cs2Root.mkdirs()
        val customRoot = File(cs2Root, "custom").also { it.mkdirs() }
        val symbolsCustom = File(cs2Root, "symbols_custom").also { it.mkdirs() }

        pruneStaleCs2(customRoot, symbolsCustom)

        for (pack in active) {
            val source = pack.cs2Directory() ?: continue
            val scriptDest =
                File(customRoot, nameOf(pack)).also {
                    it.deleteRecursively()
                    it.mkdirs()
                }

            copyScripts(source, scriptDest)
            symbolFiles(source).forEach { sym ->
                mergeSymbolLines(File(symbolsCustom, sym.name), readSymbolLines(sym))
            }
        }

        SymbolsCustomConflictStrip.strip(cs2Root)
    }

    private fun pruneStaleCs2(customRoot: File, symbolsCustom: File) {
        val activeNames = active.map { nameOf(it) }.toSet()
        val allNames = all.map { nameOf(it) }.toSet()

        customRoot.listFiles()?.forEach { child ->
            val name = child.name.lowercase()
            if (child.isDirectory && name in allNames && name !in activeNames) {
                child.deleteRecursively()
            }
        }
        File(customRoot, "_plugins").takeIf { it.exists() }?.deleteRecursively()
        File(symbolsCustom, "_plugins").takeIf { it.exists() }?.deleteRecursively()

        for (pack in all) {
            val source = pack.cs2Directory() ?: continue
            symbolFiles(source).forEach { sym ->
                stripSymbolLines(File(symbolsCustom, sym.name), readSymbolLines(sym))
            }
        }
    }

    companion object {
        private val SCANNED_PACKAGES = arrayOf("dev.openrune.pack", "org.rsmod.content")

        private val SYMBOL_LINE = Regex("""^\s*(\S+)\s+(.+?)\s*$""")

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
