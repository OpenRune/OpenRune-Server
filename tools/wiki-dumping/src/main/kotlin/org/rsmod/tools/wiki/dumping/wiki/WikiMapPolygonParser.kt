package org.rsmod.tools.wiki.dumping.wiki

data class WikiNamedPolygon(
    val name: String,
    val vertices: List<Pair<Int, Int>>,
    val levels: IntRange,
    val mapId: Int?,
)

object WikiMapPolygonParser {
    private val colonPair = Regex("""(\d+)\s*:\s*(\d+)""")
    private val commaPair = Regex("""(\d+)\s*,\s*(\d+)""")
    private val xyPair = Regex("""x\s*:\s*(\d+)\s*,\s*y\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)

    fun parseAll(wikitext: String): List<WikiNamedPolygon> =
        WikiTemplateParser
            .extractTemplates(wikitext, "Map")
            .mapNotNull(::parseMapTemplate)

    private fun parseMapTemplate(content: String): WikiNamedPolygon? {
        val params = WikiTemplateParser.parseParams(content)
        if (!params["mtype"].equals("polygon", ignoreCase = true)) {
            return null
        }
        val name = params["name"]?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val vertices = parseVertices(content, params)
        if (vertices.size < 3) {
            return null
        }
        return WikiNamedPolygon(
            name = name,
            vertices = vertices,
            levels = parseLevels(params),
            mapId = params["mapid"]?.toIntOrNull(),
        )
    }

    private fun parseLevels(params: Map<String, String>): IntRange {
        val plane = params["plane"]?.toIntOrNull()
        return if (plane != null && plane in 0..3) {
            plane..plane
        } else {
            0..3
        }
    }

    private fun parseVertices(content: String, params: Map<String, String>): List<Pair<Int, Int>> {
        val vertices = linkedSetOf<Pair<Int, Int>>()

        for ((key, value) in params) {
            if (key in IGNORED_PARAM_KEYS) {
                continue
            }
            vertices += parseCoordBlob(value)
        }

        if (vertices.size >= 3) {
            return vertices.toList()
        }

        val stripped =
            content
                .split('|')
                .filterNot { part ->
                    val trimmed = part.trim()
                    trimmed.contains('=') &&
                        trimmed.substringBefore('=').trim().lowercase() in IGNORED_PARAM_KEYS
                }.joinToString("|")

        vertices += parseCoordBlob(stripped)
        return vertices.distinct()
    }

    private fun parseCoordBlob(text: String): List<Pair<Int, Int>> {
        val vertices = mutableListOf<Pair<Int, Int>>()
        for (match in xyPair.findAll(text)) {
            val x = match.groupValues[1].toIntOrNull() ?: continue
            val z = match.groupValues[2].toIntOrNull() ?: continue
            vertices += x to z
        }
        for (match in colonPair.findAll(text)) {
            val x = match.groupValues[1].toIntOrNull() ?: continue
            val z = match.groupValues[2].toIntOrNull() ?: continue
            vertices += x to z
        }
        for (part in text.split('|')) {
            val trimmed = part.trim()
            if (trimmed.contains(':') || trimmed.contains('=')) {
                continue
            }
            val match = commaPair.matchEntire(trimmed) ?: continue
            val x = match.groupValues[1].toIntOrNull() ?: continue
            val z = match.groupValues[2].toIntOrNull() ?: continue
            vertices += x to z
        }
        return vertices
    }

    private val IGNORED_PARAM_KEYS =
        setOf(
            "name",
            "mtype",
            "zoom",
            "align",
            "caption",
            "width",
            "height",
            "plane",
            "mapid",
            "title",
            "leagueregion",
            "type",
            "r",
            "x",
            "y",
            "group",
            "sandbox",
        )
}
