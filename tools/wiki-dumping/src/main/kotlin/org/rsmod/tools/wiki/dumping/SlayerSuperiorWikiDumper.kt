package org.rsmod.tools.wiki.dumping

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlinx.coroutines.delay
import org.rsmod.tools.wiki.dumping.wiki.SuperiorMonsterRow
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiLinks
import org.rsmod.tools.wiki.dumping.wiki.WikiNpcResolver
import org.rsmod.tools.wiki.dumping.wiki.WikiTables

data class SlayerSuperiorEntry(
    val superiorNpc: String,
    val normalNpcs: List<String>,
    val wildernessAvailable: Boolean,
    val rowIndex: Int,
)

data class SlayerSuperiorDumpResult(
    val entries: List<SlayerSuperiorEntry>,
    val unmappedNpcIds: List<Int>,
    val failures: List<SlayerDumpFailure>,
)

class SlayerSuperiorWikiDumper(
    private val wiki: WikiClient,
    private val npcLookup: NpcRscmLookup,
    private val requestDelayMs: Long = 75L,
    private val quiet: Boolean = false,
) {
    suspend fun dump(pageTitle: String = "Superior slayer monster"): SlayerSuperiorDumpResult {
        val startedAt = System.currentTimeMillis()
        log("Fetching $pageTitle (Monsters section) ...")

        val source = wiki.rawPageSource(pageTitle)
        val rows = WikiTables.parseSuperiorMonsterRows(source)
        log("${rows.size} superior monster rows")

        var wikiFetches = 0
        val resolver =
            WikiNpcResolver(
                wiki = wiki,
                requestDelayMs = requestDelayMs,
                onPageFetch = {
                    wikiFetches++
                    if (!quiet) {
                        print("\r[superior-dump] wiki requests: $wikiFetches   ")
                        System.out.flush()
                    }
                },
            )

        val failures = mutableListOf<SlayerDumpFailure>()
        val entries = mutableListOf<SlayerSuperiorEntry>()
        val unmappedNpcIds = mutableListOf<Int>()

        for ((i, row) in rows.withIndex()) {
            if (!quiet) {
                val label = WikiLinks.extractPageTitles(row.superiorCell).firstOrNull() ?: "row ${row.rowIndex}"
                println("\r[superior-dump] [${i + 1}/${rows.size}] $label".padEnd(60))
            }

            val normalTitles = WikiLinks.extractPageTitles(row.normalCell)
            val superiorTitles = WikiLinks.extractPageTitles(row.superiorCell)
            if (superiorTitles.isEmpty()) continue

            val normalIds = mutableListOf<Int>()
            for (title in normalTitles) {
                when (val result = resolver.resolveDirectMonsterTitle(title, failures, row)) {
                    is WikiNpcResolver.DirectResolveResult.Ok -> normalIds += result.npcIds
                    else -> Unit
                }
                delay(requestDelayMs)
            }

            val superiorTitle = superiorTitles.first()
            val superiorIds =
                when (val result = resolver.resolveDirectMonsterTitle(superiorTitle, failures, row)) {
                    is WikiNpcResolver.DirectResolveResult.Ok -> result.npcIds
                    else -> emptyList()
                }
            delay(requestDelayMs)

            val (normalRscm, normalUnmapped) = npcLookup.toRscmList(normalIds.distinct())
            val (superiorRscm, superiorUnmapped) = npcLookup.toRscmList(superiorIds)
            unmappedNpcIds += normalUnmapped + superiorUnmapped

            val superiorNpc = superiorRscm.firstOrNull() ?: continue
            if (normalRscm.isEmpty()) continue

            entries +=
                SlayerSuperiorEntry(
                    superiorNpc = superiorNpc,
                    normalNpcs = normalRscm,
                    wildernessAvailable = row.wildernessAvailable,
                    rowIndex = row.rowIndex,
                )
        }

        if (!quiet) {
            val elapsed = (System.currentTimeMillis() - startedAt) / 1000.0
            println()
            println(
                "[superior-dump] Done in ${"%.1f".format(elapsed)}s — " +
                    "${entries.size} entries, ${wikiFetches} wiki pages",
            )
        }

        return SlayerSuperiorDumpResult(
            entries = entries,
            unmappedNpcIds = unmappedNpcIds.distinct().sorted(),
            failures = failures,
        )
    }

    fun renderToml(result: SlayerSuperiorDumpResult): String =
        buildString {
            appendLine("# https://oldschool.runescape.wiki/w/Superior_slayer_monster")
            appendLine("# Normal NPC death on task can spawn superiorNpc (requires Bigger and Badder unlock).")
            appendLine()
            for (entry in result.entries) {
                appendLine("[[slayer_superior]]")
                appendLine("superiorNpc = \"${entry.superiorNpc}\"")
                appendLine("normalNpcs = [${entry.normalNpcs.joinToString(", ") { "\"$it\"" }}]")
                appendLine("wildernessAvailable = ${entry.wildernessAvailable}")
                appendLine()
            }
        }

    suspend fun dumpToFile(output: Path, pageTitle: String = "Superior slayer monster"): SlayerSuperiorDumpResult {
        val result = dump(pageTitle)
        output.parent?.let { Files.createDirectories(it) }
        if (!quiet) {
            println("[superior-dump] Writing $output")
        }
        output.writeText(renderToml(result))
        return result
    }

    private fun log(message: String) {
        if (!quiet) {
            println("[superior-dump] $message")
        }
    }
}
