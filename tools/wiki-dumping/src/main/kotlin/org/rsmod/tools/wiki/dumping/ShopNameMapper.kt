package org.rsmod.tools.wiki.dumping

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import org.rsmod.tools.wiki.dumping.wiki.ParsedWikiShopInfobox
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiDumpStorePages
import org.rsmod.tools.wiki.dumping.wiki.WikiShopInfoboxParser

data class ShopMappingEntry(
    val id: Int,
    val slug: String,
    /** When set, used instead of auto-matching against wiki shop names. */
    val wikiArticle: String? = null,
    /**
     * Selects a store table within a multi-table wiki page.
     * Format: `section|namenotes` — e.g. `food|0 Subquests`, `items|full`.
     * Section is the wiki `==Stock==` subsection (`food` / `items`).
     * Namenotes matches `{{StoreTableHead|namenotes=(...)}}` (parens omitted).
     */
    val wikiStore: String? = null,
)

object ShopNameMapper {
    private const val DEFAULT_CSV_PATH = "tools/wiki-dumping/src/main/resources/shopmappings.csv"

    @JvmStatic
    fun main(args: Array<String>) {
        exportCsv(args)
    }

    private fun exportCsv(args: Array<String>) {
        val quiet = args.contains("--quiet")
        val inputPath = args.firstOrNull { it.startsWith("--input=") }?.substringAfter('=')
        val outputPath =
            args.firstOrNull { it.startsWith("--output=") }?.substringAfter('=')
                ?: inputPath
                ?: DEFAULT_CSV_PATH

        GameValLoader.ensureLoaded()

        val csvInput = resolvePath(inputPath ?: DEFAULT_CSV_PATH)
        val mappings = loadMappingsFromCsv(csvInput)
        if (mappings.isEmpty()) {
            System.err.println("No shop mappings found in $csvInput")
            exitProcess(1)
        }

        val wikiShops =
            WikiClient.open().use { wiki ->
                WikiDumpStorePages
                    .listShopInfoboxPages(wiki.wikiDumpStore())
                    .sortedBy { it.pageTitle }
            }
        val index = buildNameIndex(wikiShops)

        val mappedRows =
            mappings.map { entry ->
                val article =
                    entry.wikiArticle?.let(::stripWikiBrackets)?.takeIf { it.isNotBlank() }
                        ?: resolveMapping(entry, index)?.pageTitle
                toCsvRow(entry, article)
            }
        val mappedArticles =
            mappedRows
                .mapNotNull { row -> stripWikiBrackets(row.wikiArticle).takeIf { it.isNotBlank() }?.let(::normalizeShopKey) }
                .toSet()

        val wikiOnlyRows =
            wikiShops
                .filter { shop -> normalizeShopKey(shop.pageTitle) !in mappedArticles }
                .map { shop -> toWikiOnlyCsvRow(shop) }

        val rows = mappedRows + wikiOnlyRows
        val csvOutput = resolvePath(outputPath)
        writeCsv(csvOutput, rows)

        if (!quiet) {
            rows.forEach(::println)
        }
        val resolvedWiki = mappedRows.count { it.wikiArticle.isNotBlank() }
        println()
        println(
            "Wrote ${rows.size} row(s) (${mappedRows.size} mapped, ${wikiOnlyRows.size} wiki-only, " +
                "$resolvedWiki wiki matches) -> $csvOutput",
        )
    }

    data class ShopCsvEntry(
        val inv: String,
        val slug: String,
        val wikiArticle: String,
        val wikiStore: String = "",
    )

    fun loadDumpableRowsFromCsv(csvPath: java.nio.file.Path): List<ShopCsvEntry> =
        Files
            .readAllLines(csvPath)
            .asSequence()
            .dropWhile { it.startsWith("#") || it.startsWith("id,") || it.startsWith("inv,") }
            .mapNotNull { line -> parseCsvEntry(line) }
            .filter { it.inv.isNotBlank() && it.inv != "-" }
            .distinctBy { it.inv }
            .sortedBy { it.inv }
            .toList()

    private fun parseCsvEntry(line: String): ShopCsvEntry? {
        val fields = parseCsvFields(line)
        if (fields.size < 3) {
            return null
        }

        val legacyId = fields[0].toIntOrNull()
        return if (legacyId != null && legacyId >= 0) {
            ShopCsvEntry(
                inv = fields.getOrNull(1).orEmpty(),
                slug = fields[2],
                wikiArticle = fields.getOrNull(3).orEmpty(),
                wikiStore = fields.getOrNull(4).orEmpty(),
            )
        } else {
            ShopCsvEntry(
                inv = fields[0],
                slug = fields[1],
                wikiArticle = fields.getOrNull(2).orEmpty(),
                wikiStore = fields.getOrNull(3).orEmpty(),
            )
        }
    }

    fun loadMappingsFromCsv(csvPath: java.nio.file.Path): List<ShopMappingEntry> =
        Files
            .readAllLines(csvPath)
            .asSequence()
            .dropWhile { it.startsWith("#") || it.startsWith("id,") || it.startsWith("inv,") }
            .mapNotNull { line -> parseMappingRow(line) }
            .distinctBy { it.slug }
            .sortedBy { it.id }
            .toList()

    private fun parseMappingRow(line: String): ShopMappingEntry? {
        val fields = parseCsvFields(line)
        if (fields.size < 3) {
            return null
        }

        val legacyId = fields[0].toIntOrNull()
        val (inv, slug, wikiArticle, wikiStore) =
            if (legacyId != null && legacyId >= 0) {
                ParsedMappingRow(
                    inv = fields.getOrNull(1).orEmpty(),
                    slug = fields[2],
                    wikiArticle = fields.getOrNull(3),
                    wikiStore = fields.getOrNull(4),
                )
            } else {
                if (fields[0] == "-") {
                    return null
                }
                ParsedMappingRow(
                    inv = fields[0],
                    slug = fields[1],
                    wikiArticle = fields.getOrNull(2),
                    wikiStore = fields.getOrNull(3),
                )
            }

        val id =
            when {
                legacyId != null && legacyId >= 0 -> legacyId
                inv.isNotBlank() && inv != "-" -> resolveIdFromInv(inv)
                else -> resolveIdFromSlug(slug)
            } ?: return null

        return ShopMappingEntry(
            id = id,
            slug = slug,
            wikiArticle = wikiArticle?.trim()?.takeIf { it.isNotBlank() },
            wikiStore = wikiStore?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private data class ParsedMappingRow(
        val inv: String,
        val slug: String,
        val wikiArticle: String?,
        val wikiStore: String?,
    )

    private fun resolveIdFromInv(inv: String): Int? =
        runCatching { RSCM.getRSCM("${RSCMType.INV.prefix}.${inv.removePrefix("inv.")}") }
            .getOrNull()

    private fun resolveIdFromSlug(slug: String): Int? =
        runCatching { RSCM.getRSCM("${RSCMType.INV.prefix}.${slug.removePrefix("inv.")}") }
            .getOrNull()

    fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 2
                    continue
                }
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    fields += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        fields += current.toString()
        return fields
    }

    fun normalizeShopKey(name: String): String =
        name
            .lowercase()
            .replace('’', '\'')
            .replace(Regex("['`]"), "")
            .replace(Regex("[^a-z0-9]+"), "")

    fun buildNameIndex(shops: List<ParsedWikiShopInfobox>): Map<String, List<ParsedWikiShopInfobox>> {
        val index = mutableMapOf<String, MutableList<ParsedWikiShopInfobox>>()

        fun add(key: String, shop: ParsedWikiShopInfobox) {
            if (key.isBlank()) {
                return
            }
            index.getOrPut(key) { mutableListOf() }.add(shop)
        }

        for (shop in shops) {
            shop.rsName?.let { add(normalizeShopKey(it), shop) }
            add(normalizeShopKey(shop.infoboxName), shop)
            add(normalizeShopKey(shop.pageTitle), shop)
            add(normalizeShopKey(shop.slugFromTitle()), shop)
        }

        return index.mapValues { (_, entries) -> entries.distinctBy { it.pageTitle } }
    }

    fun resolveMapping(
        entry: ShopMappingEntry,
        index: Map<String, List<ParsedWikiShopInfobox>>,
    ): ParsedWikiShopInfobox? {
        val slugKey = normalizeShopKey(entry.slug.replace('_', ' '))
        val candidates = index[slugKey].orEmpty()

        return when {
            candidates.isEmpty() -> null
            candidates.size == 1 -> candidates.first()
            else ->
                candidates.firstOrNull { normalizeShopKey(it.pageTitle) == slugKey }
                    ?: candidates.firstOrNull { normalizeShopKey(it.slugFromTitle()) == slugKey }
                    ?: candidates.first()
        }
    }

    data class ShopCsvRow(
        val id: Int?,
        val inv: String,
        val slug: String,
        val wikiArticle: String,
        val wikiStore: String = "",
    ) {
        fun toCsvLine(): String =
            listOf(inv, slug, wikiArticle, wikiStore).joinToString(",") { csvEscape(it) }

        override fun toString(): String = toCsvLine()
    }

    fun resolveInvKey(id: Int, slug: String): String {
        val fullKey = resolveInvFullKey(id, slug)
        if (fullKey.isBlank()) {
            return "-"
        }
        return fullKey.removePrefix("${RSCMType.INV.prefix}.")
    }

    fun resolveInvKeyBySlug(slug: String): String {
        val slugKey = slug.removePrefix("inv.")
        val candidate = "${RSCMType.INV.prefix}.$slugKey"
        if (hasInvMapping(candidate)) {
            return slugKey
        }
        return "-"
    }

    private fun resolveInvFullKey(id: Int, slug: String): String {
        val reversed =
            runCatching { RSCM.getReverseMapping(RSCMType.INV, id) }
                .getOrNull()
                ?.trim()
                .orEmpty()
        if (reversed.isNotBlank() && reversed != "-1") {
            return if (reversed.contains('.')) reversed else "${RSCMType.INV.prefix}.$reversed"
        }

        val slugKey = slug.removePrefix("inv.")
        val candidate = "${RSCMType.INV.prefix}.$slugKey"
        if (hasInvMapping(candidate)) {
            return candidate
        }
        return ""
    }

    fun formatWikiArticle(article: String?): String {
        val trimmed = article?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return ""
        }
        return if (trimmed.startsWith("[[")) trimmed else "[[$trimmed]]"
    }

    fun stripWikiBrackets(article: String): String =
        article.trim().removePrefix("[[").removeSuffix("]]").trim()

    fun toCsvRow(entry: ShopMappingEntry, article: String?): ShopCsvRow =
        ShopCsvRow(
            id = null,
            inv = resolveInvKey(entry.id, entry.slug),
            slug = entry.slug,
            wikiArticle = formatWikiArticle(article),
            wikiStore = entry.wikiStore.orEmpty(),
        )

    fun toWikiOnlyCsvRow(shop: ParsedWikiShopInfobox): ShopCsvRow {
        val slug = WikiShopInfoboxParser.wikiSlugFromTitle(shop.pageTitle)
        return ShopCsvRow(
            id = null,
            inv = resolveInvKeyBySlug(slug),
            slug = slug,
            wikiArticle = formatWikiArticle(shop.pageTitle),
        )
    }

    fun writeCsv(outputPath: java.nio.file.Path, rows: List<ShopCsvRow>) {
        outputPath.parent?.createDirectories()
        val lines = buildList {
            add("inv,slug,wiki_article,wiki_store")
            rows.forEach { add(it.toCsvLine()) }
        }
        outputPath.writeText(lines.joinToString("\n") + "\n")
    }

    fun csvEscape(value: String): String {
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    private fun resolvePath(path: String): java.nio.file.Path {
        val parsed = Path(path)
        return if (parsed.isAbsolute) {
            parsed
        } else {
            GameValLoader.resolveRoot().resolve(parsed)
        }
    }

    private fun hasInvMapping(fullKey: String): Boolean =
        runCatching {
            RSCM.getRSCM(fullKey)
            true
        }.getOrDefault(false)
}
