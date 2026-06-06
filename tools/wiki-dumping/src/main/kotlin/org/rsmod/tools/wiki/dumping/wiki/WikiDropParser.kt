package org.rsmod.tools.wiki.dumping.wiki

enum class WikiDropSection {
    Guaranteed,
    Main,
    Tertiary,
}

data class ParsedWikiDrop(
    val name: String,
    val quantity: String,
    val rarity: String,
    val section: WikiDropSection,
    val subsection: String = "",
    val wikiNotes: WikiDropNotes = WikiDropNotes(),
    val isNothing: Boolean = false,
    val isNoted: Boolean = false,
)

data class ParsedNpcDropTable(
    val tableName: String,
    val dropVariant: String = "",
    val drops: List<ParsedWikiDrop>,
    val subtableAccesses: List<ParsedSubtableAccess>,
    val npcIds: List<Int>,
)

object WikiDropParser {
    private val npcIdPattern = Regex("""(?im)\|id(\d+)\s*=\s*([0-9,\s]+)""")
    private val dropVersionPattern = Regex("""(?im)\|dropversion(\d+)\s*=\s*(.+)$""")
    private val dropTableHeadingPattern = Regex("""(?m)^==(Drop table \d+|Drops)==\s*$""")
    private val dropVariantHeadingPattern = Regex("""(?m)^={3}(?!=)\s*(.+?)\s*={3}(?!=)\s*$""")

    fun parseNpcIds(wikitext: String): List<Int> = WikiInfoboxIds.allNpcIds(wikitext)

    /** Maps wiki drop-table section names (e.g. `Drop table 1`) to npc ids from the infobox. */
    fun parseNpcIdsByDropTable(wikitext: String): Map<String, List<Int>> {
        val versions =
            dropVersionPattern
                .findAll(wikitext)
                .associate { match ->
                    match.groupValues[1].toInt() to match.groupValues[2].trim()
                }
        if (versions.isEmpty()) {
            return emptyMap()
        }

        val idsByIndex =
            npcIdPattern
                .findAll(wikitext)
                .associate { match ->
                    match.groupValues[1].toInt() to parseIdList(match.groupValues[2])
                }

        val grouped = mutableMapOf<String, MutableList<Int>>()
        for ((index, tableName) in versions) {
            val ids = idsByIndex[index] ?: continue
            grouped.getOrPut(tableName) { mutableListOf() }.addAll(ids)
        }

        return grouped.mapValues { (_, ids) -> ids.distinct().sorted() }
    }

    fun parseAllDropTables(wikitext: String): List<ParsedNpcDropTable> {
        val sections = parseDropTableSections(wikitext)
        if (sections.isEmpty()) {
            return emptyList()
        }

        val npcByTable = parseNpcIdsByDropTable(wikitext)
        val fallbackNpcIds = parseNpcIds(wikitext)

        return sections.flatMap { (tableName, body) ->
            val variants =
                if (tableName.equals("Drops", ignoreCase = true)) {
                    parseDropVariants(body, wikitext)
                } else {
                    listOf("" to body)
                }

            variants.map { (variantName, variantBody) ->
                val npcIds =
                    when {
                        variantName.isNotBlank() ->
                            npcIdsForDropVariant(wikitext, variantName, fallbackNpcIds)
                        npcByTable.containsKey(tableName) -> npcByTable.getValue(tableName)
                        sections.size == 1 && variants.size == 1 -> fallbackNpcIds
                        else -> emptyList()
                    }

                ParsedNpcDropTable(
                    tableName = tableName,
                    dropVariant = variantName,
                    drops = parseDropsFromBody(variantBody),
                    subtableAccesses = parseSubtableAccessesFromBody(variantBody),
                    npcIds = npcIds,
                )
            }
        }
    }

    /** Splits `==Drops==` on infobox drop-version headings (e.g. `===Wilderness Slayer Cave===`), not item subsections. */
    fun parseDropVariants(dropsSectionBody: String, wikitext: String): List<Pair<String, String>> {
        val dropVersions = parseDropVersionNames(wikitext)
        if (dropVersions.isEmpty()) {
            return listOf("" to dropsSectionBody)
        }

        val matches = dropVariantHeadingPattern.findAll(dropsSectionBody).toList()
        if (matches.isEmpty()) {
            return listOf("" to dropsSectionBody)
        }

        val variantMatches =
            matches.filter { match ->
                matchesDropVersionVariant(match.groupValues[1].trim(), dropVersions)
            }
        if (variantMatches.size < 2) {
            return listOf("" to dropsSectionBody)
        }

        return variantMatches.mapIndexed { index, match ->
            val variantName = match.groupValues[1].trim()
            val start = match.range.last + 1
            val end = variantMatches.getOrNull(index + 1)?.range?.first ?: dropsSectionBody.length
            variantName to dropsSectionBody.substring(start, end)
        }
    }

    fun parseDropVersionNames(wikitext: String): List<String> =
        dropVersionPattern
            .findAll(wikitext)
            .map { it.groupValues[2].trim() }
            .filter { it.isNotEmpty() }
            .toList()

    internal fun matchesDropVersionVariant(heading: String, dropVersions: List<String>): Boolean {
        if (isStandardDropSubsection(heading) || dropVersions.isEmpty()) {
            return false
        }

        if (dropVersions.any { version -> headingMatchesDropVersion(heading, version) }) {
            return true
        }

        val tokens =
            heading
                .split(Regex("""\s+and\s+""", RegexOption.IGNORE_CASE))
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        return tokens.size >= 2 &&
            tokens.all { token ->
                dropVersions.any { version -> headingMatchesDropVersion(token, version) }
            }
    }

    private fun headingMatchesDropVersion(heading: String, version: String): Boolean =
        heading.equals(version, ignoreCase = true) ||
            heading.contains(version, ignoreCase = true) ||
            version.contains(heading, ignoreCase = true)

    internal fun isStandardDropSubsection(heading: String): Boolean {
        val normalized = heading.lowercase()
        return when {
            normalized.contains("100%") || normalized.contains("always") -> true
            normalized.contains("weapon") && normalized.contains("armour") -> true
            normalized.contains("rune") && normalized.contains("ammunition") -> true
            normalized == "herbs" || normalized.endsWith(" herb") || normalized.endsWith(" herbs") -> true
            normalized.contains("herb drop") -> true
            normalized == "materials" || normalized == "coins" || normalized == "other" -> true
            normalized.contains("tertiary") -> true
            normalized.contains("rare drop table") || normalized.contains("gem drop table") -> true
            normalized == "seeds" || normalized.contains("seed drop") -> true
            else -> false
        }
    }

    /** Resolves infobox `dropversion` npc ids for a wiki variant heading such as `Wilderness Slayer Cave`. */
    fun npcIdsForDropVariant(
        wikitext: String,
        variantName: String,
        fallbackNpcIds: List<Int>,
    ): List<Int> {
        val versionToIds = parseNpcIdsByDropTable(wikitext)
        if (versionToIds.isEmpty()) {
            return fallbackNpcIds
        }

        val tokens =
            variantName
                .split(Regex("""\s+and\s+""", RegexOption.IGNORE_CASE))
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        val matched = mutableListOf<Int>()
        for ((versionName, ids) in versionToIds) {
            val versionMatched =
                tokens.any { token ->
                    versionName.equals(token, ignoreCase = true) ||
                        versionName.contains(token, ignoreCase = true) ||
                        token.contains(versionName, ignoreCase = true)
                }
            if (versionMatched) {
                matched.addAll(ids)
            }
        }

        return matched.distinct().sorted().ifEmpty { fallbackNpcIds }
    }

    fun parseSubtableAccesses(wikitext: String): List<ParsedSubtableAccess> {
        val dropsSection = extractSection(wikitext, "==Drops==") ?: return emptyList()
        return parseSubtableAccessesFromBody(dropsSection)
    }

    private fun parseSubtableAccessesFromBody(body: String): List<ParsedSubtableAccess> {
        val parsed = mutableListOf<ParsedSubtableAccess>()

        var herbRollVariants: List<HerbRollVariant>? = null

        for (content in WikiTemplateParser.extractTemplates(body, "HerbDropTableInfo")) {
            val params = WikiTemplateParser.parseParams(content)
            val text = params["override"] ?: content
            HerbDropTableParser.parseMainAccess(text)?.let { (numerator, denominator) ->
                herbRollVariants = HerbDropTableParser.parseRollVariants(text) ?: herbRollVariants
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = herbTableKey(herbRollVariants),
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Herbs",
                        herbRollVariants = herbRollVariants,
                    )
            }
        }

        if (parsed.none { it.tableKey == SubtableKey.HERB || it.tableKey == SubtableKey.HERB_MULTI }) {
            for (content in WikiTemplateParser.extractTemplates(body, "HerbDropLines")) {
                val params = WikiTemplateParser.parseParams(content)
                val chance = params["_0"]?.trim().orEmpty().ifBlank { content.split('|').firstOrNull()?.trim().orEmpty() }
                HerbDropTableParser.parseMainAccess(chance)?.let { (numerator, denominator) ->
                    val quantity = params["_1"]?.trim().orEmpty()
                    val variants =
                        herbRollVariants
                            ?: if (HerbDropTableParser.isVariableQuantityRange(quantity)) {
                                HerbDropTableParser.standardMultiRollVariants
                            } else {
                                null
                            }
                    herbRollVariants = variants
                    parsed +=
                        ParsedSubtableAccess(
                            tableKey = herbTableKey(variants),
                            numerator = numerator,
                            denominator = denominator,
                            subsection = "Herbs",
                            herbRollVariants = variants,
                        )
                }
                break
            }
        }

        for (content in WikiTemplateParser.extractTemplates(body, "GemDropTable")) {
            val chance = content.split('|').firstOrNull()?.trim().orEmpty()
            parseAccessChance(chance)?.let { (numerator, denominator) ->
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = SubtableKey.GEM,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Gem drop table",
                    )
            }
        }

        parsed += parseRareDropTableAccesses(body, parsed)
        parsed += parseUsefulHerbAccesses(body, parsed)
        parsed += parseCombatHerbAccesses(body, parsed)
        parseGeneralSeedAccess(body)?.let { parsed += it }

        for (content in WikiTemplateParser.extractTemplates(body, "RareSeedDropTableInfo")) {
            parseAccessChance(content)?.let { (numerator, denominator) ->
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = SubtableKey.RARE_SEED,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Seeds",
                    )
            }
        }

        if (parsed.none { it.tableKey == SubtableKey.RARE_SEED }) {
            for (content in WikiTemplateParser.extractTemplates(body, "RareSeedDropLines")) {
                val chance = content.split('|').firstOrNull()?.trim().orEmpty()
                parseAccessChance(chance)?.let { (numerator, denominator) ->
                    parsed +=
                        ParsedSubtableAccess(
                            tableKey = SubtableKey.RARE_SEED,
                            numerator = numerator,
                            denominator = denominator,
                            subsection = "Seeds",
                        )
                }
                break
            }
        }

        parsed += parseProseSubtableAccesses(body, parsed)

        return parsed
    }

    private fun parseRareDropTableAccesses(
        body: String,
        existing: List<ParsedSubtableAccess>,
    ): List<ParsedSubtableAccess> {
        if (existing.any { it.tableKey == SubtableKey.RDT }) {
            return emptyList()
        }

        val parsed = mutableListOf<ParsedSubtableAccess>()
        for (content in WikiTemplateParser.extractTemplates(body, "RareDropTable")) {
            val params = WikiTemplateParser.parseParams(content)
            val rdtChance = params["_0"]?.let(::parseAccessChance) ?: parseAccessChance(content)
            rdtChance?.let { (numerator, denominator) ->
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = SubtableKey.RDT,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Rare drop table",
                    )
            }

            params["_1"]?.let(::parseAccessChance)?.let { (numerator, denominator) ->
                if (existing.none { it.tableKey == SubtableKey.GEM } &&
                    parsed.none { it.tableKey == SubtableKey.GEM }
                ) {
                    parsed +=
                        ParsedSubtableAccess(
                            tableKey = SubtableKey.GEM,
                            numerator = numerator,
                            denominator = denominator,
                            subsection = "Gem drop table",
                        )
                }
            }
        }
        return parsed
    }

    private fun parseUsefulHerbAccesses(
        body: String,
        existing: List<ParsedSubtableAccess>,
    ): List<ParsedSubtableAccess> {
        if (existing.any { it.tableKey == SubtableKey.USEFUL_HERB }) {
            return emptyList()
        }

        val parsed = mutableListOf<ParsedSubtableAccess>()
        for (content in WikiTemplateParser.extractTemplates(body, "UsefulHerbDropTableInfo")) {
            parseAccessChance(content)?.let { (numerator, denominator) ->
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = SubtableKey.USEFUL_HERB,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Herbs",
                    )
            }
        }

        if (parsed.isEmpty()) {
            for (content in WikiTemplateParser.extractTemplates(body, "UsefulHerbDropLines")) {
                val chance = content.split('|').firstOrNull()?.trim().orEmpty()
                parseAccessChance(chance)?.let { (numerator, denominator) ->
                    parsed +=
                        ParsedSubtableAccess(
                            tableKey = SubtableKey.USEFUL_HERB,
                            numerator = numerator,
                            denominator = denominator,
                            subsection = "Herbs",
                        )
                }
                break
            }
        }
        return parsed
    }

    private fun parseCombatHerbAccesses(
        body: String,
        existing: List<ParsedSubtableAccess>,
    ): List<ParsedSubtableAccess> {
        if (existing.any { it.tableKey == SubtableKey.COMBAT_HERB }) {
            return emptyList()
        }

        val parsed = mutableListOf<ParsedSubtableAccess>()
        for (content in WikiTemplateParser.extractTemplates(body, "CombatHerbDropTableInfo")) {
            parseAccessChance(content)?.let { (numerator, denominator) ->
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = SubtableKey.COMBAT_HERB,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Herbs",
                    )
            }
        }

        if (parsed.isEmpty()) {
            for (content in WikiTemplateParser.extractTemplates(body, "CombatHerbDropLines")) {
                val chance = content.split('|').firstOrNull()?.trim().orEmpty()
                parseAccessChance(chance)?.let { (numerator, denominator) ->
                    parsed +=
                        ParsedSubtableAccess(
                            tableKey = SubtableKey.COMBAT_HERB,
                            numerator = numerator,
                            denominator = denominator,
                            subsection = "Herbs",
                        )
                }
                break
            }
        }
        return parsed
    }

    private fun parseProseSubtableAccesses(
        body: String,
        existing: List<ParsedSubtableAccess>,
    ): List<ParsedSubtableAccess> {
        val parsed = mutableListOf<ParsedSubtableAccess>()
        val prosePatterns =
            listOf(
                SubtableKey.HERB to Regex("""(\d+)\s*/\s*(\d+)\s+chance of rolling the herb drop table""", RegexOption.IGNORE_CASE),
                SubtableKey.USEFUL_HERB to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the useful herb drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
                SubtableKey.COMBAT_HERB to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the combat herb drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
                SubtableKey.GEM to Regex("""(\d+)\s*/\s*(\d+)\s+chance of rolling the gem drop table""", RegexOption.IGNORE_CASE),
                SubtableKey.SEED to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the general seed drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
                SubtableKey.RARE_SEED to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the rare seed drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
                SubtableKey.RDT to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the rare drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
                SubtableKey.MEGA_RARE to
                    Regex(
                        """(\d+)\s*/\s*(\d+)\s+chance of rolling the mega-?rare drop table""",
                        RegexOption.IGNORE_CASE,
                    ),
            )

        for ((key, pattern) in prosePatterns) {
            if (existing.any { it.tableKey == key } || parsed.any { it.tableKey == key }) {
                continue
            }
            for (match in pattern.findAll(body)) {
                val numerator = match.groupValues[1].toIntOrNull() ?: continue
                val denominator = match.groupValues[2].toIntOrNull() ?: continue
                parsed +=
                    ParsedSubtableAccess(
                        tableKey = key,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = key.wikiLabel,
                        fromProse = true,
                    )
            }
        }

        return parsed
    }

    fun parseDrops(wikitext: String): List<ParsedWikiDrop> {
        val dropsSection = extractSection(wikitext, "==Drops==") ?: return emptyList()
        return parseDropsFromBody(dropsSection)
    }

    private fun parseDropsFromBody(body: String): List<ParsedWikiDrop> {
        val subsections = splitDropSubsections(body)
        val parsed = mutableListOf<ParsedWikiDrop>()

        for ((heading, sectionBody) in subsections) {
            val section = classifySection(heading)
            parsed += parseTemplatesInSection(sectionBody, section, heading)
        }

        return parsed
    }

    private fun parseDropTableSections(wikitext: String): List<Pair<String, String>> {
        val matches = dropTableHeadingPattern.findAll(wikitext).toList()
        if (matches.isEmpty()) {
            return emptyList()
        }

        return matches.mapIndexed { index, match ->
            val tableName = match.groupValues[1].trim()
            val start = match.range.last + 1
            val end =
                matches.getOrNull(index + 1)?.range?.first
                    ?: Regex("""(?m)^==[^=].*==""").find(wikitext, start)?.range?.first
                    ?: wikitext.length
            tableName to wikitext.substring(start, end)
        }
    }

    private fun parseIdList(raw: String): List<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }

    private fun classifySection(heading: String): WikiDropSection {
        val normalized = heading.lowercase()
        return when {
            normalized.contains("100%") || normalized.contains("always") -> WikiDropSection.Guaranteed
            normalized.contains("tertiary") -> WikiDropSection.Tertiary
            else -> WikiDropSection.Main
        }
    }

    private fun splitDropSubsections(dropsSection: String): List<Pair<String, String>> {
        val headingPattern = Regex("""(?m)^={3,4}(?!=)\s*(.+?)\s*={3,4}(?!=)\s*$""")
        val matches = headingPattern.findAll(dropsSection).toList()
        if (matches.isEmpty()) {
            return listOf("" to dropsSection)
        }

        return matches.mapIndexed { index, match ->
            val heading = match.groupValues[1].trim()
            val start = match.range.last + 1
            val end = matches.getOrNull(index + 1)?.range?.first ?: dropsSection.length
            heading to dropsSection.substring(start, end)
        }
    }

    private fun parseTemplatesInSection(
        body: String,
        section: WikiDropSection,
        subsection: String,
    ): List<ParsedWikiDrop> {
        val parsed = mutableListOf<ParsedWikiDrop>()
        val namedRefs = collectSectionNamedGroupDFootnotes(body)

        for (content in WikiTemplateParser.extractTemplates(body, "DropsLine")) {
            val params = WikiTemplateParser.parseParams(content)
            toParsedDrop(params, section, subsection, namedRefs)?.let(parsed::add)
        }

        for (content in WikiTemplateParser.extractTemplates(body, "DropsLineClue")) {
            val params = WikiTemplateParser.parseParams(content)
            if (
                WikiDropNoteClassifier.shouldSkipF2pOnlyDrop(
                    params,
                    parseInlineNoteFields(params["raritynotes"].orEmpty()),
                )
            ) {
                continue
            }
            val clueType = params["type"] ?: continue
            val rarity = params["rarity"] ?: params["raritynotes"] ?: continue
            val dropName = "Clue scroll ($clueType)"
            parsed +=
                ParsedWikiDrop(
                    name = dropName,
                    quantity = "1",
                    rarity = rarity,
                    section = section,
                    subsection = subsection,
                    wikiNotes =
                        DropsLineClueNotes.build(
                            dropName = dropName,
                            clueType = clueType,
                            rarity = rarity,
                            noteOverride = params["noteoverride"],
                            rarityNotes = params["raritynotes"],
                        ),
                )
        }

        return parsed
    }

    private fun toParsedDrop(
        params: Map<String, String>,
        section: WikiDropSection,
        subsection: String,
        namedRefs: Map<String, String> = emptyMap(),
    ): ParsedWikiDrop? {
        val name = params["name"]?.trim().orEmpty()
        if (name.isBlank()) {
            return null
        }

        val nameNotes = params["namenotes"].orEmpty()
        val rarityNotes = params["raritynotes"].orEmpty()
        val attachedNotes =
            buildList {
                addAll(parseInlineNoteFields(nameNotes, rarityNotes))
                addAll(resolveNamedGroupDRefLinks(nameNotes, rarityNotes, namedRefs))
            }
        if (WikiDropNoteClassifier.shouldSkipF2pOnlyDrop(params, attachedNotes)) {
            return null
        }

        if (name.equals("Nothing", ignoreCase = true)) {
            val rarity = params["rarity"]?.trim().orEmpty().ifBlank { params["raritynotes"].orEmpty() }
            if (rarity.isBlank()) {
                return null
            }
            return ParsedWikiDrop(
                name = name,
                quantity = "1",
                rarity = rarity,
                section = section,
                subsection = subsection,
                isNothing = true,
            )
        }

        val rawQuantity = params["quantity"]?.trim().orEmpty().ifBlank { "1" }
        val (quantity, isNoted) = parseWikiQuantity(rawQuantity)
        val rarity = params["rarity"]?.trim().orEmpty().ifBlank { params["raritynotes"].orEmpty() }
        if (rarity.isBlank()) {
            return null
        }

        var wikiNotes =
            WikiDropNoteClassifier.classifyAll(
                resolveDropNotes(name, nameNotes, rarityNotes, namedRefs),
                dropName = name,
            )

        return ParsedWikiDrop(
            name = name,
            quantity = quantity,
            rarity = rarity,
            section =
                if (rarity.equals("Always", ignoreCase = true) && section != WikiDropSection.Tertiary) {
                    WikiDropSection.Guaranteed
                } else {
                    section
                },
            subsection = subsection,
            wikiNotes = wikiNotes,
            isNoted = isNoted,
        )
    }

    /** Strips wiki `(noted)` quantity suffixes such as `1 (noted)` or `5 (noted)`. */
    fun parseWikiQuantity(rawQuantity: String): Pair<String, Boolean> {
        val isNoted = rawQuantity.contains("(noted)", ignoreCase = true)
        val quantity =
            rawQuantity
                .replace(Regex("""\s*\(noted\)""", RegexOption.IGNORE_CASE), "")
                .trim()
                .ifBlank { "1" }
        return quantity to isNoted
    }

    fun parseGroupDFootnotes(sectionBody: String): List<String> {
        val notes = mutableListOf<String>()

        val refPattern =
            Regex(
                """<ref[^>]*group\s*=\s*['"]?d['"]?[^>/]*>(.*?)</ref>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )
        for (match in refPattern.findAll(sectionBody)) {
            cleanWikiNotes(match.groupValues[1]).ifBlank { null }?.let(notes::add)
        }

        val tagRefPattern =
            Regex(
                """\{\{#tag:ref\|([^|}]+)\|[^}]*group\s*=\s*d""",
                RegexOption.IGNORE_CASE,
            )
        for (match in tagRefPattern.findAll(sectionBody)) {
            cleanWikiNotes(match.groupValues[1]).ifBlank { null }?.let(notes::add)
        }

        for (content in WikiTemplateParser.extractTemplates(sectionBody, "Refn")) {
            extractRefnNotesFromParams(content)?.let(notes::add)
        }

        return notes.distinct()
    }

    /** Resolves footnotes attached to a single drop line (inline text + named group-d refs). */
    fun resolveDropNotes(
        dropName: String,
        nameNotes: String,
        rarityNotes: String,
        namedRefs: Map<String, String>,
    ): List<String> {
        val inline = parseInlineNoteFields(nameNotes, rarityNotes)
        val named =
            resolveNamedGroupDRefLinks(nameNotes, rarityNotes, namedRefs)
                .filter { WikiDropNoteClassifier.relevantToDrop(dropName, it) }
        return (inline + named).distinct()
    }

    /**
     * Collects `<ref name="…" group="d">…</ref>` bodies from a subsection, including definitions
     * embedded in {{DropsLine}} `namenotes` / `raritynotes` fields.
     */
    fun collectSectionNamedGroupDFootnotes(sectionBody: String): Map<String, String> {
        val refs = linkedMapOf<String, String>()
        refs.putAll(parseNamedGroupDFootnotes(sectionBody))

        for (content in WikiTemplateParser.extractTemplates(sectionBody, "DropsLine")) {
            val params = WikiTemplateParser.parseParams(content)
            for (field in listOf(params["namenotes"].orEmpty(), params["raritynotes"].orEmpty())) {
                if (field.isBlank()) {
                    continue
                }
                for ((name, text) in parseNamedGroupDFootnotes(field)) {
                    refs.putIfAbsent(name, text)
                }
            }
        }

        return refs
    }

    /** Maps `<ref name="dual-drop-rune" group="d">…</ref>` footnote bodies in a subsection. */
    fun parseNamedGroupDFootnotes(sectionBody: String): Map<String, String> {
        val refs = linkedMapOf<String, String>()

        val refPattern =
            Regex(
                """<ref\s+([^>]*?)group\s*=\s*['"]?d['"]?([^>]*?)>(.*?)</ref>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )
        for (match in refPattern.findAll(sectionBody)) {
            val attrs = "${match.groupValues[1]} ${match.groupValues[2]}"
            val name = extractRefName(attrs) ?: continue
            cleanWikiNotes(match.groupValues[3]).ifBlank { null }?.let { refs[name] = it }
        }

        val reversedRefPattern =
            Regex(
                """<ref\s+([^>]*?)>(.*?)</ref>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )
        for (match in reversedRefPattern.findAll(sectionBody)) {
            val attrs = match.groupValues[1]
            if (!attrs.contains("group", ignoreCase = true) || !attrs.contains("d", ignoreCase = true)) {
                continue
            }
            val name = extractRefName(attrs) ?: continue
            if (name in refs) {
                continue
            }
            cleanWikiNotes(match.groupValues[2]).ifBlank { null }?.let { refs[name] = it }
        }

        return refs
    }

    private fun resolveNamedGroupDRefLinks(
        nameNotes: String,
        rarityNotes: String,
        namedRefs: Map<String, String>,
    ): List<String> {
        if (namedRefs.isEmpty()) {
            return emptyList()
        }
        val combined = "$nameNotes $rarityNotes"
        val selfClosing =
            Regex(
                """<ref\s+([^>]*?)/>""",
                RegexOption.IGNORE_CASE,
            )
        return selfClosing
            .findAll(combined)
            .mapNotNull { match ->
                val attrs = match.groupValues[1]
                if (!attrs.contains("group", ignoreCase = true) || !attrs.contains("d", ignoreCase = true)) {
                    return@mapNotNull null
                }
                val name = extractRefName(attrs) ?: return@mapNotNull null
                namedRefs[name]
            }.toList()
    }

    private fun extractRefName(attrs: String): String? {
        val match =
            Regex("""name\s*=\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE).find(attrs.trim())
                ?: return null
        return match.groupValues[1].trim().ifBlank { null }
    }

    /** Parses `raritynotes` / `namenotes` fields from {{DropsLine}} templates. */
    fun parseInlineNoteFields(vararg fields: String): List<String> {
        val notes = mutableListOf<String>()
        for (field in fields) {
            if (field.isBlank()) {
                continue
            }
            notes += extractRefnNotes(field)
            notes += extractInlineGroupDRefTags(field)
            val stripped = stripNoteTemplates(field)
            cleanWikiNotes(stripped).ifBlank { null }?.let(notes::add)
        }
        return notes.distinct()
    }

    private fun extractRefnNotes(raw: String): List<String> =
        WikiTemplateParser.extractTemplates(raw, "Refn").mapNotNull(::extractRefnNotesFromParams)

    private fun extractRefnNotesFromParams(refnContent: String): String? {
        val params = WikiTemplateParser.parseParams(refnContent)
        val group = params["group"]
        if (group != null && !group.equals("d", ignoreCase = true)) {
            return null
        }
        val text =
            params["_0"]
                ?: params["note"]
                ?: params["1"]
                ?: return null
        return cleanWikiNotes(text).ifBlank { null }
    }

    private fun extractInlineGroupDRefTags(raw: String): List<String> {
        val pattern =
            Regex(
                """<ref[^>]*group\s*=\s*['"]?d['"]?[^>]*>(.*?)</ref>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            )
        return pattern
            .findAll(raw)
            .mapNotNull { match -> cleanWikiNotes(match.groupValues[1]).ifBlank { null } }
            .toList()
    }

    private fun stripNoteTemplates(raw: String): String {
        var text = raw
        for (refn in WikiTemplateParser.extractTemplates(raw, "Refn")) {
            text = text.replace("{{Refn|$refn}}", " ")
        }
        text = text.replace(Regex("""\{\{Refn\|[^}]*}}""", RegexOption.IGNORE_CASE), " ")
        text =
            text.replace(
                Regex("""<ref[^>]*group\s*=\s*['"]?d['"]?[^>]*/?>""", RegexOption.IGNORE_CASE),
                " ",
            )
        return text
    }

    fun cleanWikiNotes(raw: String): String {
        if (raw.isBlank()) {
            return ""
        }

        var text = raw.trim()
        text = text.replace(Regex("""\{\{[^}]*}}"""), " ")
        text = text.replace(Regex("""<[^>]+>"""), " ")
        text =
            text.replace(Regex("""\[\[([^|\]]+)\|([^\]]+)]]""")) { match ->
                match.groupValues[2]
            }
        text = text.replace(Regex("""\[\[([^\]]+)]]""")) { match -> match.groupValues[1] }
        text = text.replace(Regex("""\[\[([^\]]+)$""")) { match -> match.groupValues[1] }
        text = text.replace("[[", "").replace("]]", "")
        text = text.replace(Regex("""\s+"""), " ").trim()
        return text
    }

    /** Resolves general seed table access from wiki templates (e.g. `{{GeneralSeedDropTableInfo|18/128}}`). */
    internal fun parseGeneralSeedAccess(body: String): ParsedSubtableAccess? {
        for (template in
            listOf(
                "GeneralSeedDropTableInfo",
                "GeneralSeedDropTableIntro",
                "GeneralSeedDropTable",
            )) {
            for (content in WikiTemplateParser.extractTemplates(body, template)) {
                parseSeedAccessChance(content)?.let { (numerator, denominator) ->
                    return ParsedSubtableAccess(
                        tableKey = SubtableKey.SEED,
                        numerator = numerator,
                        denominator = denominator,
                        subsection = "Seeds",
                    )
                }
            }
        }

        for (content in WikiTemplateParser.extractTemplates(body, "GeneralSeedDropLines")) {
            val params = WikiTemplateParser.parseParams(content)
            val chance =
                params["_0"]?.trim().orEmpty().ifBlank {
                    content.split('|').firstOrNull()?.trim().orEmpty()
                }
            parseAccessChance(chance)?.let { (numerator, denominator) ->
                return ParsedSubtableAccess(
                    tableKey = SubtableKey.SEED,
                    numerator = numerator,
                    denominator = denominator,
                    subsection = "Seeds",
                )
            }
            break
        }

        val seedIntroPattern =
            Regex(
                """(\d+)\s*/\s*(\d+)\s+chance of rolling the general seed drop table""",
                RegexOption.IGNORE_CASE,
            )
        for (match in seedIntroPattern.findAll(body)) {
            val numerator = match.groupValues[1].toIntOrNull() ?: continue
            val denominator = match.groupValues[2].toIntOrNull() ?: continue
            return ParsedSubtableAccess(
                tableKey = SubtableKey.SEED,
                numerator = numerator,
                denominator = denominator,
                subsection = "Seeds",
            )
        }

        return null
    }

    private fun parseSeedAccessChance(content: String): Pair<Int, Int>? {
        val params = WikiTemplateParser.parseParams(content)
        val positional = params["_0"]?.trim().orEmpty()
        if (positional.isNotBlank()) {
            parseAccessChance(positional)?.let { return it }
        }
        val firstSegment = content.split('|').firstOrNull()?.trim().orEmpty()
        return parseAccessChance(firstSegment) ?: parseAccessChance(content)
    }

    private fun parseAccessChance(raw: String): Pair<Int, Int>? {
        val fraction = Regex("""(\d+)\s*/\s*(\d+)""").find(raw.trim()) ?: return null
        val numerator = fraction.groupValues[1].toIntOrNull() ?: return null
        val denominator = fraction.groupValues[2].toIntOrNull() ?: return null
        return numerator to denominator
    }

    private fun extractSection(wikitext: String, heading: String): String? {
        val start = wikitext.indexOf(heading)
        if (start < 0) {
            return null
        }
        val after = wikitext.substring(start + heading.length)
        val nextHeading = Regex("""(?m)^==[^=].*==""").find(after)?.range?.first ?: after.length
        return after.substring(0, nextHeading)
    }

    private fun herbTableKey(variants: List<HerbRollVariant>?): SubtableKey =
        if (variants.isNullOrEmpty()) {
            SubtableKey.HERB
        } else {
            SubtableKey.HERB_MULTI
        }
}
