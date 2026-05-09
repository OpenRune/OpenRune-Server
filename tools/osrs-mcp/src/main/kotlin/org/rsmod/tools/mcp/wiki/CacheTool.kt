package org.rsmod.tools.mcp.wiki

import dev.openrune.OsrsCacheProvider
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.filesystem.Cache
import java.nio.file.Files
import java.nio.file.Path
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

enum class CacheKind {
    LIVE,
    SERVER,
    ;

    companion object {
        fun parse(value: String): CacheKind? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}

enum class CacheSearchType(val key: String) {
    Npc("npc"),
    Obj("obj"),
    Item("item"),
    Anim("anim"),
    Enum("enum"),
    Struct("struct"),
    HealthBar("healthbar"),
    HitSplat("hitsplat"),
    Varbit("varbit"),
    Varp("varp"),
    DbRow("dbrow"),
    DbTable("dbtable"),
    All("all"),
    ;

    companion object {
        fun parse(value: String): CacheSearchType? {
            val normalized = value.trim().lowercase()
            return entries.firstOrNull {
                it.key == normalized ||
                    (it == Obj && normalized == "object") ||
                    (it == Anim && (normalized == "sequence" || normalized == "seq")) ||
                    (it == DbRow && (normalized == "row" || normalized == "db_row")) ||
                    (it == DbTable && (normalized == "table" || normalized == "db_table"))
            }
        }
    }
}

class CacheTool {
    data class SearchHit(
        val type: String,
        val id: Int,
        val name: String,
        val summary: String,
        val data: String,
    )

    data class SearchResult(
        val cache: CacheKind,
        val totalMatches: Int,
        val matches: List<SearchHit>,
        val truncated: Boolean,
    )

    internal data class IndexedHit(
        val hit: SearchHit,
        val searchBlob: String,
    )

    internal data class Snapshot(
        val revision: Int,
        val byType: Map<CacheSearchType, List<IndexedHit>>,
    )

    internal val snapshots = ConcurrentHashMap<CacheKind, Snapshot>()

    fun search(cacheKind: CacheKind, type: CacheSearchType, query: String?, id: Int?, limit: Int): SearchResult {
        val root = resolveRoot(cacheKind)
        val revision = resolveRevision(root)
        val snapshot = snapshots.compute(cacheKind) { _, existing ->
            if (existing?.revision == revision) existing else buildSnapshot(cacheKind, root, revision)
        } ?: error("Failed to load cache snapshot.")

        return searchSnapshot(snapshot, cacheKind, type, query, id, limit)
    }

    internal fun searchSnapshot(
        snapshot: Snapshot,
        cacheKind: CacheKind,
        type: CacheSearchType,
        query: String?,
        id: Int?,
        limit: Int,
    ): SearchResult {
        val normalizedQuery = query?.trim()?.lowercase().orEmpty()
        val normalizedLimit = limit.coerceIn(1, 100)

        val pool =
            if (type == CacheSearchType.All) {
                snapshot.byType.values.flatten()
            } else {
                snapshot.byType[type].orEmpty()
            }

        val filtered =
            pool.filter { indexed ->
                val byId = id == null || indexed.hit.id == id
                val byQuery = normalizedQuery.isBlank() || indexed.searchBlob.contains(normalizedQuery)
                byId && byQuery
            }

        val sorted = filtered.sortedWith(compareBy<IndexedHit> { it.hit.type }.thenBy { it.hit.id })
        val sliced = sorted.take(normalizedLimit).map { it.hit }

        return SearchResult(
            cache = cacheKind,
            totalMatches = sorted.size,
            matches = sliced,
            truncated = sorted.size > sliced.size,
        )
    }

    private fun buildSnapshot(cacheKind: CacheKind, root: Path, revision: Int): Snapshot {
        val cachePath = root.resolve(".data").resolve("cache").resolve(cacheKind.name)
        require(Files.isDirectory(cachePath)) { "Unable to find cache directory at: $cachePath" }

        val cache = Cache.load(cachePath)
        try {
            val provider = OsrsCacheProvider(cache, revision)
            provider.init()

            val byType = mutableMapOf<CacheSearchType, List<IndexedHit>>()
            byType[CacheSearchType.Npc] = indexNpcs(provider.npcs)
            byType[CacheSearchType.Obj] = indexGeneric(CacheSearchType.Obj, provider.objects)
            byType[CacheSearchType.Item] = indexItems(provider.items)
            byType[CacheSearchType.Anim] = indexGeneric(CacheSearchType.Anim, provider.anims)
            byType[CacheSearchType.Enum] = indexGeneric(CacheSearchType.Enum, provider.enums)
            byType[CacheSearchType.Struct] = indexGeneric(CacheSearchType.Struct, provider.structs)
            byType[CacheSearchType.HealthBar] = indexGeneric(CacheSearchType.HealthBar, provider.healthBars)
            byType[CacheSearchType.HitSplat] = indexGeneric(CacheSearchType.HitSplat, provider.hitsplats)
            byType[CacheSearchType.Varbit] = indexGeneric(CacheSearchType.Varbit, provider.varbits)
            byType[CacheSearchType.Varp] = indexGeneric(CacheSearchType.Varp, provider.varps)
            byType[CacheSearchType.DbRow] = indexGeneric(CacheSearchType.DbRow, provider.dbrows)
            byType[CacheSearchType.DbTable] = indexGeneric(CacheSearchType.DbTable, provider.dbtables)

            return Snapshot(revision = revision, byType = byType)
        } finally {
            cache.close()
        }
    }

    private fun indexNpcs(values: Map<Int, NpcType>): List<IndexedHit> =
        values.map { (id, npc) ->
            val name = npc.name.ifBlank { "(unnamed npc)" }
            val summary = "combat=${npc.combatLevel}, size=${npc.size}, hp=${npc.hitpoints}"
            indexed(CacheSearchType.Npc, id, name, summary, describeValue(npc))
        }

    private fun indexItems(values: Map<Int, ItemType>): List<IndexedHit> =
        values.map { (id, item) ->
            val name = item.name.ifBlank { "(unnamed item)" }
            val summary = "cost=${item.cost}, stackable=${item.stackable}"
            indexed(CacheSearchType.Item, id, name, summary, describeValue(item))
        }

    private fun <T : Any> indexGeneric(type: CacheSearchType, values: Map<Int, T>): List<IndexedHit> =
        values.map { (id, value) ->
            val extractedName = runCatching { value.javaClass.getMethod("getName").invoke(value) as? String }.getOrNull()
            val name = extractedName?.ifBlank { null } ?: "(${type.key} $id)"
            indexed(type, id, name, value.toString(), describeValue(value))
        }

    internal fun indexed(type: CacheSearchType, id: Int, name: String, summary: String, data: String): IndexedHit {
        val hit = SearchHit(type = type.key, id = id, name = name, summary = summary, data = data)
        val searchBlob = "${hit.type} ${hit.id} ${hit.name} ${hit.summary}".lowercase()
        return IndexedHit(hit = hit, searchBlob = searchBlob)
    }

    private fun describeValue(value: Any): String {
        val fields = linkedMapOf<String, String>()
        var type: Class<*>? = value.javaClass
        while (type != null && type != Any::class.java) {
            for (field in type.declaredFields) {
                if (Modifier.isStatic(field.modifiers) || field.isSynthetic) {
                    continue
                }
                val extracted =
                    runCatching {
                        field.isAccessible = true
                        field.get(value)
                    }.getOrNull()
                fields.putIfAbsent(field.name, formatFieldValue(extracted))
            }
            type = type.superclass
        }
        if (fields.isEmpty()) {
            return value.toString()
        }
        return fields.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun formatFieldValue(value: Any?): String {
        if (value == null) {
            return "null"
        }
        return when (value) {
            is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
            is ByteArray -> value.joinToString(prefix = "[", postfix = "]")
            is ShortArray -> value.joinToString(prefix = "[", postfix = "]")
            is IntArray -> value.joinToString(prefix = "[", postfix = "]")
            is LongArray -> value.joinToString(prefix = "[", postfix = "]")
            is FloatArray -> value.joinToString(prefix = "[", postfix = "]")
            is DoubleArray -> value.joinToString(prefix = "[", postfix = "]")
            is CharArray -> value.joinToString(prefix = "[", postfix = "]")
            is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { formatScalar(it) }
            is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { formatScalar(it) }
            is Map<*, *> -> value.entries.joinToString(prefix = "{", postfix = "}") { "${formatScalar(it.key)}=${formatScalar(it.value)}" }
            else -> formatScalar(value)
        }
    }

    private fun formatScalar(value: Any?): String {
        if (value == null) {
            return "null"
        }
        return when (value) {
            is String -> value
            is Number, is Boolean, is Char -> value.toString()
            else -> value.toString()
        }
    }

    private fun resolveRevision(root: Path): Int {
        val gameFile = root.resolve("game.yml")
        require(Files.isRegularFile(gameFile)) { "Unable to find game.yml at: $gameFile" }

        val revisionLine =
            Files.readAllLines(gameFile)
                .firstOrNull { it.trimStart().startsWith("revision:") }
                ?: error("No revision line found in game.yml")

        val raw = revisionLine.substringAfter("revision:").trim().trim('"')
        val major = raw.substringBefore('.').trim()
        return major.toIntOrNull() ?: error("Invalid revision format in game.yml: '$raw'")
    }

    private fun resolveRoot(cacheKind: CacheKind): Path {
        val logDir = System.getenv("LOG_DIR")?.takeIf { it.isNotBlank() }
        if (logDir != null) {
            val parent = Path.of(logDir).toAbsolutePath().normalize().parent
            if (parent != null && hasCacheDir(parent, cacheKind)) {
                return parent
            }
        }

        val envRoot = System.getenv("RSPS_ROOT")?.takeIf { it.isNotBlank() }
        if (envRoot != null) {
            val envPath = Path.of(envRoot).toAbsolutePath().normalize()
            if (hasCacheDir(envPath, cacheKind)) {
                return envPath
            }
        }

        var cursor: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (cursor != null) {
            if (hasCacheDir(cursor, cacheKind)) {
                return cursor
            }
            cursor = cursor.parent
        }

        throw IllegalStateException(
            "Unable to locate repository root containing '.data/cache/${cacheKind.name}'. Set RSPS_ROOT.",
        )
    }

    private fun hasCacheDir(root: Path, cacheKind: CacheKind): Boolean =
        Files.isDirectory(root.resolve(".data").resolve("cache").resolve(cacheKind.name))
}
