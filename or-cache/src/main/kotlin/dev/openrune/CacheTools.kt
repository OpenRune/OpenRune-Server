package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.cs2.SymbolsCustomConflictStrip
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.PackSprites
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.codegen.startEnumGeneration
import dev.openrune.codegen.startGeneration
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.filesystem.Cache
import dev.openrune.gamevals.GameValProvider
import dev.openrune.gamevals.GamevalDumper
import dev.openrune.impl.GameframeTable
import dev.openrune.impl.Music
import dev.openrune.map.packing.MapPackers
import dev.openrune.tables.DidYouKnow
import dev.openrune.tables.InstanceSettingsTable
import dev.openrune.tables.PickableObjects
import dev.openrune.tables.SettingConfigs
import dev.openrune.tables.ShopCurrencyTable
import dev.openrune.tables.StatComponents
import dev.openrune.tables.skills.Cooking
import dev.openrune.tables.skills.Firemaking
import dev.openrune.tables.skills.Herblore
import dev.openrune.tables.skills.Mining
import dev.openrune.tables.skills.Runecrafting
import dev.openrune.tables.skills.ShootingStars
import dev.openrune.tables.skills.Slayer
import dev.openrune.tables.skills.Smithing
import dev.openrune.tables.skills.prayer.EctofuntusBonemeal
import dev.openrune.tables.skills.prayer.PrayerBlessedBone
import dev.openrune.tables.skills.prayer.PrayerTable
import dev.openrune.tools.MinifyServerCache
import dev.openrune.tools.PackServerConfig
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.pack.PluginPack
import dev.openrune.pack.PluginPackLoader
import dev.openrune.packscript.PackScriptPhase
import dev.openrune.packscript.PackScripts
import dev.openrune.tables.consumables.food.FoodTable
import dev.openrune.tables.consumables.potion.PotionEffectTable
import dev.openrune.tables.consumables.potion.PotionTable
import java.io.File
import kotlin.system.exitProcess

fun getCacheLocation() = File("../.data/", "cache/LIVE").path

fun getServerCacheLocation() = File("../.data/", "cache/SERVER").path

val revision : Triple<Int, Int, String> = readRevision()

private val logger = InlineLogger()

val VARBIT = CacheVarLiteral.registerExternal(254, ']', name = "VARBIT")

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <buildType>")
        exitProcess(1)
    }

    CacheVarLiteral.registerExternal(253, '[', name = "PROJANIM")

    downloadRev(TaskType.valueOf(args.first().uppercase()))
}


fun tablesToPack() = listOf(
    GameframeTable.gameframe(),
    Music.musicClassic(),
    Music.musicModern(),
    Firemaking.logs(),
    Firemaking.firelighters(),
    Firemaking.sources(),
    PrayerTable.skillTable(),
    PrayerBlessedBone.table(),
    EctofuntusBonemeal.table(),
    StatComponents.statsComponents(),
    PickableObjects.pickableObjects(),
    Mining.rocks(),
    Cooking.foods(),
    Cooking.ales(),
    Herblore.unfinishedPotions(),
    Herblore.finishedPotions(),
    Herblore.cleaningHerbs(),
    Herblore.barbarianMixes(),
    Herblore.swampTar(),
    Herblore.crushing(),
    Smithing.bars(),
    Smithing.cannonBalls(),
    Smithing.dragonForge(),
    Smithing.crystalSinging(),
    Slayer.masters(),
    Runecrafting.altars(),
    Runecrafting.runes(),
    Runecrafting.tiara(),
    Runecrafting.combo(),
    FoodTable.table(),
    PotionEffectTable.table(),
    PotionTable.table(),
    SettingConfigs.settings(),
    DidYouKnow.didYouknow(),
    InstanceSettingsTable.instanceSettings(),
    ShootingStars.locations(),
    ShopCurrencyTable.shopCurrencies(),
)

fun downloadRev(type: TaskType) {

    logger.info { "Using Revision: $revision" }

    when (type) {
        TaskType.FRESH_INSTALL -> {

            val builder =
                Builder(
                    type = TaskType.FRESH_INSTALL,
                    cacheLocation = File(getCacheLocation()),
                    serverCacheLocation = File(getServerCacheLocation()),
                )
            builder.revision(revision.first)
            builder.subRevision(revision.second)
            builder.removeXteas(false)
            builder.environment(CacheEnvironment.valueOf(revision.third))

            builder.build().initialize()

            File(getServerCacheLocation(), "xteas.json").delete()

            val cache = Cache.load(File(getCacheLocation()).toPath())

            GamevalDumper.dumpGamevals(cache, revision.first)

            buildCache(TaskType.BUILD)
        }
        TaskType.SERVER_CACHE_BUILD -> buildCache(TaskType.SERVER_CACHE_BUILD)
        TaskType.BUILD -> buildCache(TaskType.BUILD)
    }
}

fun buildCache(taskType: TaskType) {
    GameValProvider.load("../", autoAssignIds = true)

    val projectRoot = File("..")
    val pluginPacks = PluginPackLoader.load()
    val activePacks = pluginPacks.filter { it.shouldPack(projectRoot) }
    val disabledPacks = pluginPacks.filter { !it.shouldPack(projectRoot) }

    val pluginTables =
        activePacks.flatMap { it.dbTables() } + disabledPacks.flatMap { it.dbTablesWhenDisabled() }
    val pluginInterfaces = activePacks.flatMap { it.interfaces() }
    val pluginTasks = activePacks.flatMap { it.extraTasks() }
    val pluginConfigDirs =
        (
            activePacks.flatMap { it.configDirectories(projectRoot) } +
                disabledPacks.flatMap { it.configDirectoriesWhenDisabled(projectRoot) }
        ).filter { it.isDirectory }
    val pluginSpriteDirs =
        activePacks.flatMap { it.spriteDirectories(projectRoot) }.filter { it.isDirectory }
    val pluginScriptDirs =
        activePacks.flatMap { it.packScriptDirectories(projectRoot) }.filter { it.isDirectory }

    syncPluginCs2(projectRoot, pluginPacks, activePacks)

    val allTables = tablesToPack() + pluginTables

    val tasks = mutableListOf<CacheTask>(
        PackModels(File("../.data/raw-cache/models")),
        PackConfig(File("../.data/raw-cache/server")),
    )
    pluginConfigDirs.forEach { tasks += PackConfig(it) }

    val legacySpritesDir = File("../.data/raw-cache/sprites")
    if (legacySpritesDir.isDirectory) {
        tasks += PackSprites(legacySpritesDir)
    }
    pluginSpriteDirs.forEach { tasks += PackSprites(it) }

    tasks += pluginTasks
    if (pluginInterfaces.isNotEmpty()) {
        tasks += PackIfType(pluginInterfaces)
    }
    tasks += PackCs2(File("../.data/raw-cache/cs2"))
    tasks += PackDBTables(allTables)
    if (pluginScriptDirs.isNotEmpty()) {
        tasks += PackScripts(pluginScriptDirs, PackScriptPhase.AFTER_DB, allTables)
    }

    val builder =
        Builder(
            type = taskType,
            cacheLocation = File(getCacheLocation()),
            serverCacheLocation = File(getServerCacheLocation()),
        )
    builder.revision(revision.first)

    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

    if (taskType == TaskType.BUILD) {
        val serverTasks = tasks.filterNot { it is PackCs2 || it is PackIfType || it is PackScripts }

        builder.type = TaskType.SERVER_CACHE_BUILD

        builder
            .extraTasks(
                PackServerConfig(
                    revision.first,
                    File("../.data/raw-cache/server")
                ),
                MapPackers(),
                *serverTasks.toTypedArray(),
            )
            .build()
            .initialize()
    }

    if (builder.type == TaskType.SERVER_CACHE_BUILD) {
        MinifyServerCache().init(getServerCacheLocation())
        val cache = Cache.load(File(getServerCacheLocation()).toPath())
        GamevalDumper.dumpCols(cache, revision.first)
        GamevalDumper.dumpComponents(cache, revision.first)

        val type = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, revision.first)

        val rows: MutableMap<Int, DBRowType> = mutableMapOf()
        OsrsCacheProvider.DBRowDecoder().load(cache, rows)

        val enums: MutableMap<Int, EnumType> = mutableMapOf()
        OsrsCacheProvider.EnumDecoder().load(cache, enums)

        val dbtables: MutableMap<Int, DBTableType> = mutableMapOf()
        OsrsCacheProvider.DBTableDecoder().load(cache, dbtables)

        startGeneration(type, rows, enums, dbtables)
        startEnumGeneration(enums)
    }
}

fun readRevision(): Triple<Int, Int, String> {
    val file =
        listOf("../game.yml", "../game.example.yml").map(::File).firstOrNull { it.exists() }
            ?: error("No game.yml or game.example.yml found")

    return file.useLines { lines ->
        val revisionLine =
            lines.firstOrNull { it.trimStart().startsWith("revision:") }
                ?: error("No revision line found in ${file.name}")

        val revisionStr = revisionLine.substringAfter("revision:").trim()
        val match =
            Regex("""^(\d+)(?:\.(\d+))?$""").matchEntire(revisionStr)
                ?: error("Invalid revision format: '$revisionStr'")

        val major = match.groupValues[1].toInt()
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: -1

        val envLine = file.readLines().firstOrNull { it.trimStart().startsWith("environment:") }

        val environment =
            envLine?.substringAfter("environment:")?.trim()?.removeSurrounding("\"")?.ifBlank {
                "live"
            } ?: "live"

        Triple(major, minor, environment.uppercase())
    }
}

private fun syncPluginCs2(
    projectRoot: File,
    allPacks: List<PluginPack>,
    activePacks: List<PluginPack>,
) {
    val cs2Root = File(projectRoot, ".data/raw-cache/cs2")
    val customRoot = File(cs2Root, "custom").also { it.mkdirs() }
    val symbolsCustom = File(cs2Root, "symbols_custom").also { it.mkdirs() }

    fun packName(pack: PluginPack): String =
        pack::class.java.name.substringAfterLast('.').removeSuffix("PluginPack").lowercase()

    val activeNames = activePacks.map(::packName).toSet()
    val allNames = allPacks.map(::packName).toSet()

    customRoot.listFiles()?.forEach { child ->
        if (child.isDirectory && child.name.lowercase() in allNames && child.name.lowercase() !in activeNames) {
            child.deleteRecursively()
        }
    }
    File(customRoot, "_plugins").takeIf { it.exists() }?.deleteRecursively()
    File(symbolsCustom, "_plugins").takeIf { it.exists() }?.deleteRecursively()

    for (pack in allPacks) {
        for (dir in pack.cs2Directories(projectRoot).filter { it.isDirectory }) {
            val symbols = File(dir, "symbols")
            if (!symbols.isDirectory) continue
            symbols.listFiles()?.filter { it.isFile && it.extension.equals("sym", true) }?.forEach { sym ->
                stripSymbolLines(File(symbolsCustom, sym.name), readSymbolLines(sym))
            }
        }
    }

    for (pack in activePacks) {
        val name = packName(pack)
        val scriptDest =
            File(customRoot, name).also {
                it.deleteRecursively()
                it.mkdirs()
            }

        for (dir in pack.cs2Directories(projectRoot).filter { it.isDirectory }) {
            val scripts = File(dir, "script")
            if (scripts.isDirectory) {
                scripts.walkTopDown().filter { it.isFile && it.extension.equals("cs2", true) }.forEach { src ->
                    src.copyTo(File(scriptDest, src.name), overwrite = true)
                }
            }
            dir.listFiles()?.filter { it.isFile && it.extension.equals("cs2", true) }?.forEach { src ->
                src.copyTo(File(scriptDest, src.name), overwrite = true)
            }

            val symbols = File(dir, "symbols")
            if (symbols.isDirectory) {
                symbols.listFiles()?.filter { it.isFile && it.extension.equals("sym", true) }?.forEach { sym ->
                    mergeSymbolLines(File(symbolsCustom, sym.name), readSymbolLines(sym))
                }
            }
        }
    }

    SymbolsCustomConflictStrip.strip(cs2Root)
}

private val SYMBOL_LINE = Regex("""^\s*(\S+)\s+(.+?)\s*$""")

/** id → remainder of the line (name, and for dbcolumn also the type). */
private fun readSymbolLines(file: File): List<Pair<String, String>> =
    file.readLines().mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@mapNotNull null
        val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@mapNotNull null
        match.groupValues[1] to match.groupValues[2].trim()
    }

private fun symbolName(rest: String): String = rest.substringBefore('\t').substringBefore(' ').trim()

private fun stripSymbolLines(target: File, owned: List<Pair<String, String>>) {
    if (!target.exists() || owned.isEmpty()) return
    val ownedIds = owned.map { it.first }.toSet()
    val ownedNames = owned.map { symbolName(it.second) }.toSet()
    val kept =
        target.readLines().filter { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@filter false
            val match = SYMBOL_LINE.matchEntire(trimmed) ?: return@filter true
            val id = match.groupValues[1]
            val name = symbolName(match.groupValues[2])
            id !in ownedIds && name !in ownedNames
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
    val body = lines.joinToString("\n") { (id, rest) -> "$id\t$rest" }
    target.parentFile?.mkdirs()
    target.writeText(
        buildString {
            if (existing.isNotBlank()) {
                appendLine(existing)
            }
            appendLine(body)
        },
    )
}
