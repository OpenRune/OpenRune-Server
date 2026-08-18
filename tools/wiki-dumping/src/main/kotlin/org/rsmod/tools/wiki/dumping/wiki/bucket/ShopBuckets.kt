package org.rsmod.tools.wiki.dumping.wiki.bucket

import org.rsmod.tools.wiki.dumping.wiki.ParsedStoreLine
import org.rsmod.tools.wiki.dumping.wiki.ParsedStoreTable
import org.rsmod.tools.wiki.dumping.wiki.ParsedWikiShopInfobox
import org.rsmod.tools.wiki.dumping.wiki.WikiShopStoreParser

/**
 * One row of the `infobox_item` bucket's per-version item data, e.g. `Waterskin(3)` /
 * `Antipoison(3)` / `Candle` each have one row per dose/lit-state/etc variant sharing a page.
 */
data class ItemVersion(
    val name: String,
    val id: Int,
    val versionAnchor: String?,
    val isDefault: Boolean,
)

/** Typed readers over the wiki's shop-related buckets, adapted to the existing parser types. */
class ShopBuckets(private val source: BucketSource) {

    suspend fun listShops(): List<ParsedWikiShopInfobox> =
        source.rows("infobox_shop", listOf("page_name", "shop_name"), cacheKey = "all-shops").map {
            row ->
            val page = row.requireText("page_name", "infobox_shop")
            val name = row.get("shop_name")?.takeIf { it.isTextual }?.asText().orEmpty()
            ParsedWikiShopInfobox(
                pageTitle = page,
                rsName = null,
                infoboxName = name.ifBlank { page },
            )
        }

    suspend fun storeTables(pageTitle: String): List<ParsedStoreTable> {
        val rows =
            source.rows(
                "storeline",
                listOf(
                    "sold_item",
                    "store_stock",
                    "restock_time",
                    "store_sell_multiplier",
                    "store_buy_multiplier",
                    "store_delta",
                    "store_currency",
                    "store_notes",
                ),
                where = "page_name" to pageTitle,
                cacheKey = pageTitle,
            )
        val grouped = LinkedHashMap<String, MutableList<ParsedStoreLine>>()
        val heads = mutableMapOf<String, Triple<Int?, Int?, Int?>>()
        for (row in rows) {
            val notes = row.get("store_notes")?.takeIf { it.isTextual }?.asText().orEmpty()
            val line =
                ParsedStoreLine(
                    name = row.requireText("sold_item", "storeline $pageTitle"),
                    stock = row.get("store_stock")?.asText()?.toIntOrNull() ?: 0,
                    restockCycles = row.get("restock_time")?.asText()?.toIntOrNull() ?: 100,
                )
            grouped.getOrPut(notes) { mutableListOf() }.add(line)
            heads.putIfAbsent(
                notes,
                Triple(
                    row.get("store_sell_multiplier")?.asText()?.toIntOrNull(),
                    row.get("store_buy_multiplier")?.asText()?.toIntOrNull(),
                    row.get("store_delta")?.asText()?.toIntOrNull(),
                ),
            )
        }
        return grouped.map { (notes, lines) ->
            val (sell, buy, delta) = heads.getValue(notes)
            ParsedStoreTable(
                sellMultiplier = sell,
                buyMultiplier = buy,
                delta = delta,
                nameNotes = notes.trim().removeSurrounding("(", ")").ifBlank { null },
                lines = lines,
            )
        }
    }

    /**
     * `selector` is the CSV `wiki_store` value, e.g. `blackjacks|Defensive` or `food|2 Subquests`.
     * Bucket `store_notes` text doesn't follow one convention across shops — verified live: it's
     * "Defensive blackjacks" (subkey before section, no comma) on Ali's Discount Wares, "food, 2
     * Subquests" (section prefix, comma) on Culinaromancer's Chest, and just "desert gear" (no
     * section at all) on the same Ali's Discount Wares page. So match in tiers: first require every
     * `|`-separated fragment to appear in the notes (disambiguates pages like Culinaromancer's
     * Chest that group a "food"-prefixed table and an unprefixed table under the same subkey text),
     * then fall back to an exact match on the last fragment alone (the common case with no section
     * prefix), then a fuzzy contains on it.
     */
    fun selectTable(tables: List<ParsedStoreTable>, selector: String?): ParsedStoreTable? {
        val normalized = selector?.let { WikiShopStoreParser.normalizeNameNotes(it) }
        if (normalized.isNullOrBlank() || normalized == "default") {
            // Mirrors the wikitext parser's blank-selector fallback: first table in document order,
            // regardless of namenotes — verified live against Baba Yaga's Magic Shop., where the
            // *first* (post-quest, namenotes-tagged) table is the correct default and a naive
            // "prefer the unnamed table" rule would wrongly pick the second, pre-quest one.
            return tables.firstOrNull { it.lines.isNotEmpty() }
        }
        val fragments =
            selector!!.split('|').map(WikiShopStoreParser::normalizeNameNotes).filter {
                it.isNotBlank()
            }
        fun notesOf(table: ParsedStoreTable) =
            WikiShopStoreParser.normalizeNameNotes(table.nameNotes.orEmpty())
        val target = fragments.last()
        return tables.firstOrNull {
            notesOf(it).isNotBlank() && fragments.all(notesOf(it)::contains)
        }
            ?: tables.firstOrNull { notesOf(it) == target }
            ?: tables.firstOrNull {
                notesOf(it).isNotBlank() &&
                    (notesOf(it).contains(target) || target.contains(notesOf(it)))
            }
    }

    suspend fun itemPageIds(pageTitle: String): List<Int> =
        source
            .rows("item_id", listOf("id"), where = "page_name" to pageTitle, cacheKey = pageTitle)
            .flatMap { row ->
                val ids = row.get("id") ?: return@flatMap emptyList<Int>()
                if (ids.isArray) ids.mapNotNull { it.asText().toIntOrNull() }
                else listOfNotNull(ids.asText().toIntOrNull())
            }

    /**
     * Per-version rows from `infobox_item` for a shared page — e.g. `Waterskin` has one row per
     * dose (`version_anchor` `"(0)"`..`"(4)"`), `Antipoison` one per dose (`version_anchor` `"1
     * dose"`..`"4 dose"`, note the different shape from Waterskin's), `Candle`/`Karambwan vessel`
     * one per lit/empty state (`version_anchor` `"Unlit"`/`"Lit"`, `"Empty"`/`"Baited"`).
     * `default_version` is a boolean field the Bucket API represents as key-present-with-empty-
     * string for true, key-absent for false (verified live) — [ItemVersion.isDefault] reflects
     * that. `item_id` is a repeated field like [itemPageIds]'s `id`.
     */
    suspend fun itemVersions(pageTitle: String): List<ItemVersion> =
        itemVersionRows("page_name" to pageTitle, cacheKey = "page:$pageTitle")

    /**
     * Same per-version rows as [itemVersions], queried by the exact per-version **item name**
     * instead of the page title — e.g. `Unlit torch`'s page is `Torch`, `Bronze spear(kp)`'s page
     * is `Bronze spear`, neither derivable from the display name by stripping a suffix. Verified
     * live this resolves those directly, and correctly filters out the odd non-game row
     * `infobox_item` carries for some names (`Candle`'s `item_name` match includes a `"Candles
     * (interface item)"` row whose `item_id` is the literal string `"interface8128"`, not a real
     * item id — [itemVersions]'s `toIntOrNull()` parse naturally drops it).
     */
    suspend fun itemVersionsByName(itemName: String): List<ItemVersion> =
        itemVersionRows("item_name" to itemName, cacheKey = "name:$itemName")

    private suspend fun itemVersionRows(
        where: Pair<String, String>,
        cacheKey: String,
    ): List<ItemVersion> =
        source
            .rows(
                "infobox_item",
                listOf("item_name", "item_id", "version_anchor", "default_version"),
                where = where,
                cacheKey = cacheKey,
            )
            .mapNotNull { row ->
                val ids = row.get("item_id") ?: return@mapNotNull null
                val id =
                    (if (ids.isArray) ids.firstOrNull() else ids)?.asText()?.toIntOrNull()
                        ?: return@mapNotNull null
                ItemVersion(
                    name = row.get("item_name")?.takeIf { it.isTextual }?.asText().orEmpty(),
                    id = id,
                    versionAnchor = row.get("version_anchor")?.takeIf { it.isTextual }?.asText(),
                    isDefault = row.has("default_version"),
                )
            }

    /**
     * Matches a parenthetical name suffix (e.g. `"4"` from `Waterskin(4)`, `"3"` from
     * `Antipoison(3)`) against [versions]' `version_anchor` text. Verified live the anchor shape
     * isn't consistent across pages — Waterskin's is `"(4)"` (parens around the digit),
     * Antipoison's is `"4 dose"` (leading word token, no parens) — so this strips surrounding
     * parens from the anchor and accepts either a full match or a match on the anchor's first
     * whitespace-separated token, case-insensitively. Returns null unless exactly one *distinct id*
     * satisfies it — an ambiguous or absent match is never guessed at.
     */
    fun matchVersionBySuffix(versions: List<ItemVersion>, suffix: String): ItemVersion? {
        val target = suffix.trim().lowercase()
        val matches =
            versions.filter { version ->
                val anchor =
                    version.versionAnchor?.trim()?.removeSurrounding("(", ")")?.trim()?.lowercase()
                        ?: return@filter false
                anchor == target || anchor.substringBefore(' ') == target
            }
        return if (matches.map { it.id }.distinct().size == 1) matches.first() else null
    }

    /**
     * Picks the `default_version` row among [versions] (a suffixless name that hit a multi-version
     * page, e.g. `Candle` -> `Unlit`/`Lit`, `Karambwan vessel` -> `Empty`/`Baited`) — but only when
     * exactly one distinct id is marked default; ambiguity is left unresolved rather than guessed.
     */
    fun defaultVersion(versions: List<ItemVersion>): ItemVersion? {
        val defaults = versions.filter { it.isDefault }
        return if (defaults.map { it.id }.distinct().size == 1) defaults.first() else null
    }
}
