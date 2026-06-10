package org.rsmod.tools.wiki.dumping.wiki

data class ParsedStoreLine(
    val name: String,
    val stock: Int,
    val restockCycles: Int,
    /** Alternate wiki item title from `bucketname=` when `name` lookup fails. */
    val lookupName: String? = null,
)

data class ParsedStoreTable(
    val sellMultiplier: Int?,
    val buyMultiplier: Int?,
    val delta: Int?,
    val nameNotes: String?,
    val hiddenStock: Boolean = false,
    val lines: List<ParsedStoreLine>,
)

object WikiShopStoreParser {
    private val stockHeading = Regex("""(?im)^==\s*Stock\s*==""")
    /** Ends a ==Stock== section; does not stop at nested === subsections ===. */
    private val level2Heading = Regex("""(?m)^==\s*[^=].*==""")
    private val level3Heading = Regex("""(?im)^===\s*([^=]+?)\s*===""")
    private val foodStoreHeading = Regex("""(?im)^===\s*Food store\s*===""")
    private val itemsStoreHeading = Regex("""(?im)^===\s*Items store\s*===""")
    private val easternBakerHeading = Regex("""(?im)^===\s*Eastern Baker\s*===""")
    private val westernBakerHeading = Regex("""(?im)^===\s*Western Baker\s*===""")
    private val level3HeadingEnd = Regex("""(?m)^===\s*[^=].*===""")

    fun skillcapeTrimmed(inv: String): Boolean? =
        when {
            inv.endsWith("_skillcape_trimmed") -> true
            inv.endsWith("_skillcape") -> false
            else -> null
        }

    /**
     * Skillcape shop invs duplicate the regular stock plus a cape-only table under
     * `===Skill cape===` / `===Skill cape(t)===` in `==Stock==`.
     */
    fun parseSkillcapeShop(wikitext: String, trimmed: Boolean): ParsedStoreTable? {
        val stockSection = extractStockSection(wikitext) ?: return null
        val (baseBody, capeBody) = splitSkillcapeStockSection(stockSection, trimmed) ?: return null

        val baseTable =
            parseTables(baseBody).firstOrNull { !it.hiddenStock && it.lines.isNotEmpty() }
                ?: return null
        val capeLines = capeBody?.let { body -> parseTables(body).firstOrNull()?.lines }.orEmpty()
        if (capeLines.isEmpty()) {
            return null
        }

        return baseTable.copy(lines = baseTable.lines + capeLines)
    }

    private fun splitSkillcapeStockSection(stockSection: String, trimmed: Boolean): Pair<String, String?>? {
        for (match in level3Heading.findAll(stockSection)) {
            val heading = match.groupValues[1].trim()
            if (!heading.contains("cape", ignoreCase = true)) {
                continue
            }
            val isTrimmedHeading = heading.contains("(t)", ignoreCase = true)
            if (trimmed != isTrimmedHeading) {
                continue
            }

            val baseBody = stockSection.substring(0, match.range.first)
            val afterHeading = stockSection.substring(match.range.last + 1)
            val nextHeading = level3Heading.find(afterHeading)?.range?.first ?: afterHeading.length
            return baseBody to afterHeading.substring(0, nextHeading)
        }
        return null
    }

    fun parseSelectedTable(wikitext: String, wikiStore: String?): ParsedStoreTable? {
        val (sectionKey, subKey) = parseWikiStoreSelector(wikiStore)
        val level2Body = resolveLevel2Section(wikitext, sectionKey) ?: return null

        val narrowedBody =
            subKey?.let { key ->
                extractLevel3Section(level2Body, key)
                    ?: extractTabberTab(level2Body, key)
                    ?: extractPlainTabberTab(level2Body, key)
                    ?: extractLevel2Section(wikitext, key)
            }

        val body = narrowedBody ?: level2Body
        val tables = parseTables(body)
        if (tables.isNotEmpty()) {
            return if (narrowedBody != null) {
                selectTable(tables, null)
            } else {
                selectTable(tables, subKey)
            }
        }

        val wikitableLines = parsePlinktWikitable(body)
        if (wikitableLines.isNotEmpty()) {
            return ParsedStoreTable(
                sellMultiplier = null,
                buyMultiplier = null,
                delta = null,
                nameNotes = subKey,
                lines = wikitableLines,
            )
        }

        return null
    }

    private fun resolveLevel2Section(wikitext: String, sectionKey: String?): String? =
        when (sectionKey?.lowercase()) {
            "food" -> WikiText.extractSectionByHeading(wikitext, foodStoreHeading)
            "items" -> WikiText.extractSectionByHeading(wikitext, itemsStoreHeading)
            "east", "eastern" -> WikiText.extractSectionByHeading(wikitext, easternBakerHeading)
            "west", "western" -> WikiText.extractSectionByHeading(wikitext, westernBakerHeading)
            "blackjacks" -> extractLevel2Section(wikitext, "Blackjacks")
            "runes" -> extractLevel2Section(wikitext, "Runes")
            "clothing" -> extractLevel2Section(wikitext, "Clothing")
            "basic", "basic stock" -> extractLevel2Section(wikitext, "Basic stock")
            null -> extractStockSection(wikitext) ?: wikitext
            else -> extractLevel2Section(wikitext, sectionKey) ?: extractStockSection(wikitext) ?: wikitext
        }

    private fun extractLevel2Section(wikitext: String, heading: String): String? {
        val pattern = Regex("""(?im)^==\s*${Regex.escape(heading.trim())}\s*==""")
        val match = pattern.find(wikitext) ?: return null
        val afterHeading = wikitext.substring(match.range.last + 1)
        val nextHeading = level2Heading.find(afterHeading)?.range?.first ?: afterHeading.length
        return afterHeading.substring(0, nextHeading)
    }

    private fun extractLevel3Section(body: String, heading: String): String? {
        val pattern = Regex("""(?im)^===\s*${Regex.escape(heading.trim())}\s*===""")
        val match = pattern.find(body) ?: return null
        val afterHeading = body.substring(match.range.last + 1)
        val nextHeading = level3HeadingEnd.find(afterHeading)?.range?.first ?: afterHeading.length
        return afterHeading.substring(0, nextHeading)
    }

    /** Tabber tabs written as `Label=` / `|-|Label=` without a `<tabber>` wrapper (e.g. Daga's Scimitar Smithy). */
    private fun extractPlainTabberTab(body: String, tabName: String): String? {
        val target = normalizeNameNotes(tabName)
        for (chunk in body.split(Regex("""\|-\|"""))) {
            val equalsIndex = chunk.indexOf('=')
            if (equalsIndex <= 0) {
                continue
            }
            val label = normalizeNameNotes(chunk.substring(0, equalsIndex))
            if (label == target || label.contains(target) || target.contains(label)) {
                return chunk.substring(equalsIndex + 1)
            }
        }
        return null
    }

    private fun extractTabberTab(body: String, tabName: String): String? {
        val tabberOpen = body.indexOf("<tabber>", ignoreCase = true)
        if (tabberOpen < 0) {
            return null
        }
        val contentStart = tabberOpen + "<tabber>".length
        val tabberClose = body.indexOf("</tabber>", contentStart, ignoreCase = true)
        val tabberContent =
            if (tabberClose >= 0) {
                body.substring(contentStart, tabberClose)
            } else {
                body.substring(contentStart)
            }

        val target = normalizeNameNotes(tabName)
        for (chunk in tabberContent.split(Regex("""\|-\|"""))) {
            val equalsIndex = chunk.indexOf('=')
            if (equalsIndex <= 0) {
                continue
            }
            val label = normalizeNameNotes(chunk.substring(0, equalsIndex))
            if (label == target || label.contains(target) || target.contains(label)) {
                return chunk.substring(equalsIndex + 1)
            }
        }
        return null
    }

    private fun extractStockSection(wikitext: String): String? {
        val match = stockHeading.find(wikitext) ?: return null
        val afterHeading = wikitext.substring(match.range.last + 1)
        val nextHeading = level2Heading.find(afterHeading)?.range?.first ?: afterHeading.length
        return afterHeading.substring(0, nextHeading)
    }

    /** Crafting/service panels documented as wikitables with `{{plinkt|…}}` rows (e.g. Custom Fur Clothing). */
    private fun parsePlinktWikitable(body: String): List<ParsedStoreLine> {
        if (!body.contains("{|", ignoreCase = false)) {
            return emptyList()
        }
        val plinktPattern = Regex("""\{\{plinkt\|([^}|]+)""", RegexOption.IGNORE_CASE)
        return plinktPattern
            .findAll(body)
            .map { match ->
                ParsedStoreLine(
                    name = sanitizeWikiText(match.groupValues[1]),
                    stock = 0,
                    restockCycles = 100,
                )
            }
            .distinctBy { it.name }
            .toList()
    }

    fun parseTables(body: String): List<ParsedStoreTable> {
        val tables = mutableListOf<ParsedStoreTable>()
        var index = 0

        while (index < body.length) {
            val headStart = body.indexOf("{{StoreTableHead", index, ignoreCase = true)
            if (headStart < 0) {
                break
            }

            val headBlock =
                WikiText.extractBalancedTemplate(body, headStart)
                    ?: break
            val headContent = stripTemplateHeader(headBlock, "StoreTableHead")
            val headParams = WikiTemplateParser.parseParams(headContent)
            val lines = mutableListOf<ParsedStoreLine>()
            index = headStart + headBlock.length

            while (index < body.length) {
                val nextHead = body.indexOf("{{StoreTableHead", index, ignoreCase = true)
                val bottomStart = body.indexOf("{{StoreTableBottom", index, ignoreCase = true)
                val lineStart = body.indexOf("{{StoreLine", index, ignoreCase = true)

                val nextStop =
                    listOfNotNull(
                        nextHead.takeIf { it >= 0 },
                        bottomStart.takeIf { it >= 0 },
                    ).minOrNull()

                if (lineStart < 0 || (nextStop != null && lineStart > nextStop)) {
                    if (bottomStart >= 0) {
                        val bottomBlock = WikiText.extractBalancedTemplate(body, bottomStart) ?: break
                        index = bottomStart + bottomBlock.length
                    }
                    break
                }

                val lineBlock =
                    WikiText.extractBalancedTemplate(body, lineStart)
                        ?: break
                val lineContent = stripTemplateHeader(lineBlock, "StoreLine")
                val lineParams = WikiTemplateParser.parseParams(lineContent)
                val name = sanitizeWikiText(lineParams["name"].orEmpty())
                val bucketName =
                    lineParams["bucketname"]
                        ?.let(::sanitizeBucketName)
                        ?.takeIf { it.isNotBlank() }
                if (name.isNotBlank()) {
                    val stock = parseStockCount(lineParams["stock"])
                    if (stock == null) {
                        index = lineStart + lineBlock.length
                        continue
                    }
                    lines +=
                        ParsedStoreLine(
                            name = name,
                            stock = stock,
                            restockCycles = lineParams["restock"]?.toIntOrNull() ?: 100,
                            lookupName = bucketName,
                        )
                }
                index = lineStart + lineBlock.length
            }

            tables +=
                ParsedStoreTable(
                    sellMultiplier = headParams["sellmultiplier"]?.toIntOrNull(),
                    buyMultiplier = headParams["buymultiplier"]?.toIntOrNull(),
                    delta = headParams["delta"]?.toIntOrNull(),
                    nameNotes = headParams["namenotes"]?.let(::sanitizeWikiText)?.takeIf { it.isNotBlank() },
                    hiddenStock =
                        headParams["hidestock"]?.equals("y", ignoreCase = true) == true ||
                            headParams["hiderestock"]?.equals("y", ignoreCase = true) == true,
                    lines = lines,
                )
        }

        return tables
    }

    private fun parseWikiStoreSelector(wikiStore: String?): Pair<String?, String?> {
        val trimmed = wikiStore?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null to null
        }
        if (!trimmed.contains('|')) {
            // Rank/tab selector only, e.g. White Knight Armoury "Novice" tab.
            return null to trimmed
        }
        val parts = trimmed.split('|', limit = 2)
        val section = parts[0].trim().takeIf { it.isNotBlank() }
        val nameNotes = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        return section to nameNotes
    }

    private fun selectTable(tables: List<ParsedStoreTable>, nameNotesKey: String?): ParsedStoreTable? {
        if (tables.isEmpty()) {
            return null
        }
        if (nameNotesKey.isNullOrBlank()) {
            return tables.firstOrNull { !it.hiddenStock && it.lines.isNotEmpty() }
                ?: tables.firstOrNull { normalizeNameNotes(it.nameNotes.orEmpty()) == "full" }
                ?: tables.firstOrNull { !it.hiddenStock }
                ?: tables.first()
        }

        val target = normalizeNameNotes(nameNotesKey)
        return tables.firstOrNull { normalizeNameNotes(it.nameNotes.orEmpty()) == target }
            ?: tables.firstOrNull {
                normalizeNameNotes(it.nameNotes.orEmpty()).contains(target) ||
                    target.contains(normalizeNameNotes(it.nameNotes.orEmpty()))
            }
    }

    fun normalizeNameNotes(value: String): String =
        value
            .trim()
            .removePrefix("(")
            .removeSuffix(")")
            .trim()
            .lowercase()

    private fun stripTemplateHeader(block: String, templateName: String): String {
        val prefix = "{{$templateName|"
        val altPrefix = "{{$templateName}}"
        return when {
            block.startsWith(prefix, ignoreCase = true) -> block.substring(prefix.length).removeSuffix("}}").trim()
            block.startsWith(altPrefix, ignoreCase = true) -> ""
            else -> block.removePrefix("{{").substringAfter('|').removeSuffix("}}").trim()
        }
    }

    private fun sanitizeWikiText(input: String): String =
        input
            .replace(Regex("""\[\[([^|\]]+)\|([^\]]+)]]"""), "$2")
            .replace(Regex("""\[\[([^\]]+)]]"""), "$1")
            .replace("'''", "")
            .replace("''", "")
            .trim()

    private fun sanitizeBucketName(input: String): String {
        val withoutAnchor = input.substringBefore('#').trim()
        return sanitizeWikiText(withoutAnchor)
    }

    private fun parseStockCount(raw: String?): Int? {
        val value = raw?.trim().orEmpty()
        if (value.isBlank() || value.equals("inf", ignoreCase = true) || value.equals("n/a", ignoreCase = true)) {
            return null
        }
        return value.toIntOrNull()
    }
}
