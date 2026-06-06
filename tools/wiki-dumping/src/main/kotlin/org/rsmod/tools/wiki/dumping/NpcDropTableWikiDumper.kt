package org.rsmod.tools.wiki.dumping

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.ParsedNpcDropTable
import org.rsmod.tools.wiki.dumping.wiki.ParsedWikiDrop
import org.rsmod.tools.wiki.dumping.wiki.SubtableKey
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiDropParser
import org.rsmod.tools.wiki.dumping.wiki.WikiInfoboxIds
import org.rsmod.tools.wiki.dumping.wiki.WikiDropSection
import org.rsmod.tools.wiki.dumping.wiki.WikiCompanionDropParser

private const val DEFAULT_OUTPUT_DIR = "content/drops/src/main/kotlin/org/rsmod/content/drops/tables"

data class NpcDropDumpRequest(
    val wikiPage: String,
    val tableVarName: String,
    val tableIdentifier: String,
    val outputFileName: String,
    val relatedNpcPages: List<String> = emptyList(),
)

class NpcDropTableWikiDumper(
    private val wiki: WikiClient,
    private val resources: DropDumpResources,
    private val itemLookup: ItemWikiLookup,
    private val log: DropDumpLog,
) {
    private val npcLookup get() = resources.npcLookup
    private val objLookup get() = resources.objLookup
    private val remainsLookup get() = resources.remainsLookup

    suspend fun dumpAll(request: NpcDropDumpRequest): List<DumpResult> {
        val pageTitles = buildList {
            add(request.wikiPage)
            addAll(request.relatedNpcPages)
        }

        val sources = linkedMapOf<String, String>()
        for (title in pageTitles) {
            sources[title] = wiki.rawPageSource(title)
        }

        val primarySource = sources.getValue(request.wikiPage)
        if (WikiInfoboxIds.hasNonNumericNpcId(primarySource)) {
            log.verbose("${request.wikiPage} skip — non-numeric npc id")
            return emptyList()
        }

        val tables = WikiDropParser.parseAllDropTables(primarySource)
        if (tables.isEmpty()) {
            throw IllegalStateException("No drop tables found on wiki page '${request.wikiPage}'.")
        }

        val relatedNpcIds =
            sources
                .filterKeys { it != request.wikiPage }
                .values
                .flatMap(WikiDropParser::parseNpcIds)
                .distinct()

        prewarmItemLookups(tables)

        return tables.map { table ->
            val npcIds = (table.npcIds + relatedNpcIds).distinct().sorted()
            dumpTable(
                baseRequest = request,
                table = table,
                npcIds = npcIds,
            )
        }
    }

    suspend fun dumpToFile(
        request: NpcDropDumpRequest,
        outputDir: Path,
        jsonExport: JsonExportConfig? = null,
        kotlinOutput: Boolean = true,
    ): List<DumpResult> {
        lateinit var results: List<DumpResult>
        val elapsed =
            measureTimeMillis {
                results = dumpAll(request)
                val exportable = results.filter { it.hasJsonDropContent() }
                val specs = exportable.map { it.spec }.filter { it.hasDropContent() }
                if (exportable.isEmpty()) {
                    log.verbose("${request.wikiPage} skip — no resolved drops")
                    return@measureTimeMillis
                }

                outputDir.createDirectories()

                if (kotlinOutput && specs.isNotEmpty()) {
                    val output = DropTableOutputLayout.resolveMonsterOutputFile(outputDir, request.wikiPage)
                    output.parent.createDirectories()
                    val code =
                        DropTableCodeGenerator.generateFile(
                            specs,
                            packageName = DropTableOutputLayout.MONSTERS_PACKAGE,
                        )
                    output.writeText(code)

                    if (results.size > 1) {
                        cleanupStaleSplitFiles(request.wikiPage, output.parent)
                    }

                    log.info("wrote ${output.fileName} → ${output.toAbsolutePath()}")
                }

                jsonExport?.let { config ->
                    for (result in exportable) {
                        val table =
                            DropTableJsonExporter.exportTable(
                                result = result,
                                wikiPage = request.wikiPage,
                            )
                        DropTableJsonExporter.writeTable(config.jsonRoot, result.spec.tableVarName, table)
                        val jsonPath =
                            DropTableJsonOutputLayout.resolveTableFile(config.jsonRoot, result.spec.tableVarName)
                        log.info("wrote json ${config.jsonRoot.relativize(jsonPath)} → ${jsonPath.toAbsolutePath()}")
                    }

                    val manifest =
                        DropTableJsonExporter.buildManifest(
                            jsonRoot = config.jsonRoot,
                            canonical =
                                listOf(
                                    DropTableJsonExporter.CanonicalJsonExport(
                                        wikiPage = request.wikiPage,
                                        results = exportable,
                                    ),
                                ),
                            duplicates = emptyList(),
                        )
                    DropTableJsonExporter.writeManifest(config.jsonRoot, manifest)
                }
            }
        log.verbose("${request.wikiPage} processed in ${elapsed}ms")
        return results.filter { it.hasJsonDropContent() }
    }

    /** Pages with no real loot to generate — remains-only, Nothing-only, or informational ==Drops==. */
    fun isIgnorableNonLootDropContent(tables: List<ParsedNpcDropTable>): Boolean {
        if (tables.isEmpty()) {
            return false
        }
        return tables.all { table -> isIgnorableNonLootTable(table) }
    }

    private fun isIgnorableNonLootTable(table: ParsedNpcDropTable): Boolean {
        if (table.subtableAccesses.isNotEmpty()) {
            return false
        }

        if (table.drops.isEmpty()) {
            return true
        }

        if (table.drops.all { it.isNothing }) {
            return true
        }

        val substantive = table.drops.filter { !it.isNothing }
        if (substantive.isEmpty()) {
            return true
        }

        if (substantive.any { it.section != WikiDropSection.Guaranteed }) {
            return false
        }

        return substantive.all { drop ->
            remainsLookup.isIgnorableRemainsWikiDrop(drop.name, table.npcIds)
        }
    }

    private suspend fun prewarmItemLookups(tables: List<ParsedNpcDropTable>) {
        val hasSeedAccessByTable =
            tables.associate { table ->
                table.tableName to
                    table.subtableAccesses.any {
                        it.tableKey == SubtableKey.SEED || it.tableKey == SubtableKey.RARE_SEED
                    }
            }

        val names =
            tables
                .flatMap { table ->
                    val skipSeeds = hasSeedAccessByTable[table.tableName] == true
                    table.drops
                        .asSequence()
                        .filter { !it.isNothing }
                        .filter { !(skipSeeds && it.subsection.equals("Seeds", ignoreCase = true)) }
                        .map { it.name to it.isNoted }
                }.distinct()
                .filter { (name, noted) ->
                    !objLookup.canResolveLocally(name, noted = noted) &&
                        !objLookup.canResolveWithCachedWikiId(itemLookup, name, noted = noted)
                }
                .map { (name, _) -> name }

        if (names.isNotEmpty()) {
            itemLookup.prewarm(names)
        }
    }

    private fun cleanupStaleSplitFiles(wikiPage: String, outputDir: Path) {
        val base = wikiPage.replace(Regex("""[^A-Za-z0-9]"""), "")
        for (suffix in 1..9) {
            val stale = outputDir.resolve("${base}DropTable$suffix.kt")
            if (Files.deleteIfExists(stale)) {
                log.verbose("removed stale ${stale.fileName}")
            }
        }
    }

    private suspend fun dumpTable(
        baseRequest: NpcDropDumpRequest,
        table: ParsedNpcDropTable,
        npcIds: List<Int>,
    ): DumpResult {
        val (npcKeys, unmappedNpcIds) = npcLookup.toRscmList(npcIds)
        val subtableAccesses =
            table.subtableAccesses.map { access ->
                ResolvedSubtableAccess(
                    tableRef = access.tableKey.codegenRef,
                    numerator = access.numerator,
                    denominator = access.denominator,
                    subsection = access.subsection,
                    wikiLabel = access.tableKey.wikiLabel,
                    needsHardcodedSharedTable = access.fromProse || access.tableKey.needsHardcodedSharedTable,
                    herbRollVariants = access.herbRollVariants,
                )
            }

        val guaranteed = mutableListOf<ResolvedDropEntry>()
        val guaranteedIncludingRemains = mutableListOf<ResolvedDropEntry>()
        val main = mutableListOf<ResolvedDropEntry>()
        val tertiary = mutableListOf<ResolvedDropEntry>()
        val unmappedItems = mutableListOf<String>()
        val unknownDropRates = mutableListOf<UnknownDropRateEntry>()

        val hasSeedAccess =
            table.subtableAccesses.any {
                it.tableKey == SubtableKey.SEED || it.tableKey == SubtableKey.RARE_SEED
            }

        val droppedTogetherCompanionObjs =
            droppedTogetherCompanionObjs(table.drops, itemLookup, objLookup)

        for (drop in table.drops) {
            processDrop(
                drop = drop,
                hasSeedAccess = hasSeedAccess,
                wikiPage = baseRequest.wikiPage,
                table = table,
                npcIds = npcIds,
                droppedTogetherCompanionObjs = droppedTogetherCompanionObjs,
                guaranteed = guaranteed,
                guaranteedIncludingRemains = guaranteedIncludingRemains,
                main = main,
                tertiary = tertiary,
                unmappedItems = unmappedItems,
                unknownDropRates = unknownDropRates,
            )
        }

        val suffix = tableNameSuffix(table.tableName) ?: dropVariantSuffix(table.dropVariant)
        val tableVarName = toTableVarName(baseRequest.wikiPage, suffix)
        val tableIdentifier = tableIdentifier(baseRequest.wikiPage, table.tableName, table.dropVariant)
        val areaRscmKeys = DropVariantAreas.areasForVariant(table.dropVariant)
        val tableRequest =
            baseRequest.copy(
                tableVarName = tableVarName,
                tableIdentifier = tableIdentifier,
                outputFileName = outputFileName(baseRequest.wikiPage),
            )

        val spec =
            GeneratedDropTableSpec(
                tableVarName = tableVarName,
                tableIdentifier = tableIdentifier,
                npcRscmKeys = npcKeys,
                areaRscmKeys = areaRscmKeys,
                guaranteed = guaranteed,
                main = main,
                subtableAccesses = subtableAccesses,
                tertiary = tertiary,
                unmappedItems = unmappedItems.distinct(),
                unmappedLabel = table.tableName,
                unknownDropRates = unknownDropRates.distinctUnknownRates(),
            ).let { raw ->
                val preRollDrops = raw.main.filter { GeneratedDropTableSpec.isPreRollSubsection(it.subsection) }
                val standardMain = raw.main.filterNot { GeneratedDropTableSpec.isPreRollSubsection(it.subsection) }

                val (preRollEntries, _, preRollSeparate) =
                    GeneratedDropTableSpec.finalizeMainRolls(preRollDrops, emptyList())
                val (mainEntries, mainMaxRoll, separateRolls) =
                    GeneratedDropTableSpec.finalizeMainRolls(standardMain, raw.subtableAccesses)
                val reconciledMaxRoll =
                    GeneratedDropTableSpec.reconcileMainMaxRoll(
                        main = mainEntries,
                        mainMaxRoll = mainMaxRoll,
                        subtableAccesses = raw.subtableAccesses,
                    )

                raw.copy(
                    main = mainEntries,
                    mainMaxRoll = reconciledMaxRoll,
                    subtableAccesses = raw.subtableAccesses,
                    separateRolls = separateRolls,
                    preRoll = preRollEntries,
                    preRollSeparateRolls = preRollSeparate,
                )
            }

        return DumpResult(
            request = tableRequest,
            spec = spec,
            npcIds = npcIds,
            npcRscmKeys = npcKeys,
            unmappedNpcIds = unmappedNpcIds,
            unmappedItems = unmappedItems.distinct(),
            guaranteedCount = guaranteed.size,
            mainCount = main.size,
            tertiaryCount = tertiary.size,
            unknownDropRates = unknownDropRates.distinctUnknownRates(),
            guaranteedIncludingRemains = guaranteedIncludingRemains,
        )
    }

    private suspend fun droppedTogetherCompanionObjs(
        drops: List<ParsedWikiDrop>,
        itemLookup: ItemWikiLookup,
        objLookup: ObjRscmLookup,
    ): Set<String> {
        val objs = mutableSetOf<String>()
        for (drop in drops) {
            for (spec in drop.wikiNotes.companionDrops) {
                if (!spec.droppedTogether) {
                    continue
                }
                for (name in spec.wikiNames) {
                    WikiCompanionDropParser
                        .resolveObj(itemLookup, objLookup, name, drop.name, droppedTogether = true)
                        ?.let { objs.add(it) }
                }
            }
        }
        return objs
    }

    private suspend fun processDrop(
        drop: ParsedWikiDrop,
        hasSeedAccess: Boolean,
        wikiPage: String,
        table: ParsedNpcDropTable,
        npcIds: List<Int>,
        droppedTogetherCompanionObjs: Set<String>,
        guaranteed: MutableList<ResolvedDropEntry>,
        guaranteedIncludingRemains: MutableList<ResolvedDropEntry>,
        main: MutableList<ResolvedDropEntry>,
        tertiary: MutableList<ResolvedDropEntry>,
        unmappedItems: MutableList<String>,
        unknownDropRates: MutableList<UnknownDropRateEntry>,
    ) {
        if (hasSeedAccess && drop.subsection.equals("Seeds", ignoreCase = true)) {
            return
        }
        if (drop.isNothing) {
            val parsed = DropTableCodeGenerator.parseMainRarity(drop.rarity)
            if (parsed == null) {
                if (DropTableCodeGenerator.isUnknownWikiDropRate(drop.rarity)) {
                    unknownDropRates += unknownRateEntry(wikiPage, table, drop)
                } else {
                    unmappedItems += drop.name
                }
                return
            }
            val (weight, rollDenominator) = parsed
            if (drop.section == WikiDropSection.Main) {
                main +=
                    ResolvedDropEntry(
                        obj = "",
                        quantity = "1",
                        weight = weight,
                        rollDenominator = rollDenominator,
                        wikiName = drop.name,
                        subsection = drop.subsection,
                        isNothing = true,
                    )
            }
            return
        }

        if (DropTableCodeGenerator.isUnknownWikiDropRate(drop.rarity)) {
            unknownDropRates += unknownRateEntry(wikiPage, table, drop)
            return
        }

        if (drop.wikiNotes.isCompanionOnly) {
            val previewObj = objLookup.resolveWikiItem(itemLookup, drop.name, noted = drop.isNoted)
            if (previewObj != null && previewObj in droppedTogetherCompanionObjs) {
                return
            }
        }

        val resolved = DropTableCodeGenerator.resolveItemDrop(drop, itemLookup, objLookup) ?: run {
            unmappedItems += drop.name
            return
        }

        if (resolved.obj in droppedTogetherCompanionObjs && !drop.wikiNotes.hasCompanionDrops) {
            return
        }

        when (drop.section) {
            WikiDropSection.Guaranteed -> {
                guaranteedIncludingRemains += resolved
                if (!remainsLookup.isEngineRemainsDrop(resolved.obj, resolved.wikiName, npcIds)) {
                    guaranteed += resolved
                }
            }
            WikiDropSection.Main -> main += resolved
            WikiDropSection.Tertiary -> tertiary += resolved
        }
    }
}

private fun unknownRateEntry(
    wikiPage: String,
    table: ParsedNpcDropTable,
    drop: ParsedWikiDrop,
): UnknownDropRateEntry =
    UnknownDropRateEntry(
        wikiPage = wikiPage,
        tableName = table.tableName,
        itemName = drop.name,
        section = drop.section.name,
        subsection = drop.subsection,
        rarity = drop.rarity,
    )

private fun List<UnknownDropRateEntry>.distinctUnknownRates(): List<UnknownDropRateEntry> =
    distinctBy { Triple(it.itemName, it.rarity, it.section) }

data class DumpResult(
    val request: NpcDropDumpRequest,
    val spec: GeneratedDropTableSpec,
    val npcIds: List<Int>,
    val npcRscmKeys: List<String>,
    val unmappedNpcIds: List<Int>,
    val unmappedItems: List<String>,
    val guaranteedCount: Int,
    val mainCount: Int,
    val tertiaryCount: Int,
    val unknownDropRates: List<UnknownDropRateEntry> = emptyList(),
    val guaranteedIncludingRemains: List<ResolvedDropEntry> = spec.guaranteed,
) {
    fun hasJsonDropContent(): Boolean = spec.hasDropContent() || guaranteedIncludingRemains.isNotEmpty()
}

private fun findRepoRoot(): Path? = GameValLoader.resolveRootOrNull()

private fun defaultOutputDir(root: Path?): Path =
    if (root != null) {
        root.resolve(DEFAULT_OUTPUT_DIR)
    } else {
        Path(DEFAULT_OUTPUT_DIR)
    }

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val positional = args.filterNot { it.startsWith("-") }

    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val verbose = flags.contains("--verbose") || flags.contains("-v")
    val fetchDumpNpc = flags.contains("--fetch-dump")
    val exportJson = flags.contains("--json") || flags.contains("--json-only")
    val jsonOnly = flags.contains("--json-only")
    val wikiDumpDir = flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val bulkDump =
        flags.contains("--all-monsters") ||
            positional.isEmpty() ||
            positional.singleOrNull()?.startsWith("Category:", ignoreCase = true) == true
    val limitFlag = parseScanLimit(args)
    val log = DropDumpLog(quiet = quiet, verbose = verbose)

    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter("--root=")
            ?: System.getProperty("RSPS_ROOT")
            ?: findRepoRoot()?.toString()

    val repoRoot = rootDir?.let { Path(it) } ?: findRepoRoot()
    val outFromFlag = flags.firstOrNull { it.startsWith("--out=") }?.substringAfter("--out=")
    val jsonOutFromFlag = flags.firstOrNull { it.startsWith("--json-out=") }?.substringAfter("--json-out=")

    val outputDir =
        when {
            positional.size == 2 && Files.isDirectory(Path.of(positional[1])) -> Path.of(positional[1])
            else -> outFromFlag?.let { Path(it) } ?: defaultOutputDir(repoRoot)
        }
    val jsonOutputDir =
        when {
            !exportJson -> null
            jsonOutFromFlag != null -> Path(jsonOutFromFlag)
            else -> defaultJsonOutputDir(repoRoot)
        }
    val wikiPages =
        when {
            bulkDump -> emptyList()
            positional.size == 2 && Files.isDirectory(Path.of(positional[1])) -> listOf(positional[0])
            else -> positional
        }

    runBlocking {
        var summaryPages = wikiPages.size
        val totalElapsed =
            measureTimeMillis {
                val resources = DropDumpResources.load(rootDir, log, fetchDumpNpc)

                val dumpDir = WikiClient.resolveDumpDirectory(wikiDumpDir)
                log.info("wiki dump: ${dumpDir.absolutePath}")

                WikiClient.open(wikiDumpDir = wikiDumpDir, onPageFetch = { log.onWikiFetch(it) }).use { wiki ->
                    log.info(
                        "loaded ${wiki.loadedPages} wiki page(s) " +
                            "(${wiki.infoboxMonsterPages} Infobox Monster)",
                    )
                    val itemLookup = ItemWikiLookup(wiki, log)
                    val dumper = NpcDropTableWikiDumper(wiki, resources, itemLookup, log)
                    val jsonExport =
                        jsonOutputDir?.let { dir ->
                            JsonExportConfig(
                                jsonRoot = dir,
                                dumpIndex = resources.dumpIndex,
                                npcLookup = resources.npcLookup,
                            )
                        }

                    if (bulkDump) {
                        val categoryDumper =
                            MonsterCategoryDropDumper(
                                dumper = dumper,
                                wiki = wiki,
                                log = log,
                                jsonExport = jsonExport,
                                kotlinOutput = !jsonOnly,
                            )
                        log.info("Infobox Monster scan → ${outputDir.toAbsolutePath()}")
                        if (jsonExport != null) {
                            log.info("json export → ${jsonExport.jsonRoot.toAbsolutePath()}")
                        }
                        val result =
                            categoryDumper.dumpAllMonsters(
                                outputDir = outputDir,
                                limit = limitFlag,
                            )
                        summaryPages =
                            result.canonicalPages + result.duplicatePages + result.skippedPages
                        log.info(
                            "category summary — " +
                                "${result.canonicalPages} unique, " +
                                "${result.duplicatePages} exact duplicates, " +
                                "${result.skippedPages} skipped " +
                                "(${result.skippedNoDropSection} no ==Drops==, " +
                                "${result.skippedNoResolvedDrops} unresolved), " +
                                "${result.failedPages} failed",
                        )
                        if (result.unknownDropRates.isNotEmpty()) {
                            log.info(
                                "  ${result.unknownDropRates.size} drop(s) with unknown text rarity " +
                                    "→ _unknown_drop_rates.txt",
                            )
                        }
                        logSkippedExamples(log, result.skipped)
                    } else {
                        var hadUnmappedNpcIds = false

                        log.info("dumping ${wikiPages.size} page(s) → ${outputDir.toAbsolutePath()}")

                        for (wikiPage in wikiPages) {
                            val request =
                                NpcDropDumpRequest(
                                    wikiPage = wikiPage,
                                    tableVarName = toTableVarName(wikiPage),
                                    tableIdentifier = tableIdentifier(wikiPage, "Drops"),
                                    outputFileName = outputFileName(wikiPage),
                                    relatedNpcPages = defaultRelatedNpcPages(wikiPage),
                                )

                            log.info("— $wikiPage")
                            val results = dumper.dumpToFile(request, outputDir, jsonExport, kotlinOutput = !jsonOnly)

                            for (result in results) {
                                log.info(
                                    "${result.request.tableVarName}: " +
                                        "${result.guaranteedCount} guaranteed, " +
                                        "${result.mainCount} main, " +
                                        "${result.tertiaryCount} tertiary, " +
                                        "${result.npcRscmKeys.size} npcs " +
                                        "(wiki ids: ${result.npcIds.joinToString()})",
                                )

                                if (result.unmappedNpcIds.isNotEmpty()) {
                                    hadUnmappedNpcIds = true
                                    log.warn(
                                        "${result.request.tableVarName} unmapped npc ids: " +
                                            result.unmappedNpcIds.joinToString(),
                                    )
                                }
                                if (result.unmappedItems.isNotEmpty()) {
                                    log.warn(
                                        "${result.request.tableVarName} unmapped items: " +
                                            result.unmappedItems.joinToString(),
                                    )
                                }
                            }
                        }

                        log.verbose("wiki pages cached: ${wiki.cachedPages}")

                        if (hadUnmappedNpcIds) {
                            exitProcess(1)
                        }
                    }
                }
            }

        log.summary(summaryPages, totalElapsed)
    }
}

internal fun wikiPageBase(wikiPage: String): String =
    wikiPage
        .split('_')
        .filter { it.isNotBlank() }
        .mapIndexed { index, part ->
            val cleaned = part.replace(Regex("""[^A-Za-z0-9]"""), "")
            if (index == 0) {
                cleaned.replaceFirstChar { it.lowercase() }
            } else {
                cleaned.replaceFirstChar { it.uppercase() }
            }
        }.joinToString("")

internal fun wikiPageTypeName(wikiPage: String): String =
    wikiPage
        .split('_')
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.replace(Regex("""[^A-Za-z0-9]"""), "").replaceFirstChar { it.uppercase() }
        }

internal fun tableNameSuffix(tableName: String): String? =
    when {
        tableName.equals("Drops", ignoreCase = true) -> null
        else -> Regex("""(\d+)""").find(tableName)?.groupValues?.get(1)
    }

/** Suffix for multi-variant `==Drops==` sections (e.g. Wilderness Slayer Cave). */
internal fun dropVariantSuffix(dropVariant: String): String? {
    if (dropVariant.isBlank()) {
        return null
    }

    val normalized = dropVariant.lowercase()
    return when {
        normalized.contains("wilderness") -> "Wilderness"
        normalized.contains("catacombs") && !normalized.contains("standard") -> "Catacombs"
        else -> {
            dropVariant
                .split(Regex("""[\s,]+"""))
                .filter { it.isNotBlank() && !it.equals("and", ignoreCase = true) }
                .joinToString("") { part ->
                    part.replace(Regex("""[^A-Za-z0-9]"""), "").replaceFirstChar { it.uppercase() }
                }
                .takeIf { it.isNotBlank() }
        }
    }
}

internal fun toTableVarName(wikiPage: String, suffix: String? = null): String {
    val base = wikiPageBase(wikiPage)
    return if (suffix == null) "${base}DropTable" else "${base}DropTable$suffix"
}

internal fun outputFileName(wikiPage: String): String = "${wikiPageTypeName(wikiPage)}DropTable.kt"

internal fun displayWikiPageName(wikiPage: String): String =
    wikiPage.split('_').filter { it.isNotBlank() }.joinToString(" ")

internal fun tableIdentifier(wikiPage: String, tableName: String, dropVariant: String = ""): String {
    val displayName = displayWikiPageName(wikiPage)
    return when {
        dropVariant.isNotBlank() -> "$displayName $dropVariant"
        tableName.equals("Drops", ignoreCase = true) -> "$displayName Drops"
        else -> "$displayName $tableName"
    }
}

private fun defaultRelatedNpcPages(wikiPage: String): List<String> =
    when (wikiPage.equals("Man", ignoreCase = true)) {
        true -> listOf("Woman")
        else -> emptyList()
    }

/** Returns null to scan all monster pages; supports `--limit=50`, `--limit 50`, or `--limit=all`. */
internal fun parseScanLimit(args: Array<String>): Int? {
    args.firstOrNull { it.startsWith("--limit=") }?.substringAfter("--limit=")?.let { raw ->
        return parseScanLimitValue(raw)
    }

    val idx = args.indexOf("--limit")
    if (idx >= 0 && idx + 1 < args.size) {
        return parseScanLimitValue(args[idx + 1])
    }

    return null
}

private fun parseScanLimitValue(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty() || trimmed.equals("all", ignoreCase = true)) {
        return null
    }
    return trimmed.toIntOrNull()?.takeIf { it > 0 }
}

internal fun isAlternateEncounterWikiPage(wikiPage: String): Boolean {
    val normalized = wikiPage.replace('_', ' ').lowercase()
    return normalized.contains("(echo)")
}

private fun logSkippedExamples(log: DropDumpLog, skipped: List<SkippedMonsterPage>, sampleSize: Int = 10) {
    logSkippedExample(log, skipped, MonsterSkipReason.NO_DROP_SECTION, "no ==Drops==", sampleSize)
    logSkippedExample(log, skipped, MonsterSkipReason.NO_RESOLVED_DROPS, "unresolved drops", sampleSize)
    logSkippedExample(log, skipped, MonsterSkipReason.ALTERNATE_ENCOUNTER, "alternate encounter (Echo)", sampleSize)
}

private fun logSkippedExample(
    log: DropDumpLog,
    skipped: List<SkippedMonsterPage>,
    reason: MonsterSkipReason,
    label: String,
    sampleSize: Int,
) {
    val matching = skipped.filter { it.reason == reason }
    if (matching.isEmpty()) {
        return
    }
    val sample = matching.take(sampleSize).joinToString(", ") { it.wikiPage }
    val more = matching.size - sampleSize
    val suffix = if (more > 0) " (+$more more)" else ""
    log.info("  skipped $label e.g. $sample$suffix")
}
