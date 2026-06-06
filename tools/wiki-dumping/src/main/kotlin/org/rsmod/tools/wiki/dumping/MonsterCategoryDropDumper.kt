package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiDropParser
import org.rsmod.tools.wiki.dumping.wiki.WikiInfoboxIds

private const val CATEGORY_BATCH_SIZE = 50

data class CategoryDropDumpResult(
    val canonicalPages: Int,
    val duplicatePages: Int,
    val skippedPages: Int,
    val skippedNoDropSection: Int,
    val skippedNoResolvedDrops: Int,
    val failedPages: Int,
    val writtenFiles: List<String>,
    val duplicates: List<DuplicateDropTable>,
    val skipped: List<SkippedMonsterPage>,
    val unknownDropRates: List<UnknownDropRateEntry>,
)

data class DuplicateDropTable(
    val wikiPage: String,
    val canonicalWikiPage: String,
    val tableVarName: String,
)

enum class MonsterSkipReason {
    NO_DROP_SECTION,
    NO_RESOLVED_DROPS,
    ALTERNATE_ENCOUNTER,
}

data class SkippedMonsterPage(
    val wikiPage: String,
    val reason: MonsterSkipReason,
)

class MonsterCategoryDropDumper(
    private val dumper: NpcDropTableWikiDumper,
    private val wiki: WikiClient,
    private val log: DropDumpLog,
    private val jsonExport: JsonExportConfig? = null,
    private val kotlinOutput: Boolean = true,
) {
    private class CanonicalEntry(
        val wikiPage: String,
        val specs: List<GeneratedDropTableSpec>,
        val results: List<DumpResult>,
    )

    suspend fun dumpAllMonsters(
        outputDir: Path,
        limit: Int? = null,
    ): CategoryDropDumpResult {
        val canonicalByCode = linkedMapOf<String, CanonicalEntry>()
        val duplicates = mutableListOf<DuplicateDropTable>()
        val skipped = mutableListOf<SkippedMonsterPage>()
        val unknownDropRates = mutableListOf<UnknownDropRateEntry>()
        val jsonRemainsOnly = mutableListOf<DropTableJsonExporter.CanonicalJsonExport>()
        var skippedNoDropSection = 0
        var skippedNoResolvedDrops = 0
        var skippedAlternateEncounter = 0
        var failedPages = 0
        var scannedPages = 0
        var withDropPages = 0
        var continueToken: String? = null

        val scanCap =
            when {
                limit != null -> "$limit monster page(s)"
                else -> "all ${wiki.infoboxMonsterPages} Infobox Monster page(s)"
            }
        log.info("scan limit: $scanCap")

        scanLoop@ while (true) {
            if (limit != null && scannedPages >= limit) {
                break
            }

            val batch = wiki.fetchMonsterPageBatch(continueToken = continueToken, batchSize = CATEGORY_BATCH_SIZE)
            continueToken = batch.continueToken

            if (batch.titles.isEmpty()) {
                break
            }

            for (title in batch.titles) {
                if (limit != null && scannedPages >= limit) {
                    break@scanLoop
                }

                scannedPages++

                val wikiPage = title.replace(' ', '_')
                log.verbose("[$scannedPages${limit?.let { "/$it" } ?: ""}] $wikiPage")

                if (isAlternateEncounterWikiPage(wikiPage)) {
                    skippedAlternateEncounter++
                    skipped += SkippedMonsterPage(wikiPage, MonsterSkipReason.ALTERNATE_ENCOUNTER)
                    log.verbose("  skip — alternate encounter (Echo) shares npc with canonical page")
                    continue
                }

                try {
                    val source = wiki.rawPageSource(wikiPage)
                    if (WikiInfoboxIds.hasNonNumericNpcId(source)) {
                        skippedNoDropSection++
                        skipped += SkippedMonsterPage(wikiPage, MonsterSkipReason.NO_DROP_SECTION)
                        log.verbose("  skip — non-numeric npc id")
                        continue
                    }

                    val tables = WikiDropParser.parseAllDropTables(source)
                    if (tables.isEmpty()) {
                        skippedNoDropSection++
                        skipped += SkippedMonsterPage(wikiPage, MonsterSkipReason.NO_DROP_SECTION)
                        log.verbose("  skip — no drop tables")
                        continue
                    }

                    val request =
                        NpcDropDumpRequest(
                            wikiPage = wikiPage,
                            tableVarName = toTableVarName(wikiPage),
                            tableIdentifier = tableIdentifier(wikiPage, "Drops"),
                            outputFileName = outputFileName(wikiPage),
                        )

                    val results = dumper.dumpAll(request)
                    unknownDropRates += results.flatMap { it.unknownDropRates }
                    val exportableResults = results.filter { it.hasJsonDropContent() }
                    val specs = exportableResults.map { it.spec }.filter { it.hasDropContent() }
                    if (exportableResults.isEmpty()) {
                        if (dumper.isIgnorableNonLootDropContent(tables)) {
                            log.verbose("  skip — no loot (remains/nothing/informational)")
                            continue
                        }
                        skippedNoResolvedDrops++
                        skipped += SkippedMonsterPage(wikiPage, MonsterSkipReason.NO_RESOLVED_DROPS)
                        log.verbose("  skip — no resolved drops")
                        continue
                    }

                    if (specs.isEmpty()) {
                        if (jsonExport != null) {
                            jsonRemainsOnly +=
                                DropTableJsonExporter.CanonicalJsonExport(
                                    wikiPage = wikiPage,
                                    results = exportableResults,
                                )
                            withDropPages++
                            log.verbose("  json-only — remains")
                        }
                        continue
                    }

                    withDropPages++

                    val primarySpec = specs.first()
                    val codeBody = DropTableCodeGenerator.tableBodyForDedup(primarySpec)
                    val existing = canonicalByCode[codeBody]
                    if (existing != null) {
                        duplicates +=
                            DuplicateDropTable(
                                wikiPage = wikiPage,
                                canonicalWikiPage = existing.wikiPage,
                                tableVarName = existing.specs.first().tableVarName,
                            )
                        log.verbose(
                            "  exact duplicate of ${existing.wikiPage} " +
                                "→ ${existing.specs.first().tableVarName}",
                        )
                        continue
                    }

                    if (results.size > 1) {
                        log.warn("$wikiPage has ${results.size} drop tables — dedup uses first table only")
                    }

                    canonicalByCode[codeBody] =
                        CanonicalEntry(
                            wikiPage = wikiPage,
                            specs = specs,
                            results = exportableResults.filter { it.spec.hasDropContent() },
                        )
                } catch (e: Exception) {
                    failedPages++
                    log.warn("$wikiPage failed: ${e.message ?: e.javaClass.simpleName}")
                }
            }

            if (continueToken == null) {
                break
            }
        }

        val skippedPages = skippedNoDropSection + skippedNoResolvedDrops + skippedAlternateEncounter
        log.info(
            "scanned $scannedPages Infobox Monster page(s)" +
                if (limit != null) " (--limit=$limit)" else " (all)",
        )
        if (skippedPages > 0) {
            log.info(
                "skipped $skippedPages — $skippedNoDropSection without ==Drops==, " +
                    "$skippedNoResolvedDrops with no resolved drops, " +
                    "$skippedAlternateEncounter alternate encounter (Echo)",
            )
        }

        outputDir.createDirectories()
        DropTableOutputLayout.cleanupStaleAlternateEncounterFiles(outputDir, log)
        val writtenPaths = mutableSetOf<Path>()
        val jsonCanonical = mutableListOf<DropTableJsonExporter.CanonicalJsonExport>()

        for ((_, entry) in canonicalByCode) {
            val specs = entry.specs.filter { it.hasDropContent() }
            if (specs.isEmpty()) {
                continue
            }

            if (kotlinOutput) {
                val output = DropTableOutputLayout.resolveMonsterOutputFile(outputDir, entry.wikiPage)
                output.parent.createDirectories()
                val code =
                    DropTableCodeGenerator.generateFile(
                        specs,
                        packageName = DropTableOutputLayout.MONSTERS_PACKAGE,
                    )
                output.writeText(code)
                writtenPaths.add(output)

                val relative = outputDir.relativize(output).toString().replace('\\', '/')
                log.info("wrote $relative ← ${entry.wikiPage}")
            }

            if (jsonExport != null) {
                jsonCanonical +=
                    DropTableJsonExporter.CanonicalJsonExport(
                        wikiPage = entry.wikiPage,
                        results = entry.results,
                    )
            }
        }

        jsonExport?.let { config ->
            exportJsonTables(config, jsonCanonical + jsonRemainsOnly, duplicates)
        }

        if (kotlinOutput) {
            DropTableOutputLayout.cleanupStaleMonsterFiles(outputDir, writtenPaths)
            DropTableOutputLayout.cleanupGroupedSubdirs(outputDir)
            DropTableOutputLayout.cleanupLegacyFlatMonsterFiles(outputDir)

            val manifestDir = outputDir.resolve(DropTableOutputLayout.MONSTERS_DIR)
            manifestDir.createDirectories()
            writeDuplicateManifest(manifestDir, duplicates)
            writeSkippedManifest(manifestDir, skipped)
            writeUnknownDropRatesManifest(manifestDir, unknownDropRates)
        }

        return CategoryDropDumpResult(
            canonicalPages = canonicalByCode.size,
            duplicatePages = duplicates.size,
            skippedPages = skippedPages,
            skippedNoDropSection = skippedNoDropSection,
            skippedNoResolvedDrops = skippedNoResolvedDrops,
            failedPages = failedPages,
            writtenFiles = writtenPaths.map { outputDir.relativize(it).toString().replace('\\', '/') },
            duplicates = duplicates,
            skipped = skipped,
            unknownDropRates = unknownDropRates.distinctBy { Triple(it.wikiPage, it.itemName, it.rarity) },
        )
    }

    private fun exportJsonTables(
        config: JsonExportConfig,
        canonical: List<DropTableJsonExporter.CanonicalJsonExport>,
        duplicates: List<DuplicateDropTable>,
    ) {
        config.jsonRoot.createDirectories()

        for (entry in canonical) {
            for (result in entry.results) {
                val table =
                    DropTableJsonExporter.exportTable(
                        result = result,
                        wikiPage = entry.wikiPage,
                    )
                DropTableJsonExporter.writeTable(config.jsonRoot, result.spec.tableVarName, table)
                val relative =
                    config.jsonRoot
                        .relativize(
                            DropTableJsonOutputLayout.resolveTableFile(config.jsonRoot, result.spec.tableVarName),
                        ).toString()
                        .replace('\\', '/')
                log.info("wrote json $relative ← ${entry.wikiPage}")
            }
        }

        val manifest =
            DropTableJsonExporter.buildManifest(
                jsonRoot = config.jsonRoot,
                canonical = canonical,
                duplicates = duplicates,
            )
        DropTableJsonExporter.writeManifest(config.jsonRoot, manifest)
        log.info(
            "wrote json manifest (${manifest.size} npc entries) → ${DropTableJsonOutputLayout.MANIFEST_FILE}",
        )
    }

    private fun writeDuplicateManifest(outputDir: Path, duplicates: List<DuplicateDropTable>) {
        if (duplicates.isEmpty()) {
            return
        }

        val manifest = outputDir.resolve("_duplicate_drop_tables.txt")
        val lines =
            buildList {
                add("# Exact duplicate drop tables — identical generated code including npc ids")
                add("# wikiPage -> canonicalWikiPage (tableVarName)")
                add("")
                for (dup in duplicates.sortedBy { it.wikiPage }) {
                    add("${dup.wikiPage} -> ${dup.canonicalWikiPage} (${dup.tableVarName})")
                }
            }
        manifest.writeText(lines.joinToString("\n") + "\n")
        log.info("wrote duplicate manifest (${duplicates.size} exact duplicates) → ${manifest.fileName}")
    }

    private fun writeSkippedManifest(outputDir: Path, skipped: List<SkippedMonsterPage>) {
        if (skipped.isEmpty()) {
            return
        }

        val manifest = outputDir.resolve("_skipped_monsters.txt")
        val lines =
            buildList {
                add("# Skipped Infobox Monster pages")
                add("# no_drop_section — no ==Drops== wikitable, or npc id is non-numeric (removed/hist/unreleased)")
                add("# no_resolved_drops — drops section present but nothing mapped to objs")
                add("# (remains-only pages with bones/ashes/param_46 are not listed)")
                add("")
                for (entry in skipped.sortedBy { it.wikiPage }) {
                    val reason =
                        when (entry.reason) {
                            MonsterSkipReason.NO_DROP_SECTION -> "no_drop_section"
                            MonsterSkipReason.NO_RESOLVED_DROPS -> "no_resolved_drops"
                            MonsterSkipReason.ALTERNATE_ENCOUNTER -> "alternate_encounter"
                        }
                    add("${entry.wikiPage}\t$reason")
                }
            }
        manifest.writeText(lines.joinToString("\n") + "\n")
        log.info("wrote skip manifest (${skipped.size} entries) → ${manifest.fileName}")
    }

    private fun writeUnknownDropRatesManifest(outputDir: Path, unknownDropRates: List<UnknownDropRateEntry>) {
        if (unknownDropRates.isEmpty()) {
            return
        }

        val distinct =
            unknownDropRates
                .distinctBy { Triple(it.wikiPage, it.itemName, it.rarity) }
                .sortedWith(compareBy({ it.wikiPage }, { it.itemName }, { it.rarity }))

        val manifest = outputDir.resolve("_unknown_drop_rates.txt")
        val lines =
            buildList {
                add("# Wiki drops with text rarity — exact rate needs data collection")
                add("# Any non-numeric rarity (Common, Rare, Random, Varies, etc.)")
                add("# wikiPage\titem\tsection\tsubsection\trarity")
                add("")
                for (entry in distinct) {
                    add(entry.manifestLine())
                }
            }
        manifest.writeText(lines.joinToString("\n") + "\n")
        log.info("wrote unknown drop rates manifest (${distinct.size} entries) → ${manifest.fileName}")
    }
}
