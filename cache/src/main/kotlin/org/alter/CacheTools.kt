package org.alter

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import dev.openrune.OsrsCacheProvider
import dev.openrune.ServerCacheManager
import dev.openrune.cache.CacheManager
import dev.openrune.cache.gameval.Format
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.dump
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.dbtables.PackDBTables
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.VarType
import dev.openrune.filesystem.Cache
import dev.openrune.tools.PackServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.codegen.TableColumn
import org.alter.codegen.TableDef
import org.alter.codegen.generateTable
import org.alter.game.util.DbException
import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.DbQueryCache
import org.alter.game.util.columnOptional
import org.alter.game.util.vars.IntType
import org.alter.impl.skills.Firemaking
import org.alter.impl.misc.FoodTable
import org.alter.impl.skills.PrayerTable
import org.alter.impl.StatComponents
import org.alter.impl.misc.TeleTabs
import org.alter.impl.skills.Woodcutting
import org.alter.impl.skills.Herblore
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

fun getCacheLocation() = File("../data/", "cache").path
fun getRawCacheLocation(dir: String) = File("../data/", "raw-cache/$dir/")

fun tablesToPack() = listOf(
    PrayerTable.skillTable(),
    TeleTabs.teleTabs(),
    StatComponents.statsComponents(),
    FoodTable.consumableFood(),
    Firemaking.logs(),
    Woodcutting.trees(),
    Woodcutting.axes(),
    Herblore.unfinishedPotions(),
    Herblore.finishedPotions(),
    Herblore.cleaningHerbs(),
    Herblore.barbarianMixes(),
    Herblore.swampTar(),
    Herblore.crushing()
)

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <buildType>")
        exitProcess(1)
    }
    downloadRev(TaskType.valueOf(args.first().uppercase()))
}

fun downloadRev(type: TaskType) {

    val rev = readRevision()

    logger.error { "Using Revision: $rev" }

    when (type) {
        TaskType.FRESH_INSTALL -> {
            val builder = Builder(type = TaskType.FRESH_INSTALL, File(getCacheLocation()))
            builder.registerRSCM(File("../data/cfg/rscm2"))
            builder.revision(rev.first)
            builder.subRevision(rev.second)
            builder.removeXteas(false)
            builder.environment(CacheEnvironment.valueOf(rev.third))

            Files.move(
                File(getCacheLocation(), "xteas.json").toPath(),
                File("../data/", "xteas.json").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            buildCache(rev)

        }

        TaskType.BUILD -> buildCache(rev)
    }
}

data class ColInfo(
    val types: MutableSet<VarType> = mutableSetOf(),
    var optional: Boolean = false
)

fun buildCache(rev: Triple<Int, Int, String>) {

//    val tasks: List<CacheTask> = listOf(
//        PackConfig(File("../data/raw-cache/server")),
//        PackServerConfig(),
//    ).toMutableList()
//
//    val builder = Builder(type = TaskType.BUILD, cacheLocation = File(getCacheLocation()))
//    builder.registerRSCM(File("../data/cfg/rscm2"))
//    builder.revision(rev.first)
//
//    val tasksNew = tasks.toMutableList()
//    tasksNew.add(PackDBTables(tablesToPack()))
//
//    builder.extraTasks(*tasksNew.toTypedArray()).build().initialize()


    val cache = Cache.load(File(getCacheLocation()).toPath(), true)

    GameValGroupTypes.entries.forEach {
        val type = GameValHandler.readGameVal(it, cache = cache, rev.first)
        type.dump(Format.RSCM_V2, File("../data/cfg/rscm2"), it).packed(true).write()
    }

    val type = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, rev.first)

    ServerCacheManager.init(cache)

    type.forEach { t ->
        val element = t.elementAs<Table>() ?: return
        val tableName = element.name

        // Initialize all columns as required (false = not optional)
        val colInfoMap: MutableMap<String, ColInfo> = element.columns
            .associate { col -> col.name to ColInfo() }
            .toMutableMap()

        table(element.id).forEach { row ->
            element.columns.forEach { col ->
                val info = colInfoMap[col.name]!! // ✅ now works
                try {
                    val types = row.getColumn(col.id).types
                    types.forEach {
                        info.types.add(it)
                    }
                } catch (e: DbException.MissingColumn) {
                    info.optional = true
                }
            }
        }

        println("Table: $tableName")
        colInfoMap.forEach { (colName, info) ->
            println("Col=$colName, optional=${info.optional}, types=${info.types}")
        }

        val generatedColumns = element.columns.map { col ->
            val info = colInfoMap[col.name]

            TableColumn(
                name = "columns.${tableName}:${col.name}",
                simpleName = col.name,
                kotlinType = INT, // <-- still placeholder, can map from info.types later
                varType = ClassName("org.alter.game.util.vars", "ObjType"), // placeholder
                optional = info!!.optional
            )
        }


        val tableDef = TableDef(
            tableName = tableName,
            className = formatClassName(tableName),
            columns = generatedColumns
        )

        generateTable(tableDef, File("../content/build/generated/ksp/main/kotlin/"))
    }
}

fun formatClassName(tableName: String): String {
    return tableName
        .split('_', '-', '.', ':')
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        } + "Row"
}


fun readRevision(): Triple<Int, Int, String> {
    val file = listOf("../game.yml", "../game.example.yml")
        .map(::File)
        .firstOrNull { it.exists() }
        ?: error("No game.yml or game.example.yml found")

    return file.useLines { lines ->
        val revisionLine = lines.firstOrNull { it.trimStart().startsWith("revision:") }
            ?: error("No revision line found in ${file.name}")

        val revisionStr = revisionLine.substringAfter("revision:").trim()
        val match = Regex("""^(\d+)(?:\.(\d+))?$""").matchEntire(revisionStr)
            ?: error("Invalid revision format: '$revisionStr'")

        val major = match.groupValues[1].toInt()
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: -1

        val envLine = file.readLines()
            .firstOrNull { it.trimStart().startsWith("environment:") }

        val environment = envLine
            ?.substringAfter("environment:")
            ?.trim()
            ?.removeSurrounding("\"")
            ?.ifBlank { "live" }
            ?: "live"

        Triple(major, minor, environment.uppercase())
    }
}