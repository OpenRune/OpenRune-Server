package org.rsmod.tools.mcp.wiki

import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCMType
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class GameValTool private constructor(private val entries: List<GameValEntry>) {
    data class GameValEntry(
        val table: String,
        val key: String,
        val fullKey: String,
        val id: Int,
        val source: String,
    )

    data class SearchResult(
        val totalMatches: Int,
        val matches: List<GameValEntry>,
        val truncated: Boolean,
    )

    fun search(query: String?, table: String?, id: Int?, limit: Int): SearchResult {
        val normalizedQuery = query?.trim()?.lowercase().orEmpty()
        val normalizedTable = table?.trim()?.lowercase().orEmpty()

        if (normalizedTable.isNotBlank() && normalizedTable !in RSCMType.RSCM_PREFIXES) {
            val allowed = RSCMType.entries.joinToString(", ") { it.prefix }
            throw IllegalArgumentException(
                "Unknown 'table' prefix '${table?.trim()}'. Valid prefixes: $allowed",
            )
        }

        val filteredByTable =
            if (normalizedTable.isBlank()) {
                entries
            } else {
                entries.filter { it.table.equals(normalizedTable, ignoreCase = true) }
            }

        val filteredById =
            if (id == null) {
                filteredByTable
            } else {
                filteredByTable.filter { it.id == id }
            }

        val scored =
            if (normalizedQuery.isBlank()) {
                filteredById.map { ScoredEntry(score = 100, entry = it) }
            } else {
                filteredById.mapNotNull { entry ->
                    val full = entry.fullKey.lowercase()
                    val key = entry.key.lowercase()
                    val score =
                        when {
                            full == normalizedQuery -> 0
                            key == normalizedQuery -> 1
                            full.startsWith(normalizedQuery) -> 2
                            key.startsWith(normalizedQuery) -> 3
                            full.contains(normalizedQuery) -> 4
                            key.contains(normalizedQuery) -> 5
                            else -> Int.MAX_VALUE
                        }
                    if (score == Int.MAX_VALUE) null else ScoredEntry(score = score, entry = entry)
                }
            }

        val sorted = scored.sortedWith(compareBy<ScoredEntry> { it.score }.thenBy { it.entry.fullKey })
        val limited = sorted.take(limit.coerceAtLeast(1)).map { it.entry }
        return SearchResult(
            totalMatches = sorted.size,
            matches = limited,
            truncated = sorted.size > limited.size,
        )
    }

    fun totalMappingEntries(): Int = entries.size

    fun reverseLookupNpc(id: Int): List<String> =
        entries
            .filter { it.table.equals("npc", ignoreCase = true) && it.id == id }
            .map { it.fullKey }
            .distinct()

    private data class ScoredEntry(val score: Int, val entry: GameValEntry)

    companion object {
        fun load(rootDir: String? = null): GameValTool {
            val root = resolveRoot(rootDir)
            val prefix = rootDirPrefix(root)
            val provider = GameValProvider.loadIsolated(rootDir = prefix, autoAssignIds = false)
            return fromProvider(provider)
        }

        private fun fromProvider(provider: GameValProvider): GameValTool {
            val entries =
                provider.mappings.flatMap { (table, tableMap) ->
                    tableMap.map { (fullKey, id) ->
                        val key =
                            if (fullKey.startsWith("$table.")) {
                                fullKey.removePrefix("$table.")
                            } else {
                                fullKey
                            }
                        GameValEntry(
                            table = table,
                            key = key,
                            fullKey = fullKey,
                            id = id,
                            source = "merged",
                        )
                    }
                }.sortedBy { it.fullKey }
            return GameValTool(entries)
        }

        private fun rootDirPrefix(root: Path): String {
            val s = root.toAbsolutePath().normalize().toString()
            val sep = File.separator
            return if (s.endsWith(sep)) s else s + sep
        }

        private fun resolveRoot(rootDir: String?): Path {
            if (!rootDir.isNullOrBlank()) {
                return Path.of(rootDir).toAbsolutePath().normalize()
            }

            val logDir = System.getenv("LOG_DIR")?.takeIf { it.isNotBlank() }
            if (logDir != null) {
                val parent = Path.of(logDir).toAbsolutePath().normalize().parent
                if (
                    parent != null &&
                    Files.isRegularFile(parent.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat"))
                ) {
                    return parent
                }
            }

            val envRoot = System.getenv("RSPS_ROOT")?.takeIf { it.isNotBlank() }
            if (envRoot != null) {
                val envPath = Path.of(envRoot).toAbsolutePath().normalize()
                if (Files.isRegularFile(envPath.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat"))) {
                    return envPath
                }
            }

            val classpathRoots = guessRootsFromClasspath()
            for (candidateRoot in classpathRoots) {
                val candidate = candidateRoot.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat")
                if (Files.isRegularFile(candidate)) {
                    return candidateRoot
                }
            }

            var cursor: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
            while (cursor != null) {
                val candidate = cursor.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat")
                if (Files.isRegularFile(candidate)) {
                    return cursor
                }
                cursor = cursor.parent
            }
            throw IllegalStateException(
                "Unable to locate repository root containing '.data/gamevals-binary/gamevals.dat'. " +
                    "Set RSPS_ROOT or pass 'rootDir'.",
            )
        }

        private fun guessRootsFromClasspath(): List<Path> {
            val separator = System.getProperty("path.separator")
            val classpath = System.getProperty("java.class.path").orEmpty()
            if (classpath.isBlank()) {
                return emptyList()
            }

            val roots = linkedSetOf<Path>()
            for (entry in classpath.split(separator)) {
                val path = runCatching { Path.of(entry).toAbsolutePath().normalize() }.getOrNull() ?: continue
                if (!Files.exists(path)) {
                    continue
                }
                var cursor: Path? = if (Files.isRegularFile(path)) path.parent else path
                repeat(8) {
                    val current = cursor ?: return@repeat
                    roots.add(current)
                    cursor = current.parent
                }
            }
            return roots.toList()
        }
    }
}
