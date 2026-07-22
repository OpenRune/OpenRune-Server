package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.cs2.PackCs2
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.PackModels
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.codegen.startEnumGeneration
import dev.openrune.codegen.startGeneration
import dev.openrune.definition.GameValGroupTypes
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
import dev.openrune.tables.StatComponents
import dev.openrune.tables.skills.Cooking
import dev.openrune.tables.skills.Firemaking
import dev.openrune.tables.skills.Herblore
import dev.openrune.tables.skills.Mining
import dev.openrune.tables.skills.Slayer
import dev.openrune.tables.skills.Runecrafting
import dev.openrune.tables.skills.ShootingStars
import dev.openrune.tables.skills.Smithing
import dev.openrune.tables.skills.prayer.EctofuntusBonemeal
import dev.openrune.tables.skills.prayer.PrayerBlessedBone
import dev.openrune.tables.skills.prayer.PrayerTable
import dev.openrune.tools.MinifyServerCache
import dev.openrune.tools.PackServerConfig
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

    val tasks: List<CacheTask> =
        listOf(
            PackModels(File("../.data/raw-cache/models")),
            PackConfig(File("../.data/raw-cache/server")),
            PackDBTables(tablesToPack())
        ).toMutableList()

    val builder =
        Builder(
            type = taskType,
            cacheLocation = File(getCacheLocation()),
            serverCacheLocation = File(getServerCacheLocation()),
        )
    builder.revision(revision.first)

    builder.extraTasks(*tasks.toTypedArray()).build().initialize()

    if (taskType == TaskType.BUILD) {
        val serverTasks = tasks.filterNot { it is PackCs2 }

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
