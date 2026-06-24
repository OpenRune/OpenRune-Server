package org.rsmod.tools.wiki.dumping.wiki

data class WikiNpcMapSpawn(
    val x: Int,
    val z: Int,
    val plane: Int,
    val radius: Int?,
)

data class WikiNpcLocSpawn(
    val name: String,
    val location: String?,
    val coords: List<WikiSpawnCoord>,
)

object WikiNpcSpawnParser {
    private val versionedIds =
        Regex("""\|\s*id(\d+)\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    private val bareId =
        Regex("""\|\s*id\s*=\s*([\d,\s]+)""", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))

    private val infoboxMap =
        Regex(
            """\|\s*map\s*=\s*\{\{Map\b(.*?)\}\}""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )

    private val mapX = Regex("""\bx\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val mapY = Regex("""\by\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val mapPlane = Regex("""\bplane\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val mapRadius = Regex("""\br\s*=\s*(\d+)""", RegexOption.IGNORE_CASE)

    /** Returns the npc id when the page has exactly one id field; otherwise null (multi-version). */
    fun resolveSingleNpcId(source: String): Int? {
        val versioned = versionedIds.findAll(source).toList()
        if (versioned.isNotEmpty()) {
            return null
        }
        val bare = bareId.find(source) ?: return null
        val ids = parseIdList(bare.groupValues[1])
        return ids.singleOrNull()
    }

    fun parseInfoboxMapSpawns(source: String): List<WikiNpcMapSpawn> =
        infoboxMap
            .findAll(source)
            .mapNotNull { match ->
                val inner = match.groupValues[1]
                val x = mapX.find(inner)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                val z = mapY.find(inner)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
                val plane = mapPlane.find(inner)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val radius = mapRadius.find(inner)?.groupValues?.get(1)?.toIntOrNull()
                WikiNpcMapSpawn(x = x, z = z, plane = plane, radius = radius)
            }.toList()

    fun parseLocLineSpawns(source: String): List<WikiNpcLocSpawn> =
        WikiTemplateParser
            .extractTemplates(source, "LocLine")
            .mapNotNull(::parseLocLine)
            .filter { it.coords.isNotEmpty() }

    private fun parseLocLine(content: String): WikiNpcLocSpawn? {
        val params = WikiTemplateParser.parseParams(content)
        val name =
            params["name"]?.let(::sanitizeWikiMarkup)?.takeIf { it.isNotBlank() }
                ?: return null
        val defaultLevel = params["plane"]?.toIntOrNull()?.takeIf(::isValidLevel) ?: 0
        val coords = parseLocCoords(content, params, defaultLevel)
        if (coords.isEmpty()) {
            return null
        }
        return WikiNpcLocSpawn(
            name = name,
            location = params["location"]?.let(::sanitizeWikiMarkup),
            coords = coords,
        )
    }

    private fun parseLocCoords(
        content: String,
        params: Map<String, String>,
        defaultLevel: Int,
    ): List<WikiSpawnCoord> {
        val fromParams =
            buildList {
                for ((key, value) in params) {
                    if (key.startsWith("_") || key == "coords" || key == "coord") {
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
        val locPair = Regex("""x\s*:\s*(\d+)\s*,\s*y\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
        return locPair
            .findAll(text)
            .mapNotNull {
                val x = it.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val z = it.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                WikiSpawnCoord(x, z, defaultLevel)
            }.toList()
    }

    private fun parseIdList(raw: String): List<Int> =
        raw
            .split(',')
            .mapNotNull { it.trim().takeIf(String::isNotEmpty)?.toIntOrNull() }

    private fun isValidLevel(level: Int): Boolean = level in 0 until 4

    private fun sanitizeWikiMarkup(input: String): String =
        input
            .replace(Regex("""\[\[([^|\]]+)\|([^\]]+)]]"""), "$2")
            .replace(Regex("""\[\[([^\]]+)]]"""), "$1")
            .replace("'''", "")
            .replace("''", "")
            .trim()
}
