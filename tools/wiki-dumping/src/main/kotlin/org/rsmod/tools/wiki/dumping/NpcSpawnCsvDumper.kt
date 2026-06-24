package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiDumpStorePages
import org.rsmod.tools.wiki.dumping.wiki.WikiItemSpawnParser
import org.rsmod.tools.wiki.dumping.wiki.WikiNpcSpawnParser

private const val DEFAULT_CSV_RESOURCE = "2021-spawns.csv"
private const val DEFAULT_OUTPUT_DIR_RELATIVE = ".data/raw-cache/map/npcs"
private const val DEFAULT_RADIUS_FILE_RELATIVE = ".data/raw-cache/map/npcs/npc_radius.txt"
private const val WIKI_CSV_AREA_HALF_SIZE = 5

data class CsvNpcCoord(
    val x: Int,
    val z: Int,
    val plane: Int,
)

enum class NpcSpawnSource {
    CSV,
    WIKI_LOC_LINE,
    WIKI_INFOBOX_MAP,
}

data class ResolvedNpcSpawn(
    val npcKey: String,
    val coords: String,
    val x: Int,
    val z: Int,
    val level: Int,
    val areaSection: String? = null,
    val source: NpcSpawnSource = NpcSpawnSource.CSV,
)

data class NpcSpawnDumpResult(
    val spawns: List<ResolvedNpcSpawn>,
    val radii: Map<String, Int>,
    val unresolvedNpcIds: List<Int>,
    val skippedWikiPages: List<String>,
    val wikiAdded: Int,
)

class NpcSpawnCsvDumper(
    private val npcLookup: NpcRscmLookup,
    private val wiki: WikiClient?,
    private val log: DropDumpLog,
) {
    suspend fun dump(
        csvRows: List<CsvNpcSpawnRow>,
        supplementWiki: Boolean,
        wikiPageTitles: List<String>,
    ): NpcSpawnDumpResult {
        val csvCoordsByNpcKey = linkedMapOf<String, MutableList<CsvNpcCoord>>()
        val spawns = mutableListOf<ResolvedNpcSpawn>()
        val unresolved = linkedSetOf<Int>()
        val radii = linkedMapOf<String, Int>()

        for (row in csvRows) {
            val npcKey = npcLookup.toRscm(row.npcId)
            if (npcKey == null) {
                unresolved += row.npcId
                log.verbose("csv unresolved npc id ${row.npcId} (${row.ingameName})")
                continue
            }
            if (NpcSpawnFilters.isExcluded(npcKey, row.ingameName)) {
                log.verbose("csv skip filtered npc $npcKey (${row.ingameName})")
                continue
            }
            csvCoordsByNpcKey.getOrPut(npcKey) { mutableListOf() } +=
                CsvNpcCoord(x = row.x, z = row.z, plane = row.plane)
            spawns +=
                ResolvedNpcSpawn(
                    npcKey = npcKey,
                    coords = WikiItemSpawnParser.formatCoordGrid(row.x, row.z, row.plane),
                    x = row.x,
                    z = row.z,
                    level = row.plane,
                    source = NpcSpawnSource.CSV,
                )
        }

        var wikiAdded = 0
        val skippedWiki = mutableListOf<String>()
        if (supplementWiki && wiki != null) {
            for (title in wikiPageTitles) {
                if (title.isBlank()) {
                    continue
                }
                if (NpcSpawnFilters.isExcludedWikiPage(title)) {
                    continue
                }
                val added =
                    supplementFromWikiPage(
                        title = title,
                        csvCoordsByNpcKey = csvCoordsByNpcKey,
                        spawns = spawns,
                        radii = radii,
                        unresolved = unresolved,
                    )
                if (added == 0) {
                    skippedWiki += title
                } else {
                    wikiAdded += added
                }
            }
        }

        return NpcSpawnDumpResult(
            spawns = dedupe(spawns),
            radii = radii,
            unresolvedNpcIds = unresolved.sorted(),
            skippedWikiPages = skippedWiki,
            wikiAdded = wikiAdded,
        )
    }

    private suspend fun supplementFromWikiPage(
        title: String,
        csvCoordsByNpcKey: Map<String, List<CsvNpcCoord>>,
        spawns: MutableList<ResolvedNpcSpawn>,
        radii: MutableMap<String, Int>,
        unresolved: MutableSet<Int>,
    ): Int {
        if (title.isBlank()) {
            return 0
        }
        if (NpcSpawnFilters.isExcludedWikiPage(title)) {
            log.verbose("$title skip wiki — filtered page")
            return 0
        }
        val source =
            try {
                wiki?.rawPageSource(title) ?: return 0
            } catch (error: IllegalStateException) {
                log.verbose("$title skip wiki — ${error.message}")
                return 0
            }
        val npcId = WikiNpcSpawnParser.resolveSingleNpcId(source) ?: run {
            log.verbose("$title skip wiki — multi-version or missing id")
            return 0
        }
        val npcKey = npcLookup.toRscm(npcId)
        if (npcKey == null) {
            unresolved += npcId
            log.verbose("$title unresolved wiki npc id $npcId")
            return 0
        }
        if (NpcSpawnFilters.isExcluded(npcKey)) {
            log.verbose("$title skip wiki — filtered npc $npcKey")
            return 0
        }

        var added = 0

        for (mapSpawn in WikiNpcSpawnParser.parseInfoboxMapSpawns(source)) {
            mapSpawn.radius?.let { radius ->
                recordRadius(radii, npcKey, radius, title)
            }
            added +=
                addWikiSpawn(
                    npcKey = npcKey,
                    x = mapSpawn.x,
                    z = mapSpawn.z,
                    plane = mapSpawn.plane,
                    csvCoordsByNpcKey = csvCoordsByNpcKey,
                    spawns = spawns,
                    source = NpcSpawnSource.WIKI_INFOBOX_MAP,
                    pageTitle = title,
                )
        }

        for (locSpawn in WikiNpcSpawnParser.parseLocLineSpawns(source)) {
            for (coord in locSpawn.coords) {
                added +=
                    addWikiSpawn(
                        npcKey = npcKey,
                        x = coord.x,
                        z = coord.z,
                        plane = coord.level,
                        csvCoordsByNpcKey = csvCoordsByNpcKey,
                        spawns = spawns,
                        source = NpcSpawnSource.WIKI_LOC_LINE,
                        pageTitle = title,
                    )
            }
        }

        if (added > 0) {
            log.verbose("$title added $added wiki spawn(s) for $npcKey")
        }
        return added
    }

    private fun addWikiSpawn(
        npcKey: String,
        x: Int,
        z: Int,
        plane: Int,
        csvCoordsByNpcKey: Map<String, List<CsvNpcCoord>>,
        spawns: MutableList<ResolvedNpcSpawn>,
        source: NpcSpawnSource,
        pageTitle: String,
    ): Int {
        val csvCoords = csvCoordsByNpcKey[npcKey]
        when {
            csvCoords == null -> {
                // NPC not in CSV — add wiki spawn as-is.
            }
            csvCoords.size == 1 -> {
                log.verbose("$pageTitle skip $npcKey @ $x,$z,$plane — single csv instance is authoritative")
                return 0
            }
            isCoveredByCsvAreas(x, z, plane, csvCoords) -> {
                log.verbose("$pageTitle skip $npcKey @ $x,$z,$plane — within 10x10 of csv spawn")
                return 0
            }
        }
        spawns +=
            ResolvedNpcSpawn(
                npcKey = npcKey,
                coords = WikiItemSpawnParser.formatCoordGrid(x, z, plane),
                x = x,
                z = z,
                level = plane,
                source = source,
            )
        return 1
    }

    private fun isCoveredByCsvAreas(
        x: Int,
        z: Int,
        plane: Int,
        csvCoords: List<CsvNpcCoord>,
    ): Boolean =
        csvCoords.any { csv ->
            csv.plane == plane && isWithin10x10Area(x, z, csv.x, csv.z)
        }

    /** Centered 10x10 tile area around a csv coordinate. */
    private fun isWithin10x10Area(x: Int, z: Int, anchorX: Int, anchorZ: Int): Boolean {
        val minX = anchorX - WIKI_CSV_AREA_HALF_SIZE
        val maxX = anchorX + WIKI_CSV_AREA_HALF_SIZE - 1
        val minZ = anchorZ - WIKI_CSV_AREA_HALF_SIZE
        val maxZ = anchorZ + WIKI_CSV_AREA_HALF_SIZE - 1
        return x in minX..maxX && z in minZ..maxZ
    }

    private fun recordRadius(
        radii: MutableMap<String, Int>,
        npcKey: String,
        radius: Int,
        pageTitle: String,
    ) {
        if (NpcSpawnFilters.isExcluded(npcKey)) {
            return
        }
        val existing = radii[npcKey]
        if (existing == null) {
            radii[npcKey] = radius
            return
        }
        if (existing != radius) {
            log.warn("$pageTitle radius mismatch for $npcKey: existing=$existing new=$radius")
        }
    }

    fun writeToml(spawns: List<ResolvedNpcSpawn>, output: Path) {
        output.parent?.createDirectories()
        output.writeText(formatToml(spawns))
    }

    fun writeTomlByArea(
        spawns: List<ResolvedNpcSpawn>,
        areaIndex: WikiAreaIndex,
        outputDir: Path,
    ): Map<String, Int> {
        outputDir.createDirectories()
        val buckets = bucketByArea(spawns, areaIndex)
        for ((slug, bucket) in buckets) {
            writeToml(bucket, outputDir.resolve("$slug.toml"))
        }
        return buckets.mapValues { it.value.size }
    }

    fun writeRadiusFile(radii: Map<String, Int>, output: Path) {
        output.parent?.createDirectories()
        val body =
            radii
                .toSortedMap()
                .entries
                .joinToString(separator = "\n") { (npcKey, radius) -> "$npcKey=$radius" }
        output.writeText(if (body.isEmpty()) "" else "$body\n")
    }

    private fun bucketByArea(
        spawns: List<ResolvedNpcSpawn>,
        areaIndex: WikiAreaIndex,
    ): Map<String, List<ResolvedNpcSpawn>> {
        val buckets = linkedMapOf<String, MutableList<ResolvedNpcSpawn>>()
        for (spawn in spawns) {
            val resolved =
                areaIndex.resolveArea(spawn.x, spawn.z, spawn.level)
                    ?: ResolvedArea(fileSlug = WikiAreaIndex.unassignedSlug())
            buckets
                .getOrPut(resolved.fileSlug) { mutableListOf() } +=
                spawn.copy(areaSection = resolved.sectionLabel)
        }
        return buckets.mapValues { (_, bucket) ->
            bucket.sortedWith(compareBy({ it.areaSection ?: "" }, { it.npcKey }, { it.coords }))
        }
    }

    private fun formatToml(spawns: List<ResolvedNpcSpawn>): String =
        buildString {
            var currentSection: String? = null
            val distinctSections = spawns.mapNotNull { it.areaSection }.distinct()
            val hasUnsectioned = spawns.any { it.areaSection == null }
            val closeLastSection = hasUnsectioned || distinctSections.size > 1

            fun hasNamedSectionBelow(fromIndex: Int): Boolean =
                spawns.subList(fromIndex, spawns.size).any { it.areaSection != null }

            fun closeSectionIfLast(fromIndex: Int) {
                if (!closeLastSection) {
                    return
                }
                if (currentSection != null && !hasNamedSectionBelow(fromIndex)) {
                    appendLine("## $currentSection End")
                    appendLine()
                }
            }

            for ((index, spawn) in spawns.withIndex()) {
                if (spawn.areaSection != currentSection) {
                    closeSectionIfLast(fromIndex = index)
                    currentSection = spawn.areaSection
                    if (spawn.areaSection != null) {
                        appendLine("## ${spawn.areaSection}")
                        appendLine()
                    }
                }
                appendLine("[[spawn]]")
                appendLine("npc = \"${spawn.npcKey}\"")
                appendLine("coords = \"${spawn.coords}\"")
                appendLine()
            }
            closeSectionIfLast(fromIndex = spawns.size)
        }

    private fun dedupe(spawns: List<ResolvedNpcSpawn>): List<ResolvedNpcSpawn> =
        spawns.distinctBy { "${it.npcKey}|${it.coords}" }
}

private fun defaultCsvPath(): Path {
    val resource = NpcSpawnCsvDumper::class.java.getResource("/$DEFAULT_CSV_RESOURCE")
    if (resource != null) {
        return Path.of(resource.toURI())
    }
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve("tools/wiki-dumping/src/main/resources/$DEFAULT_CSV_RESOURCE")
    } else {
        Path("tools/wiki-dumping/src/main/resources/$DEFAULT_CSV_RESOURCE")
    }
}

private fun defaultOutputDir(): Path {
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve(DEFAULT_OUTPUT_DIR_RELATIVE)
    } else {
        Path(DEFAULT_OUTPUT_DIR_RELATIVE)
    }
}

private fun defaultRadiusPath(): Path {
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve(DEFAULT_RADIUS_FILE_RELATIVE)
    } else {
        Path(DEFAULT_RADIUS_FILE_RELATIVE)
    }
}

private fun findRepoRoot(): Path? = GameValLoader.resolveRootOrNull()

private fun resolveInputPath(
    flagValue: String?,
    positional: String?,
    default: Path,
    repoRoot: Path?,
): Path {
    val raw = flagValue ?: positional
    if (raw == null) {
        return default
    }
    val path = Path(raw)
    return if (path.isAbsolute) path else (repoRoot ?: Path.of(".")).resolve(path).normalize()
}

private fun resolveOutputPath(
    flagValue: String?,
    default: Path,
    repoRoot: Path?,
): Path {
    if (flagValue == null) {
        return default
    }
    val path = Path(flagValue)
    return if (path.isAbsolute) path else (repoRoot ?: Path.of(".")).resolve(path).normalize()
}

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val positional = args.filterNot { it.startsWith("-") }

    val fetchDumpNpc = flags.contains("--fetch-dump")
    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val verbose = flags.contains("--verbose") || flags.contains("-v")
    val supplementWiki = !flags.contains("--no-wiki")
    val splitByArea = !flags.contains("--combined")
    val limit = parseScanLimit(args)
    val rlhdAreasUrl =
        flags.firstOrNull { it.startsWith("--rlhd-areas=") }?.substringAfter("--rlhd-areas=")
            ?: RlhdAreaLoader.DEFAULT_RLHD_AREAS_URL
    val wikiDumpDir = flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter("--root=")
            ?: System.getProperty("RSPS_ROOT")
            ?: findRepoRoot()?.toString()
    val repoRoot = rootDir?.let { Path(it) } ?: findRepoRoot()
    val csvPath =
        resolveInputPath(
            flagValue = flags.firstOrNull { it.startsWith("--csv=") }?.substringAfter("--csv="),
            positional = positional.firstOrNull(),
            default = defaultCsvPath(),
            repoRoot = repoRoot,
        )
    val outputDir =
        resolveOutputPath(
            flags.firstOrNull { it.startsWith("--out-dir=") }?.substringAfter("--out-dir="),
            defaultOutputDir(),
            repoRoot,
        )
    val radiusFile =
        resolveOutputPath(
            flags.firstOrNull { it.startsWith("--radius-out=") }?.substringAfter("--radius-out="),
            defaultRadiusPath(),
            repoRoot,
        )
    val log = DropDumpLog(quiet = quiet, verbose = verbose)

    if (!csvPath.exists()) {
        log.error("csv not found: ${csvPath.toAbsolutePath().normalize()}")
        exitProcess(1)
    }

    runBlocking {
        val totalElapsed =
            measureTimeMillis {
                GameValLoader.ensureLoaded(rootDir)
                val dump = NpcDumpFiles.readOrFetch(rootDir, fetchDumpNpc)
                if (dump.downloaded) {
                    log.info("downloaded dump.npc to ${dump.source?.toAbsolutePath()?.normalize()}")
                }
                val npcLookup = NpcRscmLookup.load(rootDir, dump.text)
                val csvRows = CsvNpcSpawnReader.read(csvPath)
                log.info("loaded ${csvRows.size} csv spawn row(s) from ${csvPath.fileName}")

                WikiClient.open(wikiDumpDir = wikiDumpDir, onPageFetch = { log.onWikiFetch(it) }).use { wiki ->
                    log.info("loaded ${wiki.loadedPages} wiki page(s)")

                    val wikiPageTitles =
                        if (supplementWiki) {
                            collectWikiSupplementPages(wiki, csvRows, limit, log)
                        } else {
                            emptyList()
                        }

                    val areaIndex =
                        if (splitByArea) {
                            WikiAreaIndex.load(
                                wikiStore = wiki.wikiDumpStore(),
                                rootDir = repoRoot,
                                log = log,
                                rlhdAreasUrl = rlhdAreasUrl,
                            )
                        } else {
                            null
                        }

                    val dumper = NpcSpawnCsvDumper(npcLookup, wiki, log)
                    val result = dumper.dump(csvRows, supplementWiki, wikiPageTitles)

                    if (splitByArea) {
                        requireNotNull(areaIndex)
                        val buckets = dumper.writeTomlByArea(result.spawns, areaIndex, outputDir)
                        val unassigned = buckets[WikiAreaIndex.unassignedSlug()] ?: 0
                        log.info(
                            "wrote ${result.spawns.size} spawn(s) across ${buckets.size} file(s) " +
                                "in ${outputDir.toAbsolutePath().normalize()} " +
                                "($unassigned unassigned)",
                        )
                    } else {
                        val combined = outputDir.resolve("npc_spawns.toml")
                        dumper.writeToml(result.spawns, combined)
                        log.info(
                            "wrote ${result.spawns.size} spawn(s) to ${combined.toAbsolutePath().normalize()}",
                        )
                    }

                    dumper.writeRadiusFile(result.radii, radiusFile)
                    log.info(
                        "wrote ${result.radii.size} npc radius mapping(s) to " +
                            radiusFile.toAbsolutePath().normalize(),
                    )

                    if (result.wikiAdded > 0) {
                        log.info("added ${result.wikiAdded} spawn(s) from wiki supplement")
                    }
                    if (result.unresolvedNpcIds.isNotEmpty()) {
                        log.warn("${result.unresolvedNpcIds.size} unresolved npc id(s)")
                    }
                }
            }
        log.summary(1, totalElapsed)
    }
}

private suspend fun collectWikiSupplementPages(
    wiki: WikiClient,
    csvRows: List<CsvNpcSpawnRow>,
    limit: Int?,
    log: DropDumpLog,
): List<String> {
    val titles = linkedSetOf<String>()
    for (row in csvRows) {
        val wikiVersion = row.wikiVersion?.trim()?.takeIf { it.isNotBlank() } ?: continue
        titles += CsvNpcSpawnReader.wikiPageTitle(wikiVersion)
    }
    val locLinePages = WikiDumpStorePages.listLocLineTitles(wiki.wikiDumpStore())
    val infoboxMapPages = WikiDumpStorePages.listInfoboxNpcMapTitles(wiki.wikiDumpStore())
    titles += locLinePages
    titles += infoboxMapPages
    log.info(
        "wiki supplement will scan ${titles.size} page(s) " +
            "(${locLinePages.size} LocLine, ${infoboxMapPages.size} Infobox NPC map)",
    )
    val sorted = titles.filter { it.isNotBlank() && !NpcSpawnFilters.isExcludedWikiPage(it) }.sorted()
    return if (limit != null) sorted.take(limit) else sorted
}
