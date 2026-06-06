package org.rsmod.tools.wiki.dumping.wiki

import org.rsmod.tools.wiki.dumping.SlayerDumpFailure

// ---------------------------------------------------------------------------
// Assignment table
// ---------------------------------------------------------------------------

data class SuperiorMonsterRow(
    val rowIndex: Int,
    val normalCell: String,
    val superiorCell: String,
    val uniqueMechanics: Boolean,
    val wildernessAvailable: Boolean,
)

data class SlayerAssignmentRow(
    val rowIndex: Int,
    val slayerLevel: String,
    val monsterCell: String,
    val superiorCell: String,
    val alternativesCell: String,
    val label: String,
) {
    /** Name used to match dump.dbrow slayer_task (e.g. "Monkeys" from Slayer task/Monkeys). */
    fun wikiTaskName(): String {
        WikiLinks
            .extractPageTitles(monsterCell)
            .firstOrNull { it.startsWith("Slayer task/", ignoreCase = true) }
            ?.substringAfter('/')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        val stripped =
            WikiText
                .stripTemplates(monsterCell)
                .replace(Regex("""\[\[([^\]|#]+)(?:\|[^\]]*)?\]\]"""), "$1")
                .trim()
        if (stripped.isNotEmpty()) {
            return stripped.trimEnd('s', 'S').ifEmpty { stripped }
        }
        return label.substringAfter("Slayer task/", label).trim()
    }
}

object WikiTables {
    private const val ASSIGNMENTS_SECTION = "==List of assignments=="
    private val monsterVariantsHeading = Regex("""(?im)^={2,3}\s*Monster\s+[Vv]ariants?\s*={2,3}""")
    private val listOfRevenantsHeading = Regex("""(?im)^={2,3}\s*List of revenants\s*={2,3}""")
    private val metalDragonsHeading = Regex("""(?im)^===\s*Metal dragons\s*===""")

    private val proseTaskMonster =
        Regex(
            """\[\[((?:[^\]|#]|\([^)]*\))+)(?:\|[^\]]*)?\]\]s can be assigned as a \[\[slayer task\]\]""",
            RegexOption.IGNORE_CASE,
        )

    private val rowSplit = Regex("""\n\|-\s*""")
    private val dataCellLine = Regex("""^\|([^!].*)$""", RegexOption.MULTILINE)

    private const val SUPERIOR_MONSTERS_SECTION = "==Monsters=="

    fun parseSuperiorMonsterRows(wikitext: String): List<SuperiorMonsterRow> {
        val section = extractSection(wikitext, SUPERIOR_MONSTERS_SECTION) ?: return emptyList()
        val table = extractFirstWikitable(section) ?: return emptyList()
        val rowBlocks = table.split(rowSplit).drop(1)

        var carriedSuperior = ""
        var carriedWilderness = false
        var carriedUniqueMechanics = false

        val parsed = mutableListOf<SuperiorMonsterRow>()

        for ((index, rowBlock) in rowBlocks.withIndex()) {
            val cells = cellsFromRow(rowBlock).map(::normalizeTableCell)
            if (cells.isEmpty()) continue

            val normalCell =
                when {
                    cells.size >= 8 -> cells[1]
                    else ->
                        cells.firstOrNull { cell ->
                            WikiLinks.extractPageTitles(cell).isNotEmpty() && !WikiLinks.isNaCell(cell)
                        }
                } ?: continue

            val superiorInRow = if (cells.size >= 8) cells[2] else ""
            val superiorCell = superiorInRow.ifBlank { carriedSuperior }
            if (superiorInRow.isNotBlank()) {
                carriedSuperior = superiorInRow
                carriedWilderness = WikiTemplates.parseYesNo(cells[7]) ?: carriedWilderness
                carriedUniqueMechanics = WikiTemplates.parseYesNo(cells[6]) ?: carriedUniqueMechanics
            }

            if (normalCell.isBlank() || WikiLinks.isNaCell(superiorCell)) continue
            if (WikiLinks.extractPageTitles(superiorCell).isEmpty()) continue
            if (WikiLinks.extractPageTitles(normalCell).isEmpty()) continue

            parsed +=
                SuperiorMonsterRow(
                    rowIndex = index + 1,
                    normalCell = normalCell,
                    superiorCell = superiorCell,
                    uniqueMechanics = carriedUniqueMechanics,
                    wildernessAvailable = carriedWilderness,
                )
        }
        return parsed
    }

    private fun normalizeTableCell(cell: String): String =
        cell
            .trim()
            .replace(Regex("""^rowspan\s*=\s*"?[\d]+"?\s*\|""", RegexOption.IGNORE_CASE), "")
            .trim()

    fun parseAssignmentRows(wikitext: String): List<SlayerAssignmentRow> {
        val section = extractSection(wikitext, ASSIGNMENTS_SECTION) ?: return emptyList()
        val table = extractFirstWikitable(section) ?: return emptyList()
        val rows = table.split(rowSplit).drop(1)

        return rows
            .mapNotNull { rowBlock ->
                val cells = cellsFromRow(rowBlock)
                if (cells.size < 8) {
                    return@mapNotNull null
                }
                val monsterCell = cells[1]
                val label =
                    WikiLinks.extractPageTitles(monsterCell).firstOrNull()
                        ?: monsterCell.replace(Regex("""\[\[|\]\]"""), "").trim().take(80)

                SlayerAssignmentRow(
                    rowIndex = -1,
                    slayerLevel = cells[0],
                    monsterCell = monsterCell,
                    superiorCell = cells[6],
                    alternativesCell = cells[7],
                    label = label,
                )
            }.mapIndexed { index, row -> row.copy(rowIndex = index + 1) }
    }

    fun extractMonsterVariantLinks(taskPageSource: String): List<String> {
        val section = WikiText.extractSectionByHeading(taskPageSource, monsterVariantsHeading) ?: taskPageSource
        return extractWikitableColumnLinks(section, linkColumnIndex = 0)
    }

    fun extractRevenantListLinks(source: String): List<String> {
        val section = WikiText.extractSectionByHeading(source, listOfRevenantsHeading) ?: source
        return extractWikitableColumnLinks(section, linkColumnIndex = 1)
    }

    fun extractMetalDragonLinks(source: String): List<String> {
        val section = WikiText.extractSectionByHeading(source, metalDragonsHeading) ?: source
        return extractWikitableColumnLinks(section, linkColumnIndex = 1, fallbackColumnIndex = 0)
    }

    fun extractProsePrimaryMonsterLink(source: String): String? =
        proseTaskMonster
            .find(source.take(4_000))
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.replace('_', ' ')

    private fun extractWikitableColumnLinks(
        section: String,
        linkColumnIndex: Int,
        fallbackColumnIndex: Int? = null,
    ): List<String> {
        val table = extractFirstWikitable(section) ?: return emptyList()
        return table
            .split(rowSplit)
            .drop(1)
            .flatMap { rowBlock ->
                monsterLinksFromRow(cellsFromRowForMonsterTables(rowBlock), linkColumnIndex, fallbackColumnIndex)
            }.distinct()
    }

    private fun monsterLinksFromRow(
        cells: List<String>,
        primaryIndex: Int,
        fallbackIndex: Int?,
    ): List<String> {
        val primary = linksFromCell(cells, primaryIndex)
        if (primary.isNotEmpty()) {
            return primary
        }
        if (fallbackIndex != null) {
            return linksFromCell(cells, fallbackIndex)
        }
        return emptyList()
    }

    private fun linksFromCell(cells: List<String>, index: Int): List<String> {
        if (cells.size <= index) {
            return emptyList()
        }
        return WikiLinks.extractPageTitles(cells[index]).filter { !it.startsWith("File:", ignoreCase = true) }
    }

    internal fun extractSection(wikitext: String, heading: String): String? {
        val normalized = heading.trim().removePrefix("==").removeSuffix("==").trim()
        val pattern = Regex("(?im)^==\\s*${Regex.escape(normalized)}\\s*==")
        val match = pattern.find(wikitext) ?: return null
        val afterHeading = wikitext.substring(match.range.last + 1)
        val nextHeading = Regex("""(?m)^==[^=].*==""").find(afterHeading)?.range?.first ?: afterHeading.length
        return afterHeading.substring(0, nextHeading)
    }

    internal fun extractFirstWikitable(section: String): String? {
        val start = section.indexOf("{|")
        if (start < 0) {
            return null
        }
        val end = section.indexOf("|}", start)
        if (end < 0) {
            return null
        }
        return section.substring(start, end + 2)
    }

    internal fun cellsFromRow(rowBlock: String): List<String> = cellsFromRowInternal(rowBlock, stripTemplates = false)

    private fun cellsFromRowForMonsterTables(rowBlock: String): List<String> =
        cellsFromRowInternal(rowBlock, stripTemplates = true)

    private fun cellsFromRowInternal(rowBlock: String, stripTemplates: Boolean): List<String> {
        val text = if (stripTemplates) WikiText.stripTemplates(rowBlock) else rowBlock
        return dataCellLine.findAll(text).map { it.groupValues[1].trim() }.filter { it != "}" }.toList()
    }
}

// ---------------------------------------------------------------------------
// Wikitext helpers
// ---------------------------------------------------------------------------

object WikiText {
    private val redirect = Regex("""#REDIRECT\s*\[\[([^|\]#]+)""", RegexOption.IGNORE_CASE)
    private val npcInfoboxStart = Regex("""\{\{Infobox\s+(Monster|NPC)\b""", RegexOption.IGNORE_CASE)
    private val slayerInfoboxStart = Regex("""\{\{Infobox\s+Slayer\b""", RegexOption.IGNORE_CASE)

    fun resolveRedirectTarget(source: String): String? =
        redirect
            .find(source.trim())
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?.replace('_', ' ')

    fun stripTemplates(text: String): String {
        val out = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            if (text.startsWith("{{", index)) {
                var depth = 0
                var cursor = index
                while (cursor < text.length - 1) {
                    if (text.startsWith("{{", cursor)) {
                        depth++
                    }
                    if (text.startsWith("}}", cursor)) {
                        depth--
                        if (depth == 0) {
                            cursor += 2
                            break
                        }
                    }
                    cursor++
                }
                index = cursor
            } else {
                out.append(text[index])
                index++
            }
        }
        return out.toString()
    }

    fun extractSectionByHeading(wikitext: String, headingPattern: Regex): String? {
        val match = headingPattern.find(wikitext) ?: return null
        val afterHeading = wikitext.substring(match.range.last + 1)
        val nextHeading = Regex("""(?m)^={2,3}[^=].*={2,3}""").find(afterHeading)?.range?.first ?: afterHeading.length
        return afterHeading.substring(0, nextHeading)
    }

    fun extractSectionForAnchor(wikitext: String, anchor: String): String? {
        val normalized = anchor.trim().replace('_', ' ')
        val headingPatterns =
            listOf(
                Regex("(?im)^===\\s*${Regex.escape(normalized)}\\s*==="),
                Regex("(?im)^==\\s*${Regex.escape(normalized)}\\s*=="),
            )
        for (pattern in headingPatterns) {
            extractSectionByHeading(wikitext, pattern)?.let { return it }
        }
        return null
    }

    fun extractNpcInfoboxSources(wikitext: String): String = extractInfoboxSources(wikitext, npcInfoboxStart)

    fun extractSlayerInfoboxSources(wikitext: String): String = extractInfoboxSources(wikitext, slayerInfoboxStart)

    private fun extractInfoboxSources(wikitext: String, startPattern: Regex): String {
        val blocks = mutableListOf<String>()
        var searchFrom = 0
        while (searchFrom < wikitext.length) {
            val match = startPattern.find(wikitext, searchFrom) ?: break
            val block = extractBalancedTemplate(wikitext, match.range.first)
            if (block != null) {
                blocks += block
            }
            searchFrom = match.range.last + 1
        }
        return blocks.joinToString("\n")
    }

    internal fun extractBalancedTemplate(source: String, startIndex: Int): String? {
        if (!source.startsWith("{{", startIndex)) {
            return null
        }
        var depth = 0
        var cursor = startIndex
        while (cursor < source.length - 1) {
            if (source.startsWith("{{", cursor)) {
                depth++
            }
            if (source.startsWith("}}", cursor)) {
                depth--
                if (depth == 0) {
                    cursor += 2
                    break
                }
            }
            cursor++
        }
        return if (cursor > startIndex) source.substring(startIndex, cursor) else null
    }
}

// ---------------------------------------------------------------------------
// Wiki links
// ---------------------------------------------------------------------------

object WikiTemplates {
    private val yesNo = Regex("""\{\{\s*(Yes|No)\s*\}\}""", RegexOption.IGNORE_CASE)

    fun parseYesNo(cell: String): Boolean? =
        yesNo
            .find(cell.trim())
            ?.groupValues
            ?.get(1)
            ?.equals("yes", ignoreCase = true)
}

object WikiLinks {
    private val wikiLink = Regex("""\[\[((?:[^\]|#]|\([^)]*\))+)(?:\|[^\]]*)?\]\]""")

    fun extractPageTitles(cell: String): List<String> {
        if (cell.isBlank() || isNaCell(cell)) {
            return emptyList()
        }
        val stripped = WikiText.stripTemplates(cell)
        return wikiLink
            .findAll(stripped)
            .map { it.groupValues[1].trim().replace('_', ' ') }
            .filter { title ->
                title.isNotBlank() &&
                    title !in setOf("NA", "No", "Yes") &&
                    !title.startsWith("File:", ignoreCase = true) &&
                    !title.startsWith("Category:", ignoreCase = true)
            }
            .distinct()
            .toList()
    }

    fun isSlayerTaskPage(title: String): Boolean = title.startsWith("Slayer task/", ignoreCase = true)

    fun isRevenantPage(title: String): Boolean =
        title.equals("Revenant", ignoreCase = true) || title.equals("Revenants", ignoreCase = true)

    fun isMetalDragonsPage(title: String, redirectAnchor: String?): Boolean {
        if (title.equals("Metal dragons", ignoreCase = true) || title.equals("Metal dragon", ignoreCase = true)) {
            return true
        }
        val anchor = redirectAnchor?.replace('_', ' ') ?: return false
        return anchor.contains("metal dragon", ignoreCase = true)
    }

    fun shouldSuppressEmptyFailure(title: String): Boolean {
        val lower = title.lowercase()
        return lower.contains("/strategies") ||
            lower.startsWith("calculator:") ||
            lower.endsWith(" reward point") ||
            title.equals("Grotesque Guardians", ignoreCase = true) ||
            title.equals("Bear", ignoreCase = true)
    }

    fun isNaCell(cell: String): Boolean {
        val trimmed = cell.trim()
        return trimmed.equals("N/A", ignoreCase = true) ||
            trimmed.equals("{{NA}}", ignoreCase = true) ||
            trimmed.equals("{{NA|}}", ignoreCase = true) ||
            trimmed.matches(Regex("""\{\{NA\|[^}]*\}\}""", RegexOption.IGNORE_CASE))
    }
}

// ---------------------------------------------------------------------------
// Infobox NPC ids
// ---------------------------------------------------------------------------

object WikiInfoboxIds {
    private val versionedIds =
        Regex("""\|\s*id(\d+)\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    private val bareId = Regex("""\|\s*id\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
    private val infoboxIdField =
        Regex("""(?im)^\s*\|id(\d*)\s*=\s*(.+?)\s*$""")

    fun allNpcIds(source: String): List<Int> = parseIds(WikiText.extractNpcInfoboxSources(source))

    fun allSlayerTaskIds(source: String): List<Int> = parseIds(WikiText.extractSlayerInfoboxSources(source))

    /** True when the infobox lists id fields but none are numeric (`removed`, `hist309`, `unreleased`, etc.). */
    fun hasNonNumericNpcId(source: String): Boolean {
        val infobox = WikiText.extractNpcInfoboxSources(source)
        if (infobox.isBlank()) {
            return false
        }
        val values = infoboxIdField.findAll(infobox).map { it.groupValues[2].trim() }.toList()
        if (values.isEmpty()) {
            return false
        }
        val tokens =
            values
                .flatMap { value -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
        if (tokens.isEmpty()) {
            return false
        }
        return tokens.none { it.toIntOrNull() != null }
    }

    private fun parseIds(infoboxSource: String): List<Int> {
        if (infoboxSource.isBlank()) {
            return emptyList()
        }
        val versioned =
            versionedIds
                .findAll(infoboxSource)
                .flatMap { parseIdList(it.groupValues[2]) }
                .toList()
        if (versioned.isNotEmpty()) {
            return versioned.distinct().sorted()
        }
        val bare = bareId.find(infoboxSource) ?: return emptyList()
        return parseIdList(bare.groupValues[1]).distinct().sorted()
    }

    private fun parseIdList(raw: String): List<Int> =
        raw.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }
}

// ---------------------------------------------------------------------------
// NPC resolver
// ---------------------------------------------------------------------------

class WikiNpcResolver(
    private val wiki: WikiClient,
    private val onPageFetch: ((String) -> Unit)? = null,
) {
    private val pageSourceCache = mutableMapOf<String, String>()
    private val redirectAnchorByTitle = mutableMapOf<String, String>()
    private val npcIdCache = mutableMapOf<String, List<Int>>()
    private val fetchedTitles = mutableSetOf<String>()

    val pagesFetched: Int get() = fetchedTitles.size

    suspend fun resolveDirectMonsterTitle(
        title: String,
        failures: MutableList<SlayerDumpFailure>,
        row: SuperiorMonsterRow? = null,
    ): DirectResolveResult {
        return when (val result = resolveTitle(title)) {
            is ResolveResult.Ok -> DirectResolveResult.Ok(result.npcIds)
            is ResolveResult.Empty -> {
                if (row != null) {
                    val label =
                        WikiLinks.extractPageTitles(row.normalCell).firstOrNull()
                            ?: "superior row ${row.rowIndex}"
                    recordFailure(
                        failures,
                        SlayerAssignmentRow(row.rowIndex, "", row.normalCell, "", "", label),
                        title,
                        result.reason,
                    )
                }
                DirectResolveResult.Empty
            }
            is ResolveResult.Error -> {
                if (row != null) {
                    failures +=
                        SlayerDumpFailure(row.rowIndex, row.normalCell, title, result.message)
                }
                DirectResolveResult.Error
            }
        }
    }

    sealed interface DirectResolveResult {
        data class Ok(val npcIds: List<Int>) : DirectResolveResult
        data object Empty : DirectResolveResult
        data object Error : DirectResolveResult
    }

    suspend fun resolveRow(
        row: SlayerAssignmentRow,
        failures: MutableList<SlayerDumpFailure>,
    ): List<Int> {
        val titles =
            buildList {
                addAll(WikiLinks.extractPageTitles(row.monsterCell))
                addAll(WikiLinks.extractPageTitles(row.alternativesCell))
            }.distinct()

        val ids = linkedSetOf<Int>()
        for (title in titles) {
            when (val result = resolveTitle(title)) {
                is ResolveResult.Ok -> ids += result.npcIds
                is ResolveResult.Empty -> recordFailure(failures, row, title, result.reason)
                is ResolveResult.Error ->
                    failures +=
                        SlayerDumpFailure(row.rowIndex, row.wikiTaskName(), title, result.message)
            }
        }
        return ids.sorted()
    }

    private suspend fun resolveTitle(title: String): ResolveResult {
        npcIdCache[title]?.let { cached ->
            return if (cached.isEmpty()) {
                ResolveResult.Empty("No NPC ids on cached page '$title'")
            } else {
                ResolveResult.Ok(cached)
            }
        }

        return try {
            val result =
                if (WikiLinks.isSlayerTaskPage(title)) {
                    resolveSlayerTaskPage(title)
                } else {
                    resolveDirectNpcPage(title)
                }
            npcIdCache[title] = result.npcIds
            result
        } catch (e: Exception) {
            ResolveResult.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun resolveSlayerTaskPage(taskTitle: String): ResolveResult {
        val source = fetchSource(taskTitle)
        val combined = linkedSetOf<Int>()

        for (link in WikiTables.extractMonsterVariantLinks(source)) {
            when (val nested = resolveTitle(link)) {
                is ResolveResult.Ok -> combined += nested.npcIds
                else -> Unit
            }
        }
        if (combined.isNotEmpty()) {
            return ResolveResult.Ok(combined.sorted())
        }

        combined += WikiInfoboxIds.allSlayerTaskIds(source)
        if (combined.isNotEmpty()) {
            return ResolveResult.Ok(combined.sorted())
        }

        val proseLink = WikiTables.extractProsePrimaryMonsterLink(source)
        if (proseLink != null) {
            when (val nested = resolveTitle(proseLink)) {
                is ResolveResult.Ok -> return ResolveResult.Ok(nested.npcIds)
                is ResolveResult.Empty ->
                    return ResolveResult.Empty("Prose link '$proseLink' on '$taskTitle' has no Infobox Monster ids")
                is ResolveResult.Error -> return nested
            }
        }

        return ResolveResult.Empty(
            "Slayer task '$taskTitle' has no Monster variants, Infobox Slayer ids, or prose monster link",
        )
    }

    private suspend fun resolveDirectNpcPage(title: String): ResolveResult {
        val source = fetchSource(title)

        if (WikiLinks.isRevenantPage(title)) {
            val ids = resolveRevenantList(source)
            if (ids.isNotEmpty()) {
                return ResolveResult.Ok(ids)
            }
        }

        if (WikiLinks.isMetalDragonsPage(title, redirectAnchorByTitle[title])) {
            val ids = resolveMetalDragonTable(source)
            if (ids.isNotEmpty()) {
                return ResolveResult.Ok(ids)
            }
        }

        val scoped =
            redirectAnchorByTitle[title]?.let { WikiText.extractSectionForAnchor(source, it) } ?: source
        val ids = WikiInfoboxIds.allNpcIds(scoped)
        if (ids.isNotEmpty()) {
            return ResolveResult.Ok(ids)
        }

        return ResolveResult.Empty("No |id= or |idN= in Infobox Monster on '$title'")
    }

    private suspend fun resolveRevenantList(source: String): List<Int> =
        resolveLinkedNpcIds(WikiTables.extractRevenantListLinks(source))

    private suspend fun resolveMetalDragonTable(source: String): List<Int> =
        resolveLinkedNpcIds(WikiTables.extractMetalDragonLinks(source))

    private suspend fun resolveLinkedNpcIds(links: List<String>): List<Int> {
        val combined = linkedSetOf<Int>()
        for (link in links) {
            when (val nested = resolveTitle(link)) {
                is ResolveResult.Ok -> combined += nested.npcIds
                else -> Unit
            }
        }
        return combined.sorted()
    }

    private suspend fun fetchSource(title: String, depth: Int = 0): String {
        pageSourceCache[title]?.let { return it }
        if (depth > 5) {
            throw IllegalStateException("Too many redirects resolving '$title'")
        }
        if (fetchedTitles.add(title)) {
            onPageFetch?.invoke(title)
        }
        val raw = wiki.rawPageSource(title)
        val redirectTarget = WikiText.resolveRedirectTarget(raw)
        if (redirectTarget != null) {
            val anchor = redirectTarget.substringAfter('#', "").trim().takeIf { it.isNotEmpty() }
            val baseTitle = redirectTarget.substringBefore('#').trim()
            if (anchor != null) {
                redirectAnchorByTitle[title] = anchor
            }
            val resolved = fetchSource(baseTitle, depth + 1)
            pageSourceCache[title] = resolved
            return resolved
        }
        pageSourceCache[title] = raw
        return raw
    }

    private fun recordFailure(
        failures: MutableList<SlayerDumpFailure>,
        row: SlayerAssignmentRow,
        pageTitle: String,
        reason: String,
    ) {
        if (WikiLinks.shouldSuppressEmptyFailure(pageTitle)) {
            return
        }
        failures += SlayerDumpFailure(row.rowIndex, row.wikiTaskName(), pageTitle, reason)
    }

    private sealed interface ResolveResult {
        val npcIds: List<Int>

        data class Ok(override val npcIds: List<Int>) : ResolveResult

        data class Empty(val reason: String) : ResolveResult {
            override val npcIds: List<Int> = emptyList()
        }

        data class Error(val message: String) : ResolveResult {
            override val npcIds: List<Int> = emptyList()
        }
    }
}
