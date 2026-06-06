package org.rsmod.tools.wiki.dumping.wiki

import org.rsmod.map.CoordGrid

data class ParsedWikiItemSpawn(
    val itemName: String,
    val location: String?,
    val members: String?,
    val coords: List<WikiSpawnCoord>,
)

data class WikiSpawnCoord(
    val x: Int,
    val z: Int,
    val level: Int,
    val count: Int = 1,
)

object WikiItemSpawnParser {
    private val spawnsHeader = Regex("""==\s*Spawns\s*==""", RegexOption.IGNORE_CASE)
    private val nextSectionHeader = Regex("""\n==(?!=)""")
    private val coordChunk = Regex("""\d+,\d+(?:,[^,|]+)*""")
    private val xyPair = Regex("""(\d+)\s*,\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val locPair = Regex("""x\s*:\s*(\d+)\s*,\s*y\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val planeTag = Regex("""plane\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val qtyTag = Regex("""(?:qty|quantity)\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)

    fun extractSpawnsSection(wikitext: String): String? {
        val match = spawnsHeader.find(wikitext) ?: return null
        val bodyStart = match.range.last + 1
        val rest = wikitext.substring(bodyStart)
        val end = nextSectionHeader.find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }

    fun parseSpawnsSection(section: String): List<ParsedWikiItemSpawn> =
        WikiTemplateParser
            .extractTemplates(section, "ItemSpawnLine")
            .mapNotNull(::parseSpawnLine)

    fun parseAllSpawns(wikitext: String): List<ParsedWikiItemSpawn> {
        val section = extractSpawnsSection(wikitext) ?: wikitext
        return parseSpawnsSection(section)
    }

    private fun parseSpawnLine(content: String): ParsedWikiItemSpawn? {
        val params = WikiTemplateParser.parseParams(content)
        val itemName =
            params["name"]?.let(::sanitizeWikiMarkup)?.takeIf { it.isNotBlank() }
                ?: return null
        val coords = parseCoords(content, params)
        if (coords.isEmpty()) {
            return null
        }
        return ParsedWikiItemSpawn(
            itemName = itemName,
            location = params["location"]?.let(::sanitizeWikiMarkup),
            members = params["members"],
            coords = coords,
        )
    }

    private fun parseCoords(content: String, params: Map<String, String>): List<WikiSpawnCoord> {
        val defaultLevel = params["plane"]?.toIntOrNull()?.takeIf(::isValidLevel) ?: 0
        val fromParams =
            buildList {
                for ((key, value) in params) {
                    if (key.startsWith("_")) {
                        addAll(parseCoordText(value, defaultLevel))
                    }
                    if (key == "coords" || key == "coord") {
                        addAll(parseCoordText(value, defaultLevel))
                    }
                }
            }
        if (fromParams.isNotEmpty()) {
            return fromParams.distinct()
        }

        return parseCoordText(content, defaultLevel).distinct()
    }

    private fun parseCoordText(text: String, defaultLevel: Int): List<WikiSpawnCoord> {
        val coords = mutableListOf<WikiSpawnCoord>()
        for (match in locPair.findAll(text)) {
            val x = match.groupValues[1].toIntOrNull() ?: continue
            val z = match.groupValues[2].toIntOrNull() ?: continue
            coords += WikiSpawnCoord(x, z, defaultLevel)
        }
        for (chunk in coordChunk.findAll(text)) {
            val xy = xyPair.find(chunk.value) ?: continue
            val x = xy.groupValues[1].toIntOrNull() ?: continue
            val z = xy.groupValues[2].toIntOrNull() ?: continue
            val level =
                planeTag
                    .find(chunk.value)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?.takeIf(::isValidLevel)
                    ?: defaultLevel
            val count =
                qtyTag
                    .find(chunk.value)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
                    ?: 1
            coords += WikiSpawnCoord(x, z, level, count)
        }
        return coords
    }

    fun formatCoordGrid(x: Int, z: Int, level: Int = 0): String {
        val coord = CoordGrid(x, z, level)
        return "${coord.level}_${coord.mx}_${coord.mz}_${coord.lx}_${coord.lz}"
    }

    private fun isValidLevel(level: Int): Boolean = level in 0 until CoordGrid.LEVEL_COUNT

    private fun sanitizeWikiMarkup(input: String): String =
        input
            .replace(Regex("""\[\[([^|\]]+)\|([^\]]+)]]"""), "$2")
            .replace(Regex("""\[\[([^\]]+)]]"""), "$1")
            .replace("'''", "")
            .replace("''", "")
            .trim()
}
