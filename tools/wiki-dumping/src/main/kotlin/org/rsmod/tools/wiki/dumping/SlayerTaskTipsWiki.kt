package org.rsmod.tools.wiki.dumping

import org.rsmod.tools.wiki.dumping.wiki.WikiLinks
import org.rsmod.tools.wiki.dumping.wiki.WikiText

/**
 * Parses [Slayer_task_tips](https://oldschool.runescape.wiki/w/Slayer_task_tips) and maps wiki
 * monster names to slayer [targetId] values for [enum.slayer_task_tips] and [slayer_npc_tips.toml].
 */
object SlayerTaskTipsWiki {
    private val rowSplit = Regex("""\n\|-\s*""")
    private val dataCellLine = Regex("""^\|([^!].*)$""", RegexOption.MULTILINE)
    private val headerCellLine = Regex("""^!+([^!].*)$""", RegexOption.MULTILINE)
    private val markdownTipRow =
        Regex("""^\|\s*\[([^\]]+)\]\([^)]*\)\s*\|\s*(.+?)\s*\|\s*$""", RegexOption.MULTILINE)

    data class ResolveResult(
        val tipsByTargetId: Map<Int, String>,
        val unmappedMonsters: List<String>,
    )

    fun resolveTipsByTargetId(
        tipsWikitext: String,
        taskIndex: Map<String, Int>,
        entries: List<SlayerTargetMonsterEntry>,
    ): ResolveResult {
        val assignmentIndex = DbrowSlayerTaskIndex.indexFromEntries(entries)
        val taskTips = linkedMapOf<Int, String>()
        val unmapped = mutableListOf<String>()

        val wikiRows = parseTipsFromWikitext(tipsWikitext)
        if (wikiRows.isEmpty()) {
            System.err.println("[slayer-dump] No tips parsed from wiki wikitext; check Slayer_task_tips page format.")
        }

        for ((monster, tip) in wikiRows) {
            val targetId =
                resolveTargetId(
                    monster = monster,
                    taskIndex = taskIndex,
                    assignmentIndex = assignmentIndex,
                    entries = entries,
                )
                    ?: run {
                        unmapped += monster
                        continue
                    }
            if (targetId in taskTips && taskTips[targetId] != tip) {
                continue
            }
            taskTips[targetId] = tip
        }

        return ResolveResult(tipsByTargetId = taskTips, unmappedMonsters = unmapped.distinct())
    }

    fun renderTaskTipsEnum(tipsByTargetId: Map<Int, String>): String =
        buildString {
            appendLine("# https://oldschool.runescape.wiki/w/Slayer_task_tips")
            appendLine("# Task id -> tip (dialogue). Regenerate: SlayerTaskWikiDumper main.")
            appendLine()
            appendLine("[[enum]]")
            appendLine("isServerOnly = true")
            appendLine("id = \"enum.slayer_task_tips\"")
            appendLine("keyType = \"INT\"")
            appendLine("valueType = \"STRING\"")
            appendLine()
            appendLine("[enum.values]")
            for ((targetId, tip) in tipsByTargetId.toSortedMap()) {
                appendLine("$targetId = \"${escapeTomlString(tip)}\"")
            }
        }

    fun renderNpcTipsToml(
        entries: List<SlayerTargetMonsterEntry>,
        tipsByTargetId: Map<Int, String>,
    ): String =
        buildString {
            appendLine("# https://oldschool.runescape.wiki/w/Slayer_task_tips")
            appendLine("# NPC param.slayer_task_tip. Regenerate: SlayerTaskWikiDumper main.")
            appendLine()

            val tipToTargets = linkedMapOf<String, LinkedHashSet<String>>()
            for (entry in entries) {
                val tip = tipsByTargetId[entry.targetId] ?: continue
                val bucket = tipToTargets.getOrPut(tip) { linkedSetOf() }
                for (npc in entry.targets) {
                    bucket += npc
                }
            }

            for ((tip, targets) in tipToTargets) {
                if (targets.isEmpty()) continue
                appendLine("[[npc_tip]]")
                appendLine("targets = [${targets.sorted().joinToString(", ") { "\"$it\"" }}]")
                appendLine("tip = \"${escapeTomlString(tip)}\"")
                appendLine()
            }
        }

    fun escapeTomlString(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    internal fun parseTipsFromWikitext(source: String): List<Pair<String, String>> {
        val rows = mutableListOf<Pair<String, String>>()
        var searchFrom = 0
        while (true) {
            val start = source.indexOf("{|", searchFrom)
            if (start < 0) break
            val end = source.indexOf("|}", start)
            if (end < 0) break
            val table = source.substring(start, end + 2)
            rows += parseTipsTable(table)
            searchFrom = end + 2
        }
        if (rows.isEmpty()) {
            rows += parseMarkdownTipRows(source)
        }
        return rows
    }

    private fun parseMarkdownTipRows(source: String): List<Pair<String, String>> =
        markdownTipRow
            .findAll(source)
            .mapNotNull { match ->
                val monster = match.groupValues[1].trim()
                val tip = match.groupValues[2].trim()
                if (monster.isBlank() || tip.isBlank() || isHeaderRow(listOf(monster, tip))) {
                    null
                } else {
                    monster to tip
                }
            }.toList()

    private fun parseTipsTable(table: String): List<Pair<String, String>> {
        val rowBlocks = table.split(rowSplit).drop(1)
        val rows = mutableListOf<Pair<String, String>>()

        for (rowBlock in rowBlocks) {
            val cells = cellsFromRow(rowBlock)
            if (cells.size < 2 || isHeaderRow(cells)) continue

            val monsterCell = cells[0]
            val tipCell = WikiText.stripTemplates(cells[1]).trim()
            if (monsterCell.isBlank() || tipCell.isBlank()) continue
            if (tipCell.contains("edit | edit source", ignoreCase = true)) continue

            val monsters = monsterNamesFromCell(monsterCell)
            for (monster in monsters) {
                rows += monster to tipCell
            }
        }
        return rows
    }

    private fun monsterNamesFromCell(cell: String): List<String> {
        val titles = WikiLinks.extractPageTitles(cell)
        if (titles.isNotEmpty()) {
            return titles.map { title ->
                title
                    .removePrefix("Slayer task/")
                    .trim()
            }
        }
        val stripped =
            WikiText
                .stripTemplates(cell)
                .replace(Regex("""\[\[([^\]|#]+)(?:\|[^\]]*)?\]\]"""), "$1")
                .replace(Regex("""\[\[|\]\]"""), "")
                .trim()
        return if (stripped.isNotEmpty()) listOf(stripped) else emptyList()
    }

    private fun isHeaderRow(cells: List<String>): Boolean {
        val joined = cells.joinToString(" ").lowercase()
        return "monster" in joined && "tip" in joined && cells.getOrNull(1)?.length?.let { it < 48 } == true
    }

    private fun cellsFromRow(rowBlock: String): List<String> {
        val raw =
            dataCellLine
                .findAll(rowBlock)
                .map { it.groupValues[1].trim() }
                .filter { it != "}" }
                .toList()
        if (raw.size == 1 && "||" in raw[0]) {
            return raw[0].split("||").map { it.trim() }.filter { it.isNotEmpty() }
        }
        return raw
    }

    private fun resolveTargetId(
        monster: String,
        taskIndex: Map<String, Int>,
        assignmentIndex: Map<String, Int>,
        entries: List<SlayerTargetMonsterEntry>,
    ): Int? {
        for (key in DbrowSlayerTaskIndex.normalizeKeys(monster)) {
            DbrowSlayerTaskIndex.resolve(key, taskIndex)?.takeIf { it > 0 }?.let { return it }
            assignmentIndex[key]?.let { return it }
        }

        val norm = DbrowSlayerTaskIndex.normalize(monster)
        val tokens = norm.split(' ').filter { it.length > 2 }
        var bestId: Int? = null
        var bestScore = 0
        for (entry in entries) {
            if (entry.targetId <= 0) continue
            var score = 0
            val taskNorm = DbrowSlayerTaskIndex.normalize(entry.taskName)
            if (norm == taskNorm || norm in taskNorm || taskNorm in norm) {
                score = maxOf(score, 12)
            }
            for (npc in entry.targets) {
                val npcNorm = DbrowSlayerTaskIndex.normalize(npc.removePrefix("npc."))
                if (norm in npcNorm || npcNorm in norm) {
                    score = maxOf(score, 10)
                }
                for (token in tokens) {
                    if (token in npcNorm) {
                        score += 2
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score
                bestId = entry.targetId
            }
        }
        return bestId?.takeIf { bestScore >= 6 }
    }
}
