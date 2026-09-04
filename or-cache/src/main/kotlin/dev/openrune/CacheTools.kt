package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.cacheTool
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
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
import dev.openrune.pack.PluginPacks
import dev.openrune.tables.CollectionLogCategoriesTable
import dev.openrune.tables.DidYouKnow
import dev.openrune.tables.InstanceSettingsTable
import dev.openrune.tables.PickableObjects
import dev.openrune.tables.SettingConfigs
import dev.openrune.tables.ShopCurrencyTable
import dev.openrune.tables.StatComponents
import dev.openrune.tables.consumables.food.FoodTable
import dev.openrune.tables.consumables.potion.PotionEffectTable
import dev.openrune.tables.consumables.potion.PotionTable
import dev.openrune.tables.skills.Cooking
import dev.openrune.tables.skills.Firemaking
import dev.openrune.tables.skills.Herblore
import dev.openrune.tables.skills.Mining
import dev.openrune.tables.skills.Runecrafting
import dev.openrune.tables.skills.Slayer
import dev.openrune.tables.skills.Smithing
import dev.openrune.tables.skills.prayer.EctofuntusBonemeal
import dev.openrune.tables.skills.prayer.PrayerBlessedBone
import dev.openrune.tables.skills.prayer.PrayerTable
import dev.openrune.tools.MinifyServerCache
import dev.openrune.tools.PackServerConfig
import java.io.File
import kotlin.system.exitProcess

private val logger = InlineLogger()

private val projectRoot = File("..")

fun getCacheLocation(): String = File("../.data/", "cache/LIVE").path

fun getServerCacheLocation(): String = File("../.data/", "cache/SERVER").path

fun getCs2Location(): File = DirectoryConstants.CS2_PATH.toFile()

val revision: Triple<Int, Int, String> = readRevision()

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <buildType>")
        exitProcess(1)
    }

    val command = args.first().uppercase()

    if (command == "CLEAN_CS2") {
        DirectoryConstants.cleanCs2()
        return
    }

    CacheVarLiteral.registerExternal(253, '[', name = "PROJANIM")
    CacheVarLiteral.registerExternal(254, ']', name = "VARBIT")

    downloadRev(TaskType.valueOf(args.first().uppercase()))
}

fun downloadRev(type: TaskType) {
    logger.info { "Using Revision: $revision" }

    if (type == TaskType.FRESH_INSTALL) {
        freshInstall()
        buildCache(TaskType.BUILD)
        return
    }

    buildCache(type)
}

@Suppress("DEPRECATION")
private fun freshInstall() {
    val rev = revision
    Builder(
        type = TaskType.FRESH_INSTALL,
        cacheLocation = File(getCacheLocation()),
        serverCacheLocation = File(getServerCacheLocation())
    )
        .revision(rev.first)
        .subRevision(rev.second)
        .removeXteas(false)
        .environment(CacheEnvironment.valueOf(rev.third))
        .build()
        .initialize()

    File(getServerCacheLocation(), "xteas.json").delete()
    GamevalDumper.dumpGamevals(Cache.load(File(getCacheLocation()).toPath()), rev.first)
}

fun buildCache(type: TaskType) {
    GameValProvider.load("../")

    val packs = PluginPacks.discover(projectRoot)
    packs.validate()
    packs.syncCs2(DirectoryConstants.CS2_PATH.toFile())
    GameValProvider.load("../")

    val packTasks = packs.buildPackTasks(tablesToPack())
    newCacheTool(type, packTasks).initialize()

    if (type == TaskType.BUILD) {
        buildServerCache(packTasks, packs)
    }

    finalizeServerCache()
}

private fun buildServerCache(packTasks: List<CacheTask>, packs: PluginPacks) {
    val serverTasks = packTasks.filterNot { it is PackCs2 || it is PackIfType }
    val serverOnly = listOf(
        PackServerConfig(
            revision.first,
            File("../.data/raw-cache/server"),
            extraDirectories = packs.configDirectories(),
        ),
        MapPackers(),
    )

    // The previous buildCache() pass just wrote a large batch of fresh binary cache files, and
    // Windows Defender/Search Indexer (both confirmed running) hold a brief scan/index lock on a
    // freshly-written file before releasing it. CacheTool.initialize() below (external
    // dev.or2:tools dependency, can't patch directly) then fails to overwrite `SERVER/
    // main_file_cache.idx255` mid-copy from LIVE - FileAlreadyExistsException: "Tried to overwrite
    // the destination, but failed to delete it." A single System.gc() didn't help (ruled out - this
    // isn't a JVM-held mmap like the MinifyServerCache.kt case), and the lock was confirmed to
    // persist with zero JVM processes running at all - it's a genuine external OS-level lock, not
    // our own state. Retrying with backoff is the correct mitigation for that, not GC.
    retryOnFileLock { newCacheTool(TaskType.SERVER_CACHE_BUILD, serverOnly + serverTasks).initialize() }
}

private fun retryOnFileLock(attempts: Int = 6, block: () -> Unit) {
    repeat(attempts) { attempt ->
        try {
            block()
            return
        } catch (e: java.io.IOException) {
            // kotlin.io.FileAlreadyExistsException (thrown by the external copyTo call this wraps)
            // is a java.io.IOException, not a java.nio.file.FileSystemException - catch the broader
            // type deliberately.
            if (attempt == attempts - 1) throw e
            val delayMs = 500L * (attempt + 1)
            logger.warn { "Cache file locked (likely AV/indexer scan) - retrying in ${delayMs}ms: ${e.message}" }
            Thread.sleep(delayMs)
        }
    }
}

private fun finalizeServerCache() {
    MinifyServerCache().init(getServerCacheLocation())

    val cache = Cache.load(File(getServerCacheLocation()).toPath())
    GamevalDumper.dumpCols(cache, revision.first)
    GamevalDumper.dumpComponents(cache, revision.first)

    val tableTypes =
        GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, revision.first)

    val rows = mutableMapOf<Int, DBRowType>()
    OsrsCacheProvider.DBRowDecoder().load(cache, rows)

    val enums = mutableMapOf<Int, EnumType>()
    OsrsCacheProvider.EnumDecoder().load(cache, enums)

    val dbTables = mutableMapOf<Int, DBTableType>()
    OsrsCacheProvider.DBTableDecoder().load(cache, dbTables)

    startGeneration(tableTypes, rows, enums, dbTables)
    startEnumGeneration(enums)
}

fun tablesToPack(): List<DBTable> = listOf(
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
    CollectionLogCategoriesTable.collectionLogCategories(),
    ShopCurrencyTable.shopCurrencies(),
)

private fun newCacheTool(type: TaskType, packTasks: List<CacheTask>): CacheTool {
    val rev = revision.first
    return cacheTool {
        taskType = type
        revision(rev)
        cache(getCacheLocation())
        serverCache(getServerCacheLocation())
        autoCert = true
        tasks { packTasks.forEach { +it } }
    }
}

fun readRevision(): Triple<Int, Int, String> {
    val file =
        listOf("../game.yml", "../game.example.yml").map(::File).firstOrNull { it.exists() }
            ?: error("No game.yml or game.example.yml found")

    val lines = file.readLines()
    val revisionLine =
        lines.firstOrNull { it.trimStart().startsWith("revision:") }
            ?: error("No revision line found in ${file.name}")

    val revisionStr = revisionLine.substringAfter("revision:").trim()
    val match =
        Regex("""^(\d+)(?:\.(\d+))?$""").matchEntire(revisionStr)
            ?: error("Invalid revision format: '$revisionStr'")

    val major = match.groupValues[1].toInt()
    val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: -1

    val envLine = lines.firstOrNull { it.trimStart().startsWith("environment:") }
    val environment =
        envLine?.substringAfter("environment:")?.trim()?.removeSurrounding("\"")?.ifBlank { "live" }
            ?: "live"

    return Triple(major, minor, environment.uppercase())
}
