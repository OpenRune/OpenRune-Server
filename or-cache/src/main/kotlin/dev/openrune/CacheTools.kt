package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.cacheTool
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.cs2.UnpackDefaultCs2
import dev.openrune.cache.tools.iftype.PackIfType
import dev.openrune.cache.tools.incremental.CacheVerification
import dev.openrune.cache.tools.incremental.IncrementalSession
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.PackSprites
import dev.openrune.cache.tools.tasks.impl.PackWorldMap
import dev.openrune.codegen.startEnumGeneration
import dev.openrune.codegen.startGeneration
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.filesystem.Cache
import dev.openrune.gamevals.GameValMaxIdManifest
import dev.openrune.gamevals.GameValProvider
import dev.openrune.gamevals.GamevalDumper
import dev.openrune.gamevals.GeneratedGameVals
import dev.openrune.impl.GameframeTable
import dev.openrune.impl.Music
import dev.openrune.map.packing.MapPackers
import dev.openrune.pack.PluginPacks
import dev.openrune.tables.CollectionLogCategoriesTable
import dev.openrune.tables.DidYouKnow
import dev.openrune.tables.GauntletRoomSlotsTable
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
import dev.openrune.tables.skills.Motherlode
import dev.openrune.tables.skills.Runecrafting
import dev.openrune.tables.skills.Slayer
import dev.openrune.tables.skills.SlayerRequirements
import dev.openrune.tables.skills.Smithing
import dev.openrune.tables.skills.prayer.EctofuntusBonemeal
import dev.openrune.tables.skills.prayer.PrayerBlessedBone
import dev.openrune.tables.skills.prayer.PrayerTable
import dev.openrune.toml.rsconfig.rsconfig
import dev.openrune.toml.tomlMapper
import dev.openrune.tools.GamevalChecker
import dev.openrune.tools.LocSpawnDumper
import dev.openrune.tools.MinifyServerCache
import dev.openrune.tools.PackServerConfig
import dev.openrune.tools.ProjectGamevalNames
import dev.openrune.tools.SeqDumper
import dev.openrune.tools.ShortestPathTransports
import dev.openrune.tools.SlayerBossKeys
import dev.openrune.tools.SlayerTargetMonsterLoader
import dev.openrune.types.SlayerNpcData
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

    if (command == "DUMP_SEQS") {
        GameValProvider.load("../")
        SeqDumper.dump(
            Cache.load(File(getServerCacheLocation()).toPath()),
            args.drop(1).flatMap { it.split(',') }.map(String::trim).filter(String::isNotEmpty),
        )
        return
    }

    if (command == "DUMP_LOCS") {
        GameValProvider.load("../")
        LocSpawnDumper.dump(
            Cache.load(File(getCacheLocation()).toPath()),
            Cache.load(File(getServerCacheLocation()).toPath()),
            revision.first,
            args.drop(1).flatMap { it.split(',') }.map(String::trim).filter(String::isNotEmpty),
        )
        return
    }

    if (command == "EXPORT_BASE_GAMEVALS") {
        exportBaseGamevals()
        return
    }

    if (command == "DOWNLOAD_TRANSPORTS") {
        downloadTransports(args.getOrNull(1)?.let(::File))
        return
    }

    if (command == "CHECK_GAMEVALS") {
        checkGamevals(args)
        return
    }

    registerCacheLiterals()
    downloadRev(TaskType.valueOf(command))
}

private fun checkGamevals(args: Array<String>) {
    val snapshot = args.firstOrNull { it.startsWith("--snapshot-dir=") }
        ?.substringAfter('=')?.let(::File)
    val provider = loadGamevalProvider(snapshot)
    val generated =
        if (snapshot == null) {
            emptySet()
        } else {
            registerCacheLiterals()
            ConstantProvider.load(provider)
            val packs = PluginPacks.discover(projectRoot)
            val tables = tablesToPack(loadSlayerData(), SlayerRequirements::gearSchema)
            ProjectGamevalNames.collect(provider, packs.active, tables)
        }
    val result =
        GamevalChecker.check(
            root = projectRoot.canonicalFile,
            provider = provider,
            reportFile = File("build/reports/gamevals/gameval-report.txt"),
            generated = generated,
        )
    if ("--fail" in args && (result.missing.isNotEmpty() || result.unassigned.isNotEmpty())) {
        exitProcess(1)
    }
}

private fun registerCacheLiterals() {
    CacheVarLiteral.registerExternal(253, '[', name = "PROJANIM")
    CacheVarLiteral.registerExternal(254, ']', name = "VARBIT")
}

private fun revisionLabel(): String =
    if (revision.second < 0) "${revision.first}" else "${revision.first}.${revision.second}"

private fun exportBaseGamevals() {
    val output = File("build/reports/base-gamevals")
    val work = File("build/base-gamevals-cache")
    val live = File(work, "LIVE")
    downloadOsrsCache(live, File(work, "SERVER"))
    GamevalDumper.dumpGamevals(Cache.load(live.toPath()), revision.first, output)
    GameValMaxIdManifest.writeTo(
        File(output, "max-ids.toml"),
        GameValMaxIdManifest.readMaxIdsFromDats(output),
        revision.first,
    )
    File(output, "revision.txt").writeText("${revisionLabel()}\n")
}

fun downloadRev(type: TaskType) {
    logger.info { "Using Revision: $revision" }

    if (type == TaskType.FRESH_INSTALL) {
        freshInstall()
        buildCache(TaskType.BUILD, force = true)
        return
    }

    buildCache(type)
}

@Suppress("DEPRECATION")
private fun downloadOsrsCache(live: File, server: File) {
    val rev = revision
    Builder(
        type = TaskType.FRESH_INSTALL,
        cacheLocation = live,
        serverCacheLocation = server,
    )
        .revision(rev.first)
        .subRevision(rev.second)
        .removeXteas(false)
        .environment(CacheEnvironment.valueOf(rev.third))
        .build()
        .initialize()
}

private fun freshInstall() {
    downloadOsrsCache(File(getCacheLocation()), File(getServerCacheLocation()))
    File(getServerCacheLocation(), "xteas.json").delete()

    IncrementalSession.clearState(File(getCacheLocation()), incrementalStateFile(TaskType.BUILD))
    IncrementalSession.clearState(
        File(getServerCacheLocation()),
        incrementalStateFile(TaskType.SERVER_CACHE_BUILD),
    )

    logger.info { "Dumping gamevals from the fresh cache" }
    GamevalDumper.dumpGamevals(Cache.load(File(getCacheLocation()).toPath()), revision.first)
}

private fun downloadTransports(snapshot: File? = null) {
    val target = projectRoot.resolve("content/travel/map-transports/src/main/resources/transports")
    ShortestPathTransports.download(target, File("build/reports/transports"), loadGamevalProvider(snapshot))
}

private fun loadGamevalProvider(snapshot: File?): GameValProvider {
    if (snapshot == null) return GameValProvider.loadIsolated("../")
    require(File(snapshot, "revision.txt").readText().trim() == revisionLabel()) {
        "Gameval snapshot revision does not match ${revisionLabel()}"
    }
    require(
        File(snapshot, "gamevals.dat").isFile &&
            File(snapshot, GeneratedGameVals.FILE_NAME).isFile,
    ) { "Incomplete gameval snapshot: $snapshot" }
    return GameValProvider.loadIsolated("../", snapshot)
}

fun buildCache(type: TaskType, force: Boolean = false) {
    GameValProvider.load("../")

    val packs = PluginPacks.discover(projectRoot)
    packs.validate()

    val cs2Overrides = packs.cs2Overrides(
        DirectoryConstants.CS2_PATH.toFile(),
        ConstantProvider.getCurrentProviders().filterIsInstance<GameValProvider>().firstOrNull(),
    )

    val packTasks = packs.buildPackTasks(tablesToPack(), cs2Overrides)
    newCacheTool(type, packTasks).initialize()

    if (type == TaskType.BUILD) {
        buildServerCache(packTasks, packs)
    }

    finalizeServerCache(force)
}

private fun buildServerCache(packTasks: List<CacheTask>, packs: PluginPacks) {
    val serverTasks = packTasks.filterNot {
        it is PackCs2 || it is UnpackDefaultCs2 || it is PackIfType ||
            it is PackModels || it is PackSprites || it is PackWorldMap
    }
    val serverOnly = listOf(
        PackServerConfig(
            revision.first,
            File("../.data/raw-cache/server"),
            extraDirectories = packs.configDirectories(),
        ),
        MapPackers(),
    )

    newCacheTool(TaskType.SERVER_CACHE_BUILD, serverOnly + serverTasks).initialize()
}

private fun finalizeServerCache(force: Boolean = false) {
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

    startGeneration(tableTypes, rows, enums, dbTables, force)
    startEnumGeneration(enums, force)
}

fun tablesToPack(): List<DBTable> {
    val slayerData = loadSlayerData()
    val slayerBossKey = loadSlayerBossKeys()
    return tablesToPack(slayerData) { SlayerRequirements.gear(slayerBossKey) }
}

private fun loadSlayerData(): Map<Int, SlayerNpcData> =
    SlayerTargetMonsterLoader.loadNpcSlayerData(
        File("../.data/raw-cache/server/slayer"),
        tomlMapper { rsconfig { enableConstantProvider() } },
    )

private fun loadSlayerBossKeys(): (String) -> Int =
    SlayerBossKeys.load(Cache.load(File(getCacheLocation()).toPath()), revision.first)

private fun tablesToPack(
    slayerData: Map<Int, SlayerNpcData>,
    slayerGear: () -> DBTable,
): List<DBTable> = listOf(
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
    Motherlode.payDirt(),
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
    Slayer.streakBonuses(),
    slayerGear(),
    SlayerRequirements.finishers(slayerData),
    SlayerRequirements.weaponRules(slayerData),
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
    GauntletRoomSlotsTable.table(),
    CollectionLogCategoriesTable.collectionLogCategories(),
    ShopCurrencyTable.shopCurrencies(),
)

private fun newCacheTool(type: TaskType, packTasks: List<CacheTask>): CacheTool {
    val rev = revision.first
    val subRev = revision.second
    return cacheTool {
        taskType = type
        revision(rev)
        subRevision(subRev)
        cache(getCacheLocation())
        serverCache(getServerCacheLocation())
        autoCert = type == TaskType.BUILD

        if (type == TaskType.SERVER_CACHE_BUILD) {
            serverEmptyIndices = MinifyServerCache.STRIPPED_INDICES
        }

        incremental = true
        incrementalDatabase(incrementalStateFile(type).path)

        verification =
            if (type == TaskType.SERVER_CACHE_BUILD) CacheVerification.OUTPUT_CRC
            else CacheVerification.FINGERPRINT

        progress = CombinedProgress()

        tasks { packTasks.forEach { +it } }
    }
}

private fun incrementalStateFile(type: TaskType): File {
    val name = if (type == TaskType.SERVER_CACHE_BUILD) "incremental_server" else "incremental_live"
    return File("../.data/cache", name)
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
