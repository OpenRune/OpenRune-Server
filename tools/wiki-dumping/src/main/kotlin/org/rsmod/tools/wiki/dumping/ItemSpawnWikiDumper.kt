package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiItemSpawnParser

private const val DEFAULT_OUTPUT_RELATIVE = ".data/raw-cache/map/objs/wiki_item_spawns.toml"
private const val DEFAULT_OUTPUT_DIR_RELATIVE = ".data/raw-cache/map/objs"

data class ResolvedItemSpawn(
    val objKey: String,
    val coords: String,
    val x: Int,
    val z: Int,
    val level: Int,
    val count: Int = 1,
    val areaSection: String? = null,
)

data class ItemSpawnDumpResult(
    val spawns: List<ResolvedItemSpawn>,
    val skippedPages: List<String>,
    val unresolvedItems: List<String>,
)

class ItemSpawnWikiDumper(
    private val wiki: WikiClient,
    private val objLookup: ObjRscmLookup,
    private val itemLookup: ItemWikiLookup,
    private val log: DropDumpLog,
) {
    suspend fun dumpPage(pageTitle: String): ItemSpawnDumpResult {
        val source = wiki.rawPageSource(pageTitle)
        val parsed = WikiItemSpawnParser.parseAllSpawns(source)
        if (parsed.isEmpty()) {
            log.verbose("$pageTitle skip — no ItemSpawnLine templates")
            return ItemSpawnDumpResult(emptyList(), listOf(pageTitle), emptyList())
        }

        itemLookup.prewarm(parsed.map { it.itemName } + pageTitle)

        val spawns = mutableListOf<ResolvedItemSpawn>()
        val unresolved = linkedSetOf<String>()

        for (spawn in parsed) {
            val objKey = objLookup.resolveWikiItemOnPage(itemLookup, spawn.itemName, pageTitle)
            if (objKey == null) {
                unresolved += spawn.itemName
                log.warn("$pageTitle unresolved item '${spawn.itemName}'")
                continue
            }
            for (coord in spawn.coords) {
                spawns +=
                    ResolvedItemSpawn(
                        objKey = objKey,
                        coords =
                            WikiItemSpawnParser.formatCoordGrid(
                                x = coord.x,
                                z = coord.z,
                                level = coord.level,
                            ),
                        x = coord.x,
                        z = coord.z,
                        level = coord.level,
                        count = coord.count,
                    )
            }
        }

        return ItemSpawnDumpResult(
            spawns = spawns,
            skippedPages = emptyList(),
            unresolvedItems = unresolved.toList(),
        )
    }

    suspend fun dumpPages(pageTitles: List<String>): ItemSpawnDumpResult {
        val allSpawns = mutableListOf<ResolvedItemSpawn>()
        val skipped = mutableListOf<String>()
        val unresolved = linkedSetOf<String>()

        for (title in pageTitles) {
            val result = dumpPage(title)
            allSpawns += result.spawns
            skipped += result.skippedPages
            unresolved += result.unresolvedItems
        }

        return ItemSpawnDumpResult(
            spawns = dedupe(allSpawns),
            skippedPages = skipped,
            unresolvedItems = unresolved.toList(),
        )
    }

    fun writeToml(spawns: List<ResolvedItemSpawn>, output: Path) {
        output.parent?.createDirectories()
        output.writeText(formatToml(spawns))
    }

    fun writeTomlByArea(
        spawns: List<ResolvedItemSpawn>,
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

    private fun bucketByArea(
        spawns: List<ResolvedItemSpawn>,
        areaIndex: WikiAreaIndex,
    ): Map<String, List<ResolvedItemSpawn>> {
        val buckets = linkedMapOf<String, MutableList<ResolvedItemSpawn>>()
        for (spawn in spawns) {
            val resolved =
                areaIndex.resolveArea(spawn.x, spawn.z, spawn.level)
                    ?: ResolvedArea(fileSlug = WikiAreaIndex.unassignedSlug())
            buckets
                .getOrPut(resolved.fileSlug) { mutableListOf() } +=
                spawn.copy(areaSection = resolved.sectionLabel)
        }
        return buckets.mapValues { (_, bucket) ->
            bucket.sortedWith(compareBy({ it.areaSection ?: "" }, { it.objKey }, { it.coords }))
        }
    }

    private fun formatToml(spawns: List<ResolvedItemSpawn>): String =
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
                appendLine("obj = \"${spawn.objKey}\"")
                appendLine("coords = \"${spawn.coords}\"")
                if (spawn.count != 1) {
                    appendLine("count = ${spawn.count}")
                }
                appendLine()
            }
            closeSectionIfLast(fromIndex = spawns.size)
        }

    private fun dedupe(spawns: List<ResolvedItemSpawn>): List<ResolvedItemSpawn> =
        spawns.distinctBy { "${it.objKey}|${it.coords}|${it.count}" }
}

private fun defaultOutputDir(): Path {
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve(DEFAULT_OUTPUT_DIR_RELATIVE)
    } else {
        Path(DEFAULT_OUTPUT_DIR_RELATIVE)
    }
}

private fun findRepoRoot(): Path? = GameValLoader.resolveRootOrNull()

private fun defaultOutputPath(): Path {
    val repo = findRepoRoot()
    return if (repo != null) {
        repo.resolve(DEFAULT_OUTPUT_RELATIVE)
    } else {
        Path(DEFAULT_OUTPUT_RELATIVE)
    }
}

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val positional = args.filterNot { it.startsWith("-") }

    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val verbose = flags.contains("--verbose") || flags.contains("-v")
    val dumpAll = flags.contains("--all") || positional.isEmpty()
    val limit = parseScanLimit(args)
    val splitByArea = !flags.contains("--combined")
    val rlhdAreasUrl =
        flags.firstOrNull { it.startsWith("--rlhd-areas=") }?.substringAfter("--rlhd-areas=")
            ?: RlhdAreaLoader.DEFAULT_RLHD_AREAS_URL
    val wikiDumpDir = flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter("--root=")
            ?: System.getProperty("RSPS_ROOT")
            ?: findRepoRoot()?.toString()
    val repoRoot = rootDir?.let { Path(it) } ?: findRepoRoot()
    val outputFile =
        flags.firstOrNull { it.startsWith("--out=") }?.substringAfter("--out=")?.let { Path(it) }
            ?: if (!splitByArea) defaultOutputPath() else null
    val outputDir =
        flags.firstOrNull { it.startsWith("--out-dir=") }?.substringAfter("--out-dir=")?.let { Path(it) }
            ?: if (splitByArea) defaultOutputDir() else null
    val log = DropDumpLog(quiet = quiet, verbose = verbose)

    runBlocking {
        val totalElapsed =
            measureTimeMillis {
                GameValLoader.ensureLoaded(rootDir)
                val objLookup = ObjRscmLookup()

                val dumpDir = WikiClient.resolveDumpDirectory(wikiDumpDir)
                log.info("wiki dump: ${dumpDir.absolutePath}")

                WikiClient.open(wikiDumpDir = wikiDumpDir, onPageFetch = { log.onWikiFetch(it) }).use { wiki ->
                    log.info(
                        "loaded ${wiki.loadedPages} wiki page(s) " +
                            "(${wiki.itemSpawnPageCount} ItemSpawnLine page(s))",
                    )
                    val areaIndex =
                        WikiAreaIndex.load(
                            wikiStore = wiki.wikiDumpStore(),
                            rootDir = repoRoot,
                            log = log,
                            rlhdAreasUrl = rlhdAreasUrl,
                        )
                    val itemLookup = ItemWikiLookup(wiki, log)
                    val dumper = ItemSpawnWikiDumper(wiki, objLookup, itemLookup, log)

                    val pageTitles =
                        if (dumpAll) {
                            collectAllItemSpawnPages(wiki, limit, log)
                        } else {
                            positional
                        }

                    if (pageTitles.isEmpty()) {
                        log.error("no wiki pages to scan")
                        exitProcess(1)
                    }

                    log.info("scanning ${pageTitles.size} page(s)")
                    val result = dumper.dumpPages(pageTitles)
                    if (splitByArea) {
                        val dir = outputDir ?: defaultOutputDir()
                        val buckets = dumper.writeTomlByArea(result.spawns, areaIndex, dir)
                        val unassigned = buckets[WikiAreaIndex.unassignedSlug()] ?: 0
                        log.info(
                            "wrote ${result.spawns.size} spawn(s) across ${buckets.size} file(s) " +
                                "in ${dir.toAbsolutePath().normalize()} " +
                                "($unassigned unassigned)",
                        )
                    } else {
                        val file = outputFile ?: defaultOutputPath()
                        dumper.writeToml(result.spawns, file)
                        log.info(
                            "wrote ${result.spawns.size} spawn(s) to ${file.toAbsolutePath().normalize()}",
                        )
                    }
                    if (result.unresolvedItems.isNotEmpty()) {
                        log.warn("${result.unresolvedItems.size} unresolved item name(s)")
                    }
                    if (result.skippedPages.isNotEmpty()) {
                        log.warn("${result.skippedPages.size} page(s) had no spawn lines")
                    }
                }
            }
        log.summary(if (dumpAll) 1 else positional.size, totalElapsed)
    }
}

private suspend fun collectAllItemSpawnPages(
    wiki: WikiClient,
    limit: Int?,
    log: DropDumpLog,
): List<String> {
    val titles = mutableListOf<String>()
    var continueToken: String? = null
    while (true) {
        val batch = wiki.fetchItemSpawnPageBatch(batchSize = 500, continueToken = continueToken)
        titles += batch.titles
        log.verbose("indexed ${titles.size}/${wiki.itemSpawnPageCount} spawn page(s)")
        if (limit != null && titles.size >= limit) {
            return titles.take(limit)
        }
        continueToken = batch.continueToken ?: break
    }
    return titles
}
