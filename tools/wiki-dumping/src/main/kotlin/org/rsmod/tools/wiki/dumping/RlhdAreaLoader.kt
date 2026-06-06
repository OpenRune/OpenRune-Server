package org.rsmod.tools.wiki.dumping

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

internal object RlhdAreaLoader {
    const val DEFAULT_RLHD_AREAS_URL =
        "https://raw.githubusercontent.com/117HD/RLHD/refs/heads/master/src/main/resources/rs117/hd/scene/areas.json"

    fun loadFromUrl(url: String, log: DropDumpLog): List<WikiAreaRegion> {
        log.info("loading RLHD areas from $url")
        val json =
            runCatching {
                runBlocking {
                    HttpClient(CIO).use { client ->
                        client.get(url).bodyAsText()
                    }
                }
            }.getOrElse { error ->
                log.warn("failed to fetch RLHD areas from $url: ${error.message}")
                return emptyList()
            }
        return parse(json, log)
    }

    private fun parse(json: String, log: DropDumpLog): List<WikiAreaRegion> {
        val root =
            runCatching {
                jacksonObjectMapper().readTree(sanitizeJsonArray(json))
            }.getOrElse { error ->
                log.warn("failed to parse RLHD areas JSON: ${error.message}")
                return emptyList()
            }
        if (!root.isArray) {
            log.warn("RLHD areas JSON is not an array")
            return emptyList()
        }

        val rawEntries = root.mapNotNull(::parseRawEntry)
        val byName = rawEntries.associateBy { it.name }
        val visiting = mutableSetOf<String>()

        val regions =
            rawEntries.mapNotNull { entry ->
                if (!isSpawnBucket(entry.name)) {
                    return@mapNotNull null
                }
                val boxes = resolveBoxes(entry, byName, visiting)
                if (boxes.isEmpty()) {
                    return@mapNotNull null
                }
                WikiAreaRegion(
                    name = entry.name,
                    slug = slugifyRlhd(entry.name),
                    source = AreaSource.RLHD,
                    polygons = emptyList(),
                    boxes = boxes,
                    levels = 0..3,
                    rankArea = boxes.sumOf { it.area },
                )
            }

        log.verbose("loaded ${regions.size} RLHD area region(s)")
        return regions
    }

    private fun parseRawEntry(node: JsonNode): RlhdRawEntry? {
        val name = node.path("name").takeIf { it.isTextual }?.asText()?.trim().orEmpty()
        if (name.isBlank()) {
            return null
        }
        val parsedAabbs = parseAabbNode(node.path("aabbs"))
        return RlhdRawEntry(
            name = name,
            aabbs = parsedAabbs.intArrays,
            aabbRefs = parsedAabbs.stringRefs,
            areaRefs = parseStringArray(node.path("areas")),
            regionIds = parseIntArray(node.path("regions")),
            regionBoxIds = parseRegionBoxArrays(node.path("regionBoxes")),
        )
    }

    private data class ParsedAabbs(
        val intArrays: List<IntArray>,
        val stringRefs: List<String>,
    )

    private fun parseAabbNode(node: JsonNode): ParsedAabbs {
        if (!node.isArray) {
            return ParsedAabbs(emptyList(), emptyList())
        }
        val intArrays = mutableListOf<IntArray>()
        val stringRefs = mutableListOf<String>()
        for (entry in node) {
            when {
                entry.isArray -> {
                    val values = entry.mapNotNull { value -> value.takeIf { it.isNumber }?.asInt() }.toIntArray()
                    if (values.isNotEmpty()) {
                        intArrays += values
                    }
                }
                entry.isTextual -> {
                    entry.asText().trim().takeIf { it.isNotEmpty() }?.let { stringRefs += it }
                }
            }
        }
        return ParsedAabbs(intArrays, stringRefs)
    }

    /** Strip markdown wrappers (e.g. from browser saves) so Jackson sees a JSON array. */
    private fun sanitizeJsonArray(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) {
            return trimmed
        }
        val start = trimmed.indexOf('[')
        check(start >= 0) { "no JSON array found" }
        return trimmed.substring(start)
    }

    private fun parseStringArray(node: JsonNode): List<String> {
        if (!node.isArray) {
            return emptyList()
        }
        return node.mapNotNull { value -> value.takeIf { it.isTextual }?.asText()?.trim()?.takeIf { it.isNotEmpty() } }
    }

    private fun parseIntArray(node: JsonNode): List<Int> {
        if (!node.isArray) {
            return emptyList()
        }
        return node.mapNotNull { value -> value.takeIf { it.isNumber }?.asInt() }
    }

    private fun parseRegionBoxArrays(node: JsonNode): List<IntArray> {
        if (!node.isArray) {
            return emptyList()
        }
        return node.mapNotNull { entry ->
            if (!entry.isArray) {
                return@mapNotNull null
            }
            entry.mapNotNull { value -> value.takeIf { it.isNumber }?.asInt() }.toIntArray().takeIf { it.isNotEmpty() }
        }
    }

    private fun resolveBoxes(
        entry: RlhdRawEntry,
        byName: Map<String, RlhdRawEntry>,
        visiting: MutableSet<String>,
    ): List<AreaBox> {
        if (!visiting.add(entry.name)) {
            return emptyList()
        }
        try {
            val boxes = mutableListOf<AreaBox>()
            for (values in entry.aabbs) {
                AreaBox.fromInts(values)?.let { boxes += it }
            }
            for (ref in entry.aabbRefs) {
                val referenced = byName[ref] ?: continue
                boxes += resolveBoxes(referenced, byName, visiting)
            }
            for (regionId in entry.regionIds) {
                boxes += AreaBox.fromRegionId(regionId)
            }
            for (regionBox in entry.regionBoxIds) {
                when (regionBox.size) {
                    1 -> boxes += AreaBox.fromRegionId(regionBox[0])
                    2 -> {
                        boxes += AreaBox.fromRegionId(regionBox[0])
                        boxes += AreaBox.fromRegionId(regionBox[1])
                    }
                }
            }
            for (ref in entry.areaRefs) {
                val referenced = byName[ref] ?: continue
                boxes += resolveBoxes(referenced, byName, visiting)
            }
            return boxes
        } finally {
            visiting.remove(entry.name)
        }
    }

    private fun isSpawnBucket(name: String): Boolean {
        val upper = name.uppercase()
        if ("FIX" in upper) {
            return false
        }
        if ("BLEND" in upper) {
            return false
        }
        if ("BRIGHTNESS" in upper) {
            return false
        }
        if (upper.endsWith("_INSTANCE")) {
            return false
        }
        if ("NO_BLENDING" in upper) {
            return false
        }
        return true
    }

    private fun slugifyRlhd(name: String): String =
        name
            .lowercase()
            .replace("'", "")
            .replace(Regex("""[^a-z0-9]+"""), "_")
            .trim('_')

    private data class RlhdRawEntry(
        val name: String,
        val aabbs: List<IntArray>,
        val aabbRefs: List<String>,
        val areaRefs: List<String>,
        val regionIds: List<Int>,
        val regionBoxIds: List<IntArray>,
    )
}
