package org.rsmod.tools.wiki.dumping

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.openrune.wiki.WikiDumpStore
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.math.max
import kotlin.math.min
import org.rsmod.tools.wiki.dumping.wiki.WikiDumpStorePages
import org.rsmod.tools.wiki.dumping.wiki.WikiMapPolygonParser
import org.rsmod.tools.wiki.dumping.wiki.WikiNamedPolygon

enum class AreaSource {
    WIKI,
    LOCAL_JSON,
    RLHD,
}

data class WikiAreaRegion(
    val name: String,
    val slug: String,
    val source: AreaSource = AreaSource.WIKI,
    val polygons: List<List<Pair<Int, Int>>> = emptyList(),
    val boxes: List<AreaBox> = emptyList(),
    val levels: IntRange,
    val rankArea: Long,
) {
    fun contains(x: Int, z: Int, level: Int): Boolean {
        if (level !in levels) {
            return false
        }
        if (polygons.any { pointInPolygon(x, z, it) }) {
            return true
        }
        return boxes.any { it.contains(x, z, level) }
    }

    private fun pointInPolygon(x: Int, z: Int, polygon: List<Pair<Int, Int>>): Boolean {
        var inside = false
        var previous = polygon.last()
        for (current in polygon) {
            val (x1, z1) = current
            val (x0, z0) = previous
            val intersects = (z1 > z) != (z0 > z) && x < (x0 - x1) * (z - z1) / (z0 - z1).toDouble() + x1
            if (intersects) {
                inside = !inside
            }
            previous = current
        }
        return inside
    }
}

data class ResolvedArea(
    val fileSlug: String,
    /** Most specific matching sub-area name, when a smaller region also matched. */
    val sectionLabel: String? = null,
)

class WikiAreaIndex private constructor(
    private val wikiRegions: List<WikiAreaRegion>,
    private val localRegions: List<WikiAreaRegion>,
    private val rlhdRegions: List<WikiAreaRegion>,
    private val regionsBySlug: Map<String, WikiAreaRegion>,
) {
    fun resolveArea(x: Int, z: Int, level: Int): ResolvedArea? {
        val wikiMatches = wikiRegions.filter { it.contains(x, z, level) }
        val localMatches = localRegions.filter { it.contains(x, z, level) }
        val rlhdMatches = rlhdRegions.filter { it.contains(x, z, level) }

        val specificMatches =
            wikiMatches.filterNot { it.slug in AREA_FALLBACK_ONLY_SLUGS } +
                localMatches.filterNot { it.slug in AREA_FALLBACK_ONLY_SLUGS } +
                rlhdMatches.filterNot { it.slug in AREA_FALLBACK_ONLY_SLUGS }

        if (specificMatches.isNotEmpty()) {
            val winning = specificMatches.maxWith(areaMatchComparator)
            val mostSpecific = specificMatches.minByOrNull { it.rankArea }
            val sectionLabel =
                mostSpecific
                    ?.takeIf { it.slug != winning.slug }
                    ?.name
                    ?.let(::formatSectionLabel)
            return promoteToParent(
                winning = winning,
                sectionLabel = sectionLabel,
            )
        }

        val fallbackMatches =
            wikiMatches.filter { it.slug in AREA_FALLBACK_ONLY_SLUGS } +
                localMatches.filter { it.slug in AREA_FALLBACK_ONLY_SLUGS } +
                rlhdMatches.filter { it.slug in AREA_FALLBACK_ONLY_SLUGS }
        val bestFallback = fallbackMatches.maxWithOrNull(areaMatchComparator) ?: return null
        return ResolvedArea(fileSlug = bestFallback.slug)
    }

    fun resolveAreaSlug(x: Int, z: Int, level: Int): String? = resolveArea(x, z, level)?.fileSlug

    private val areaMatchComparator =
        compareBy<WikiAreaRegion>({ it.rankArea }, { sourcePriority(it.source) })

    private fun sourcePriority(source: AreaSource): Int =
        when (source) {
            AreaSource.WIKI -> 2
            AreaSource.LOCAL_JSON -> 1
            AreaSource.RLHD -> 0
        }

    private fun promoteToParent(
        winning: WikiAreaRegion,
        sectionLabel: String?,
    ): ResolvedArea {
        val parentSlug = shortestPromotableParentSlug(winning) ?: winning.slug
        val label =
            when {
                sectionLabel != null -> sectionLabel
                parentSlug != winning.slug -> formatSectionLabel(winning.name)
                else -> null
            }
        return ResolvedArea(fileSlug = parentSlug, sectionLabel = label)
    }

    private fun shortestPromotableParentSlug(child: WikiAreaRegion): String? {
        val parts = child.slug.split('_')
        for (len in 1 until parts.size) {
            val parentSlug = parts.take(len).joinToString("_")
            val parent = regionsBySlug[parentSlug] ?: continue
            if (child.slug.startsWith("${parent.slug}_") && parent.rankArea > child.rankArea) {
                return parentSlug
            }
        }
        return null
    }

    private fun formatSectionLabel(name: String): String {
        if (name.any { it.isLowerCase() }) {
            return name
        }
        return name
            .lowercase()
            .split('_')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    val regionCount: Int get() = wikiRegions.size + localRegions.size + rlhdRegions.size

    companion object {
        private const val DEFAULT_AREA_DIR = ".data/raw-cache/map/area"
        private const val UNASSIGNED_SLUG = "unassigned"

        /** Broad area regions — only used when no specific wiki/local/RLHD match exists. */
        private val AREA_FALLBACK_ONLY_SLUGS =
            setOf(
                "kingdom_of_asgarnia_region",
                "kingdom_of_kandarin_region",
                "kingdom_of_misthalin_region",
                "warm_regions",
                "snowy_regions",
                "multiway",
                "overworld",
                "zeah_upper_levels",
            )

        fun load(
            wikiStore: WikiDumpStore,
            rootDir: Path?,
            log: DropDumpLog,
            rlhdAreasUrl: String = RlhdAreaLoader.DEFAULT_RLHD_AREAS_URL,
        ): WikiAreaIndex {
            val fromWiki = loadFromWiki(wikiStore, log)
            val fromJson = loadFromAreaJson(defaultAreaDir(rootDir), log)
            val fromRlhd = RlhdAreaLoader.loadFromUrl(rlhdAreasUrl, log)

            log.info(
                "loaded ${fromWiki.size} wiki + ${fromJson.size} local + ${fromRlhd.size} RLHD " +
                    "area region(s) for spawn bucketing",
            )
            val wikiMerged = mergeRegions(fromWiki)
            val localMerged = mergeRegions(fromJson)
            val rlhdMerged = mergeRegions(fromRlhd)
            val regionsBySlug = mergeRegions(wikiMerged + localMerged + rlhdMerged).associateBy { it.slug }
            return WikiAreaIndex(
                wikiRegions = wikiMerged,
                localRegions = localMerged,
                rlhdRegions = rlhdMerged,
                regionsBySlug = regionsBySlug,
            )
        }

        fun unassignedSlug(): String = UNASSIGNED_SLUG

        private fun defaultAreaDir(rootDir: Path?): Path {
            val repo = rootDir ?: GameValLoader.resolveRootOrNull()
            return if (repo != null) {
                repo.resolve(DEFAULT_AREA_DIR)
            } else {
                Path.of(DEFAULT_AREA_DIR)
            }
        }

        private fun loadFromWiki(store: WikiDumpStore, log: DropDumpLog): List<WikiAreaRegion> {
            val grouped = linkedMapOf<String, MutableList<WikiNamedPolygon>>()
            var pages = 0
            for ((_, wikitext) in WikiDumpStorePages.allMainNamespacePages(store)) {
                pages++
                for (polygon in WikiMapPolygonParser.parseAll(wikitext)) {
                    grouped.getOrPut(locationSlug(polygon.name)) { mutableListOf() } += polygon
                }
            }
            log.verbose("indexed wiki map polygons from $pages page(s)")
            return grouped.map { (slug, polygons) ->
                toRegion(
                    name = locationDisplayName(polygons.first().name),
                    slug = slug,
                    source = AreaSource.WIKI,
                    polygons = polygons,
                )
            }
        }

        private fun loadFromAreaJson(areaDir: Path, log: DropDumpLog): List<WikiAreaRegion> {
            if (!areaDir.exists()) {
                return emptyList()
            }
            val mapper = jacksonObjectMapper()
            val regions = mutableListOf<WikiAreaRegion>()
            for (path in areaDir.listDirectoryEntries("*.json")) {
                if (!path.isRegularFile() || path.fileName.toString() == "rlhd_areas.json") {
                    continue
                }
                runCatching {
                    val entries: List<AreaJsonFile> = mapper.readValue(path.toFile().readText())
                    for (entry in entries) {
                        val polygons =
                            entry.polygons.map { polygon ->
                                polygon.vertices.map { vertex -> vertex.x to vertex.z }
                            }
                        if (polygons.isEmpty()) {
                            continue
                        }
                        val levels =
                            entry.levels
                                .filter { it in 0..3 }
                                .sorted()
                                .let { sorted ->
                                    if (sorted.isEmpty()) {
                                        0..3
                                    } else {
                                        sorted.first()..sorted.last()
                                    }
                                }
                        regions +=
                            WikiAreaRegion(
                                name = entry.name,
                                slug = path.nameWithoutExtension,
                                source = AreaSource.LOCAL_JSON,
                                polygons = polygons,
                                levels = levels,
                                rankArea = polygons.sumOf(::boundingBoxArea),
                            )
                    }
                }.onFailure { error ->
                    log.warn("failed to read area json ${path.fileName}: ${error.message}")
                }
            }
            log.verbose("loaded ${regions.size} area region(s) from ${areaDir.toAbsolutePath().normalize()}")
            return regions
        }

        private fun toRegion(
            name: String,
            slug: String,
            source: AreaSource,
            polygons: List<WikiNamedPolygon>,
        ): WikiAreaRegion {
            val vertexGroups = polygons.map { it.vertices }
            val levels =
                polygons
                    .map { it.levels }
                    .reduce { acc, range -> min(acc.first, range.first)..max(acc.last, range.last) }
            return WikiAreaRegion(
                name = name,
                slug = slug,
                source = source,
                polygons = vertexGroups,
                levels = levels,
                rankArea = vertexGroups.sumOf(::boundingBoxArea),
            )
        }

        private fun mergeRegions(regions: List<WikiAreaRegion>): List<WikiAreaRegion> {
            val bySlug = linkedMapOf<String, WikiAreaRegion>()
            for (region in regions) {
                val existing = bySlug[region.slug]
                if (existing == null) {
                    bySlug[region.slug] = region
                    continue
                }
                bySlug[region.slug] =
                    WikiAreaRegion(
                        name = existing.name,
                        slug = existing.slug,
                        source = existing.source,
                        polygons = existing.polygons + region.polygons,
                        boxes = existing.boxes + region.boxes,
                        levels = min(existing.levels.first, region.levels.first)..max(existing.levels.last, region.levels.last),
                        rankArea = max(existing.rankArea, region.rankArea),
                    )
            }
            return bySlug.values.sortedBy { it.slug }
        }

        private fun normalizeName(name: String): String = name.trim().trimEnd('.')

        private fun locationSlug(name: String): String = slugify(parentLocationName(name))

        private fun locationDisplayName(name: String): String = parentLocationName(name)

        private fun parentLocationName(name: String): String {
            val trimmed = normalizeName(name)
            val match = PARENTHETICAL_LOCATION.find(trimmed) ?: return trimmed
            return match.groupValues[1].trim().ifBlank { trimmed }
        }

        private fun slugify(name: String): String =
            name
                .lowercase()
                .replace("'", "")
                .replace(Regex("""[^a-z0-9]+"""), "_")
                .trim('_')

        private val PARENTHETICAL_LOCATION = Regex("""\(([^)]+)\)\s*$""")

        private fun boundingBoxArea(vertices: List<Pair<Int, Int>>): Long {
            if (vertices.isEmpty()) {
                return 0
            }
            val minX = vertices.minOf { it.first }
            val maxX = vertices.maxOf { it.first }
            val minZ = vertices.minOf { it.second }
            val maxZ = vertices.maxOf { it.second }
            return (maxX - minX).toLong() * (maxZ - minZ)
        }
    }

    private data class AreaJsonFile(
        val name: String,
        val areaId: String? = null,
        val levels: List<Int> = emptyList(),
        val polygons: List<AreaJsonPolygon> = emptyList(),
    )

    private data class AreaJsonPolygon(
        val vertices: List<AreaJsonVertex> = emptyList(),
    )

    private data class AreaJsonVertex(
        val x: Int,
        val z: Int,
    )
}
