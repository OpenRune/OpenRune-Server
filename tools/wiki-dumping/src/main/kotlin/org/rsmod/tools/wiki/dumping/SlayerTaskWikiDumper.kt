package org.rsmod.tools.wiki.dumping

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.SlayerAssignmentRow
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiNpcResolver
import org.rsmod.tools.wiki.dumping.wiki.WikiTables

private const val DEFAULT_DBROW_RELATIVE = ".data/osrs-dumps/dump.dbrow"

const val SLAYER_RAW_CACHE_DIR = ".data/raw-cache/server/slayer"

private fun findRepoRoot(): Path? = GameValLoader.resolveRootOrNull()

private fun defaultSlayerOutputDir(): Path {
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve(SLAYER_RAW_CACHE_DIR)
    } else {
        Path(SLAYER_RAW_CACHE_DIR)
    }
}

data class SlayerTargetMonsterEntry(
    val targetId: Int,
    val targets: List<String>,
    val unmappedNpcIds: List<Int>,
    val taskName: String,
)

data class SlayerDumpResult(
    val entries: List<SlayerTargetMonsterEntry>,
    val unmatchedTasks: List<String>,
    val unmappedNpcIds: List<Int>,
    val failures: List<SlayerDumpFailure>,
) {
    val missing: List<SlayerTargetMonsterEntry> = entries.filter { it.targets.isEmpty() }
}

data class SlayerDumpFailure(
    val wikiRow: Int,
    val taskName: String,
    val pageTitle: String,
    val reason: String,
)

object DbrowSlayerTaskIndex {
    fun fromSource(dbrowSource: String): Map<String, Int> {
        val byName = mutableMapOf<String, Int>()
        for (task in parseTasks(dbrowSource)) {
            byName[normalize(task.nameLower)] = task.id
            task.nameUpper?.let { byName[normalize(it)] = task.id }
        }
        return byName
    }

    private val manualTargetIds =
        mapOf(
            "flesh crawler" to 77,
            "werewolf" to 33,
            "zygomites" to 74,
        )

    /** Wiki [Slayer_task] assignment rows and [Slayer_task_tips] monster names → [targetId]. */
    fun indexFromEntries(entries: List<SlayerTargetMonsterEntry>): Map<String, Int> {
        val byName = mutableMapOf<String, Int>()
        for (entry in entries) {
            if (entry.targetId <= 0) continue
            for (key in normalizeKeys(entry.taskName)) {
                byName.putIfAbsent(key, entry.targetId)
            }
        }
        return byName
    }

    fun resolve(wikiTaskName: String, index: Map<String, Int>): Int? {
        val normalized = normalize(wikiTaskName)
        manualTargetIds[normalized]?.let { return it }
        return index[normalized]
            ?: index[normalized.removeSuffix("s")]
            ?: index["${normalized}s"]
    }

    fun normalizeKeys(name: String): List<String> {
        val base = normalize(name)
        val keys = linkedSetOf(base)
        if (base.endsWith("s")) {
            keys += base.dropLast(1)
        } else {
            keys += "${base}s"
        }
        return keys.toList()
    }

    fun normalize(name: String): String =
        name
            .lowercase()
            .replace('_', ' ')
            .replace(Regex("""[^a-z0-9']+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private data class TaskDef(val id: Int, val nameLower: String, val nameUpper: String?)

    private fun parseTasks(source: String): List<TaskDef> {
        val tasks = mutableListOf<TaskDef>()
        var table: String? = null
        var id: Int? = null
        var nameLower: String? = null
        var nameUpper: String? = null

        fun flush() {
            val taskId = id
            val lower = nameLower
            if (table == "slayer_task" && taskId != null && taskId >= 0 && lower != null) {
                tasks += TaskDef(taskId, lower, nameUpper)
            }
            table = null
            id = null
            nameLower = null
            nameUpper = null
        }

        for (line in source.lineSequence()) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.startsWith("//") -> flush()
                trimmed.startsWith("table=") -> table = trimmed.substringAfter("table=").trim()
                trimmed.startsWith("data=id,") -> id = trimmed.substringAfter("data=id,").trim().toIntOrNull()
                trimmed.startsWith("data=name_lowercase,") ->
                    nameLower = trimmed.substringAfter("data=name_lowercase,").trim()
                trimmed.startsWith("data=name_uppercase,") ->
                    nameUpper = trimmed.substringAfter("data=name_uppercase,").trim()
            }
        }
        flush()
        return tasks
    }

}

class SlayerTaskWikiDumper(
    private val wiki: WikiClient,
    private val npcLookup: NpcRscmLookup,
    private val taskIndex: Map<String, Int> = emptyMap(),
    private val quiet: Boolean = false,
) {
    suspend fun dump(pageTitle: String = "Slayer_task"): SlayerDumpResult {
        val startedAt = System.currentTimeMillis()
        log("Fetching $pageTitle ...")

        val source = wiki.rawPageSource(pageTitle)
        val rows = WikiTables.parseAssignmentRows(source)
        log("${rows.size} assignment rows")

        var wikiFetches = 0
        val resolver =
            WikiNpcResolver(
                wiki = wiki,
                onPageFetch = {
                    wikiFetches++
                    if (!quiet) {
                        print("\r[slayer-dump] wiki requests: $wikiFetches   ")
                        System.out.flush()
                    }
                },
            )

        val failures = mutableListOf<SlayerDumpFailure>()
        val entries = mutableListOf<SlayerTargetMonsterEntry>()
        val unmatched = mutableListOf<String>()
        val unmappedNpcIds = mutableListOf<Int>()

        for ((i, row) in rows.withIndex()) {
            val taskName = row.wikiTaskName()
            if (!quiet) {
                println("\r[slayer-dump] [${i + 1}/${rows.size}] $taskName".padEnd(60))
            }

            val npcIds = resolver.resolveRow(row, failures)
            val (targets, rowUnmapped) = npcLookup.toRscmList(npcIds)
            val filteredTargets = targets.filterNot { isSuperiorRscmKey(it) }
            unmappedNpcIds += rowUnmapped

            val targetId = DbrowSlayerTaskIndex.resolve(taskName, taskIndex) ?: 0
            if (targetId == 0 && taskIndex.isNotEmpty()) {
                unmatched += taskName
            }

            entries +=
                SlayerTargetMonsterEntry(
                    targetId = targetId,
                    targets = filteredTargets,
                    unmappedNpcIds = rowUnmapped,
                    taskName = taskName,
                )
        }

        if (!quiet) {
            val elapsed = (System.currentTimeMillis() - startedAt) / 1000.0
            println()
            println(
                "[slayer-dump] Done in ${"%.1f".format(elapsed)}s — " +
                    "${entries.count { it.targets.isNotEmpty() }}/${entries.size} rows with npc targets, " +
                    "${resolver.pagesFetched} wiki pages",
            )
            if (unmatched.isNotEmpty()) {
                println("[slayer-dump] ${unmatched.size} task(s) without dbrow target_id match")
            }
        }

        return SlayerDumpResult(
            entries = entries,
            unmatchedTasks = unmatched.distinct(),
            unmappedNpcIds = unmappedNpcIds.distinct().sorted(),
            failures = failures,
        )
    }

    fun renderToml(result: SlayerDumpResult): String =
        buildString {
            appendLine("# https://oldschool.runescape.wiki/w/Slayer_task")
            appendLine()
            for (entry in result.entries) {
                appendLine("[[slayer_target_monster]]")
                appendLine("targetId = ${entry.targetId}")
                if (entry.targets.isEmpty()) {
                    appendLine("targets = []")
                } else {
                    appendLine("targets = [${entry.targets.joinToString(", ") { "\"$it\"" }}]")
                }
                appendLine()
            }
        }

    suspend fun dumpToFile(
        output: Path,
        pageTitle: String = "Slayer_task",
        tipsPageTitle: String = "Slayer_task_tips",
    ): SlayerDumpResult {
        val result = dump(pageTitle)
        val tipsSource = wiki.rawPageSource(tipsPageTitle)
        val tipsResult = SlayerTaskTipsWiki.resolveTipsByTargetId(tipsSource, taskIndex, result.entries)
        val tipsByTargetId = tipsResult.tipsByTargetId
        val tipsEnumOutput = output.parent?.resolve("slayer_task_tips.toml") ?: Path("slayer_task_tips.toml")
        val npcTipsOutput = output.parent?.resolve("slayer_npc_tips.toml") ?: Path("slayer_npc_tips.toml")

        output.parent?.let { Files.createDirectories(it) }

        if (!quiet) {
            println("[slayer-dump] Writing $output")
            println("[slayer-dump] Writing $tipsEnumOutput (${tipsByTargetId.size} task tips)")
            println("[slayer-dump] Writing $npcTipsOutput")
            if (tipsResult.unmappedMonsters.isNotEmpty()) {
                println(
                    "[slayer-dump] ${tipsResult.unmappedMonsters.size} tip monster name(s) " +
                        "had no dbrow/assignment/npc match",
                )
            }
        }
        output.writeText(renderToml(result))
        tipsEnumOutput.writeText(SlayerTaskTipsWiki.renderTaskTipsEnum(tipsByTargetId))
        npcTipsOutput.writeText(SlayerTaskTipsWiki.renderNpcTipsToml(result.entries, tipsByTargetId))
        if (tipsResult.unmappedMonsters.isNotEmpty()) {
            System.err.println()
            System.err.println("=== slayer-dump UNMAPPED tip monsters (${tipsResult.unmappedMonsters.size}) ===")
            for (name in tipsResult.unmappedMonsters.sorted()) {
                System.err.println("  $name")
            }
        }
        return result
    }

    private fun log(message: String) {
        if (!quiet) {
            println("[slayer-dump] $message")
        }
    }

    companion object {
        /** Superior NPCs are packed via [slayer_superior.toml], not task target lists. */
        fun isSuperiorRscmKey(key: String): Boolean = key.contains("superior_", ignoreCase = true)

        fun loadEntriesFromTargetMonstersToml(path: Path): List<SlayerTargetMonsterEntry> {
            if (!path.exists()) return emptyList()
            val blocks = path.readText().split("[[slayer_target_monster]]").drop(1)
            return blocks.mapNotNull { block ->
                val targetId =
                    Regex("""targetId\s*=\s*(\d+)""").find(block)?.groupValues?.get(1)?.toIntOrNull()
                        ?: return@mapNotNull null
                val targetsLine = block.lineSequence().firstOrNull { it.trimStart().startsWith("targets =") } ?: ""
                val targets =
                    Regex(""""([^"]+)"""")
                        .findAll(targetsLine)
                        .map { it.groupValues[1] }
                        .filterNot { SlayerTaskWikiDumper.isSuperiorRscmKey(it) }
                        .toList()
                SlayerTargetMonsterEntry(
                    targetId = targetId,
                    targets = targets,
                    unmappedNpcIds = emptyList(),
                    taskName = "",
                )
            }
        }
    }

    suspend fun dumpTipsOnly(
        targetMonstersFile: Path,
        tipsPageTitle: String = "Slayer_task_tips",
    ) {
        val entries = loadEntriesFromTargetMonstersToml(targetMonstersFile)
        if (entries.isEmpty()) {
            System.err.println("[slayer-dump] No entries in $targetMonstersFile — run full dump first.")
            exitProcess(1)
        }
        val tipsSource = wiki.rawPageSource(tipsPageTitle)
        val rowCount = SlayerTaskTipsWiki.parseTipsFromWikitext(tipsSource).size
        if (!quiet) {
            println("[slayer-dump] Parsed $rowCount tip rows from wiki")
        }
        val tipsResult = SlayerTaskTipsWiki.resolveTipsByTargetId(tipsSource, taskIndex, entries)
        val tipsByTargetId = tipsResult.tipsByTargetId
        val tipsEnumOutput = targetMonstersFile.parent.resolve("slayer_task_tips.toml")
        val npcTipsOutput = targetMonstersFile.parent.resolve("slayer_npc_tips.toml")
        targetMonstersFile.parent.let { Files.createDirectories(it) }
        if (!quiet) {
            println("[slayer-dump] Writing $tipsEnumOutput (${tipsByTargetId.size} mapped task tips)")
            println("[slayer-dump] Writing $npcTipsOutput")
        }
        tipsEnumOutput.writeText(SlayerTaskTipsWiki.renderTaskTipsEnum(tipsByTargetId))
        npcTipsOutput.writeText(SlayerTaskTipsWiki.renderNpcTipsToml(entries, tipsByTargetId))
        if (tipsResult.unmappedMonsters.isNotEmpty()) {
            System.err.println()
            System.err.println("=== slayer-dump UNMAPPED tip monsters (${tipsResult.unmappedMonsters.size}) ===")
            for (name in tipsResult.unmappedMonsters.sorted()) {
                System.err.println("  $name")
            }
        }
        if (tipsByTargetId.isEmpty()) {
            exitProcess(1)
        }
    }
}

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val positional = args.filterNot { it.startsWith("-") }

    val slayerDir = defaultSlayerOutputDir()
    val output =
        positional.getOrNull(0)?.let { Path(it) }
            ?: slayerDir.resolve("slayer_target_monsters.toml")
    val pageTitle = positional.getOrNull(1) ?: "Slayer_task"
    val superiorOutput =
        positional.getOrNull(2)?.let { Path(it) }
            ?: output.parent?.resolve("slayer_superior.toml")
            ?: slayerDir.resolve("slayer_superior.toml")
    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val dbrowPath =
        flags.firstOrNull { it.startsWith("--dbrow=") }?.substringAfter("--dbrow=")
            ?: System.getProperty("dbrow")
    val wikiDumpDir = flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter("--root=")
            ?: System.getProperty("RSPS_ROOT")
            ?: findRepoRoot()?.toString()

    val npcLookup = NpcRscmLookup.load(rootDir)
    if (!quiet) {
        println("[slayer-dump] Loaded gamevals for npc RSCM lookup")
        println("[slayer-dump] Targets: $output")
        println("[slayer-dump] Tips enum: ${output.parent.resolve("slayer_task_tips.toml")}")
        println("[slayer-dump] NPC tips:  ${output.parent.resolve("slayer_npc_tips.toml")}")
        println("[slayer-dump] Superior: $superiorOutput")
    }

    runBlocking {
        WikiClient.open(wikiDumpDir = wikiDumpDir).use { wiki ->
            val dbrow =
                DbrowDumpFiles.requireLocal(rootDir, dbrowPath)
            if (!quiet) {
                println("[slayer-dump] dbrow: ${dbrow.source.toAbsolutePath()}")
                println("[slayer-dump] wiki dump: ${WikiClient.resolveDumpDirectory(wikiDumpDir).absolutePath}")
            }

            val taskIndex = DbrowSlayerTaskIndex.fromSource(dbrow.text)
            if (!quiet) {
                println("[slayer-dump] ${taskIndex.size} slayer_task name keys from dbrow")
            }

            var exitCode = 0

            val dumper = SlayerTaskWikiDumper(wiki, npcLookup, taskIndex = taskIndex, quiet = quiet)

            if (flags.contains("--tips-only")) {
                dumper.dumpTipsOnly(output)
                return@runBlocking
            }

            val result = dumper.dumpToFile(output, pageTitle)
            println("Wrote ${result.entries.size} slayer task entries to $output")
            reportDumpIssues("slayer-dump", result.unmappedNpcIds, result.unmatchedTasks, result.failures)
            if (result.missing.isNotEmpty()) {
                exitCode = 1
            }

            val superiorDumper = SlayerSuperiorWikiDumper(wiki, npcLookup, quiet = quiet)
            val superiorResult = superiorDumper.dumpToFile(superiorOutput)
            println("Wrote ${superiorResult.entries.size} superior entries to $superiorOutput")
            reportSuperiorIssues(superiorResult)
            if (superiorResult.entries.isEmpty()) {
                exitCode = 1
            }

            if (exitCode != 0) {
                exitProcess(exitCode)
            }
        }
    }
}

private fun reportDumpIssues(
    prefix: String,
    unmappedNpcIds: List<Int>,
    unmatchedTasks: List<String>,
    failures: List<SlayerDumpFailure>,
) {
    if (unmappedNpcIds.isNotEmpty()) {
        System.err.println()
        System.err.println("=== $prefix UNMAPPED npc ids (${unmappedNpcIds.size}) ===")
        for (id in unmappedNpcIds) {
            System.err.println("  $id")
        }
    }
    if (unmatchedTasks.isNotEmpty()) {
        System.err.println()
        System.err.println("=== $prefix UNMATCHED target_id (${unmatchedTasks.size}) ===")
        for (name in unmatchedTasks.sorted()) {
            System.err.println("  $name")
        }
    }
    val distinctFailures =
        failures.distinctBy { "${it.wikiRow}:${it.pageTitle}:${it.reason}" }.sortedBy { it.wikiRow }
    if (distinctFailures.isNotEmpty()) {
        System.err.println()
        System.err.println("=== $prefix FAILURES (${distinctFailures.size}) ===")
        for (f in distinctFailures) {
            System.err.println("  row ${f.wikiRow} (${f.taskName}) — ${f.pageTitle}: ${f.reason}")
        }
    }
}

private fun reportSuperiorIssues(result: SlayerSuperiorDumpResult) {
    if (result.unmappedNpcIds.isNotEmpty()) {
        System.err.println()
        System.err.println("=== superior-dump UNMAPPED npc ids (${result.unmappedNpcIds.size}) ===")
        for (id in result.unmappedNpcIds) {
            System.err.println("  $id")
        }
    }
    val failures =
        result.failures.distinctBy { "${it.wikiRow}:${it.pageTitle}:${it.reason}" }.sortedBy { it.wikiRow }
    if (failures.isNotEmpty()) {
        System.err.println()
        System.err.println("=== superior-dump FAILURES (${failures.size}) ===")
        for (f in failures) {
            System.err.println("  row ${f.wikiRow} — ${f.pageTitle}: ${f.reason}")
        }
    }
}
