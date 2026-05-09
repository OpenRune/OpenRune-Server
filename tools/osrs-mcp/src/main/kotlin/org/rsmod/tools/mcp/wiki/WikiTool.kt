package org.rsmod.tools.mcp.wiki

class WikiTool(
    private val wikiProvider: () -> WikiClient,
    private val gameValToolProvider: () -> GameValTool = { GameValTool.load() },
    private val cacheTool: CacheTool = CacheTool(),
) {
    private val wiki: WikiClient by lazy(wikiProvider)

    @Volatile private var gameValsTool: GameValTool? = null
    private val gameValsLock = Any()

    private fun gameVals(): GameValTool {
        gameValsTool?.let { return it }
        synchronized(gameValsLock) {
            gameValsTool?.let { return it }
            return gameValToolProvider().also { gameValsTool = it }
        }
    }

    fun reloadGamevalsFromDisk(): String {
        val fresh = gameValToolProvider()
        synchronized(gameValsLock) {
            gameValsTool = fresh
        }
        return "Reloaded gamevals from disk (${fresh.totalMappingEntries()} mapping rows). " +
            "Use after editing .rscm, content gamevals.toml, or binary dats; no OpenRune cache build required for those files."
    }

    fun reloadCacheSnapshotsFromDisk(): String {
        cacheTool.clearSnapshots()
        return "Cleared in-memory cache snapshots. Next cache_search will re-index from disk."
    }

    fun reloadAllLocalData(): String =
        reloadGamevalsFromDisk() + "\n" + reloadCacheSnapshotsFromDisk()

    companion object {
        private const val DATA_TRUNCATE_CHARS = 800
    }

    private data class SpawnCoord(val x: Int, val y: Int)

    private data class LocEntry(
        val name: String,
        val location: String,
        val mapId: String?,
        val plane: String?,
        val members: String?,
        val levels: String?,
        val coords: List<SpawnCoord>,
    )

    suspend fun wikiSearch(query: String, limit: Int): String {
        val hits = wiki.search(query, limit)
        if (hits.isEmpty()) {
            return "No wiki matches found for '$query'."
        }

        return buildString {
            appendLine("Found ${hits.size} results for '$query':")
            hits.forEachIndexed { index, hit ->
                appendLine("${index + 1}. ${hit.title}")
                appendLine("   URL: ${wiki.wikiUrlForTitle(hit.title)}")
                if (hit.snippet.isNotBlank()) {
                    appendLine("   Snippet: ${hit.snippet}")
                }
            }
        }.trimEnd()
    }

    suspend fun wikiPage(title: String, maxChars: Int): String {
        val page = wiki.page(title, maxChars)
        return buildString {
            appendLine("Title: ${page.title}")
            appendLine("URL: ${page.url}")
            appendLine()
            append(page.text)
        }.trimEnd()
    }

    suspend fun wikiNpcSpawns(title: String, npcName: String?, location: String?): String {
        val source = wiki.rawPageSource(title)
        val infoboxSection = formatInfoboxNpcIdsResolved(source)
        val allEntries = parseLocEntries(source)
        if (allEntries.isEmpty()) {
            return buildString {
                append("No {{LocLine}} entries found on '$title'.")
                if (infoboxSection.isNotBlank()) {
                    appendLine()
                    append(infoboxSection)
                }
            }.trimEnd()
        }

        val byNpc =
            if (npcName.isNullOrBlank()) {
                allEntries
            } else {
                allEntries.filter { it.name.equals(npcName, ignoreCase = true) }
            }

        val filtered =
            if (location.isNullOrBlank()) {
                byNpc
            } else {
                byNpc.filter { it.location.contains(location, ignoreCase = true) }
            }

        if (filtered.isEmpty()) {
            return buildString {
                append("No spawn entries matched")
                if (!npcName.isNullOrBlank()) append(" npc='$npcName'")
                if (!location.isNullOrBlank()) append(" location='$location'")
                append(" on '$title'.")
                if (infoboxSection.isNotBlank()) {
                    appendLine()
                    append(infoboxSection)
                }
            }.trimEnd()
        }

        return buildString {
            appendLine("Found ${filtered.size} spawn entries on '$title':")
            filtered.forEachIndexed { index, entry ->
                appendLine("${index + 1}. ${entry.name}")
                appendLine("   Location: ${entry.location}")
                entry.levels?.let { appendLine("   Levels: $it") }
                entry.members?.let { appendLine("   Members: $it") }
                entry.mapId?.let { appendLine("   Map ID: $it") }
                entry.plane?.let { appendLine("   Plane: $it") }
                appendLine("   Spawn count: ${entry.coords.size}")
                appendLine(
                    "   Coordinates: ${entry.coords.joinToString("|") { "x:${it.x},y:${it.y}" }}",
                )
            }
            if (infoboxSection.isNotBlank()) {
                appendLine()
                append(infoboxSection)
            }
        }.trimEnd()
    }

    private fun formatInfoboxNpcIdsResolved(source: String): String {
        val lines = WikiInfoboxNpcIds.parseMonsterIdLines(source)
        if (lines.isEmpty()) {
            return ""
        }
        return buildString {
            appendLine("== Infobox NPC IDs (resolved via loaded gamevals) ==")
            for (line in lines) {
                appendLine("${line.label}:")
                for (id in line.npcIds) {
                    val keys = gameVals().reverseLookupNpc(id)
                    if (keys.isEmpty()) {
                        appendLine("  - id $id -> (no npc.* mapping in loaded gamevals)")
                    } else {
                        appendLine("  - id $id -> ${keys.joinToString(" | ")}")
                    }
                }
            }
        }.trimEnd()
    }

    fun gamevalSearch(query: String?, table: String?, id: Int?, limit: Int): String {
        val normalizedQuery = query?.trim().orEmpty().ifBlank { null }
        val normalizedTable = table?.trim().orEmpty().ifBlank { null }
        val result = gameVals().search(query = normalizedQuery, table = normalizedTable, id = id, limit = limit)
        if (result.totalMatches == 0) {
            return buildString {
                append("No gameval entries matched")
                if (normalizedQuery != null) append(" query='$normalizedQuery'")
                if (normalizedTable != null) append(" table='$normalizedTable'")
                if (id != null) append(" id=$id")
                append(".")
            }
        }

        if (result.totalMatches == 1) {
            val match = result.matches.first()
            return buildString {
                appendLine("Found 1 gameval entry:")
                appendLine("Key: ${match.fullKey}")
                appendLine("ID: ${match.id}")
                append("Source: ${match.source}")
            }
        }

        return buildString {
            appendLine(
                "Found ${result.totalMatches} gameval matches; showing ${result.matches.size}.",
            )
            result.matches.forEachIndexed { index, match ->
                appendLine("${index + 1}. ${match.fullKey} = ${match.id} (${match.source})")
            }
            if (result.truncated) {
                appendLine("Results truncated. Increase 'limit' to see more.")
            }
            append(
                "If you want one specific entry, rerun with an exact key (for example query=\"${result.matches.first().fullKey}\") or with id=<number>.",
            )
        }.trimEnd()
    }

    fun cacheSearch(cache: CacheKind, type: CacheSearchType, query: String?, id: Int?, limit: Int): String {
        val normalizedQuery = query?.trim().orEmpty().ifBlank { null }
        val result = cacheTool.search(cacheKind = cache, type = type, query = normalizedQuery, id = id, limit = limit)
        if (result.totalMatches == 0) {
            return buildString {
                append("No cache entries matched")
                append(" cache='${cache.name}'")
                append(" type='${type.key}'")
                if (normalizedQuery != null) append(" query='$normalizedQuery'")
                if (id != null) append(" id=$id")
                append('.')
            }
        }

        return buildString {
            appendLine("Cache: ${result.cache.name}")
            appendLine("Found ${result.totalMatches} cache matches; showing ${result.matches.size}.")
            result.matches.forEachIndexed { index, hit ->
                appendLine("${index + 1}. [${hit.type}] ${hit.id} - ${hit.name}")
                appendLine("   ${hit.summary}")
                val data = if (hit.data.length <= DATA_TRUNCATE_CHARS) {
                    hit.data
                } else {
                    hit.data.take(DATA_TRUNCATE_CHARS) + "... (truncated; search by id for full data)"
                }
                appendLine("   data: $data")
            }
            if (result.truncated) {
                appendLine("Results truncated. Increase 'limit' to see more.")
            }
            if (normalizedQuery != null || id != null) {
                append("Rerun with narrower filters for a smaller result set.")
            }
        }.trimEnd()
    }

    private fun parseLocEntries(source: String): List<LocEntry> {
        val blockRegex = Regex("\\{\\{LocLine\\b(.*?)\\}\\}", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        return blockRegex
            .findAll(source)
            .mapNotNull { parseLocEntry(it.groupValues[1]) }
            .toList()
    }

    private fun parseLocEntry(block: String): LocEntry? {
        val coords =
            Regex("x:(\\d+),y:(\\d+)", RegexOption.IGNORE_CASE)
                .findAll(block)
                .mapNotNull {
                    val x = it.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                    val y = it.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                    SpawnCoord(x, y)
                }
                .toList()

        val name = readField(block, "name") ?: return null
        val location = readField(block, "location") ?: "Unknown"

        return LocEntry(
            name = sanitizeWikiMarkup(name),
            location = sanitizeWikiMarkup(location),
            mapId = readField(block, "mapID"),
            plane = readField(block, "plane"),
            members = readField(block, "members"),
            levels = readField(block, "levels"),
            coords = coords,
        )
    }

    private fun readField(block: String, key: String): String? {
        val regex = Regex("\\|\\s*${Regex.escape(key)}\\s*=\\s*([^\\n\\r]*)", RegexOption.IGNORE_CASE)
        val value = regex.find(block)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        return value.takeIf { it.isNotBlank() }
    }

    private fun sanitizeWikiMarkup(input: String): String =
        input
            .replace(Regex("\\[\\[([^|\\]]+)\\|([^\\]]+)\\]\\]"), "$2")
            .replace(Regex("\\[\\[([^\\]]+)\\]\\]"), "$1")
            .trim()
}
