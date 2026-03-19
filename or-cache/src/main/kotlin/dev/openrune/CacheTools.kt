package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.tools.Builder
import dev.openrune.cache.tools.CacheEnvironment
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.TaskType
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.filesystem.Cache
import dev.openrune.tools.MinifyServerCache
import dev.openrune.tools.PackServerConfig
import dev.openrune.gamevals.GameValProvider
import dev.openrune.gamevals.GamevalDumper
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

fun getCacheLocation() = File("../data/", "cache/LIVE").path
fun getServerCacheLocation() = File("../data/", "cache/SERVER").path
fun getRawCacheLocation(dir: String) = File("../data/", "raw-cache/$dir/")

private val logger = InlineLogger()

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: <buildType>")
        exitProcess(1)
    }
    downloadRev(TaskType.valueOf(args.first().uppercase()))
}

fun downloadRev(type: TaskType) {

    val rev = readRevision()

    logger.info { "Using Revision: $rev" }

    when (type) {
        TaskType.FRESH_INSTALL -> {

            val builder = Builder(type = TaskType.FRESH_INSTALL,
                cacheLocation = File(getCacheLocation()),
                serverCacheLocation = File(getServerCacheLocation())
            )
            builder.revision(rev.first)
            builder.subRevision(rev.second)
            builder.removeXteas(false)
            builder.environment(CacheEnvironment.valueOf(rev.third))

            builder.build().initialize()

            Files.move(
                File(getCacheLocation(), "xteas.json").toPath(),
                File("../data/cache/", "xteas.json").toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )

            File(getServerCacheLocation(), "xteas.json").delete()

            val cache = Cache.load(File(getCacheLocation()).toPath())

            GamevalDumper.dumpGamevals(cache,rev.first)

            buildCache(TaskType.BUILD,rev)
        }
        TaskType.SERVER_CACHE_BUILD -> buildCache(TaskType.SERVER_CACHE_BUILD,rev)
        TaskType.BUILD -> buildCache(TaskType.BUILD,rev)

    }
}


fun buildCache(taskType: TaskType,rev: Triple<Int, Int, String>) {
    GameValProvider.load(autoAssignIds = true)

    val tasks: List<CacheTask> = listOf(
        PackConfig(File("../data/raw-cache/server"))
    ).toMutableList()

    val builder = Builder(type = taskType,
        cacheLocation = File(getCacheLocation()),
        serverCacheLocation = File(getServerCacheLocation())
    )
    builder.revision(rev.first)

    val tasksNew = tasks.toMutableList()

    builder.extraTasks(*tasksNew.toTypedArray()).build().initialize()
    if (taskType == TaskType.BUILD) {
        builder.type = TaskType.SERVER_CACHE_BUILD
        builder.extraTasks(
            PackServerConfig(File("../data/raw-cache/server")),
            *tasksNew.toTypedArray()
        ).build().initialize()
    }

    if (builder.type == TaskType.SERVER_CACHE_BUILD) {
        MinifyServerCache().init(getServerCacheLocation())
        val cache = Cache.load(File(getServerCacheLocation()).toPath())
        GamevalDumper.dumpCols(cache,rev.first)
    }
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
