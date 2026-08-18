package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.ParsedStoreLine
import org.rsmod.tools.wiki.dumping.wiki.ParsedStoreTable
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiShopInfoboxParser
import org.rsmod.tools.wiki.dumping.wiki.WikiShopStoreParser
import org.rsmod.tools.wiki.dumping.wiki.bucket.BucketSource
import org.rsmod.tools.wiki.dumping.wiki.bucket.ItemVersion
import org.rsmod.tools.wiki.dumping.wiki.bucket.ShopBuckets

private const val DEFAULT_MAPPINGS_RELATIVE =
    "tools/wiki-dumping/src/main/resources/shopmappings.csv"
private const val DEFAULT_OUTPUT_RELATIVE = ".data/raw-cache/server/shops"

/**
 * `inv`s that bucket mode must skip outright rather than write wrong or partial stock for — each
 * reason verified live 2026-08-17 against a full-corpus regeneration. These are structural gaps in
 * what the wiki's Bucket API exposes, not matching bugs: no amount of selector/name-matching logic
 * can recover the data because the bucket has zero (or ambiguous, indistinguishable) rows for it.
 */
private val BUCKET_UNSERVABLE_REASONS: Map<String, String> =
    mapOf(
        // bucket=No opt-out: the page's real {{StoreTableHead}} sets `bucket=No`, so the
        // storeline bucket has zero rows for it under any query shape.
        "aldarin_general_store" to "bucket=No opt-out on the page's StoreTableHead",
        "darkruneshop_crap" to "bucket=No opt-out on the page's StoreTableHead",
        "darkruneshop_uber" to "bucket=No opt-out on the page's StoreTableHead",
        "mcannonshop" to
            "bucket=No opt-out on the real cannon-parts table; only an unrelated hidden " +
                "repair/buy-back table remains bucket-visible",
        // Page renamed on the wiki (redirect): bucket's exact-match `where('page_name', ...)`
        // can't follow it — dump-mode's live page fetch resolves the redirect, buckets can't.
        // Follow-up: this one is CSV-fixable — drop the trailing period from magicshop's
        // wikiArticle ("Betty's Magic Emporium.") so it matches the page's current, post-rename
        // title ("Betty's Magic Emporium") directly instead of relying on redirect resolution.
        "magicshop" to
            "page renamed on wiki (redirect); bucket can't follow it, CSV article is stale",
        // Stock documented as a {{plinkt}} wikitable, not {{StoreLine}} — invisible to the
        // storeline bucket, which only ever sees StoreLine template invocations.
        "hunting_customfurshop" to
            "stock is a {{plinkt}} wikitable, not {{StoreLine}} — not in the bucket",
        // Two StoreTableHead blocks on the same page share identical (blank) store_notes, so the
        // storeline bucket's group-by-store_notes collapses them into one indistinguishable group.
        "tzhaar_shop_rune" to
            "two unnamed StoreTableHead tables collapse into one group in the bucket",
        // CSV wiki_store selector text has no equivalent in the live page's store_notes.
        "sophanem_cloth_store_updated" to
            "CSV wiki_store selector text absent from the live page's store_notes",
    )

data class ResolvedShopStock(val objKey: String, val count: Int, val restockCycles: Int)

data class ShopDumpResult(
    val inv: String,
    val wikiTitle: String,
    val outputFile: Path?,
    val stock: List<ResolvedShopStock>,
    val unresolvedItems: List<String>,
    val skippedReason: String? = null,
    val table: ParsedStoreTable? = null,
    val shopName: String? = null,
)

class ShopWikiDumper(
    private val wiki: WikiClient?,
    private val objLookup: ObjRscmLookup,
    private val itemLookup: ItemWikiLookup?,
    private val log: DropDumpLog,
    private val buckets: ShopBuckets? = null,
    /**
     * Wiki article titles (stripped, as returned by [ShopNameMapper.stripWikiBrackets]) that also
     * have a `_skillcape`/`_skillcape_trimmed` CSV row pointing at them. Bucket mode needs this to
     * exclude skill-cape lines from a *base* (non-cape) row's table — see [dumpShop].
     */
    private val capeSiblingArticles: Set<String> = emptySet(),
) {
    private var shopNamesByTitleCache: Map<String, String>? = null

    private suspend fun shopNamesByTitle(): Map<String, String> {
        shopNamesByTitleCache?.let {
            return it
        }
        val map =
            checkNotNull(buckets) { "bucket mode requires ShopBuckets" }
                .listShops()
                .associate { it.pageTitle to it.infoboxName }
        shopNamesByTitleCache = map
        return map
    }

    suspend fun dumpShop(row: ShopNameMapper.ShopCsvEntry): ShopDumpResult {
        val resolvedRow = ShopSpecialHandlers.resolveRow(row)
        val wikiTitle = ShopNameMapper.stripWikiBrackets(resolvedRow.wikiArticle)
        if (wikiTitle.isBlank()) {
            return skipped(resolvedRow, "no wiki article")
        }
        if (buckets != null) {
            BUCKET_UNSERVABLE_REASONS[resolvedRow.inv]?.let { reason ->
                return skipped(resolvedRow, "bucket source can't serve this shop: $reason")
            }
        }

        val shopName: String?
        val table: ParsedStoreTable?

        if (buckets != null) {
            shopName =
                ShopSpecialHandlers.resolveShopDisplayName(
                    resolvedRow.inv,
                    shopNamesByTitle()[wikiTitle],
                )
            val tables = buckets.storeTables(wikiTitle)
            table =
                WikiShopStoreParser.skillcapeTrimmed(resolvedRow.inv)?.let { trimmed ->
                    selectSkillcapeFromBuckets(tables, trimmed)
                }
                    ?: buckets
                        .selectTable(tables, resolvedRow.wikiStore.takeIf { it.isNotBlank() })
                        ?.let { selected -> excludeCapeSiblingLines(wikiTitle, selected) }
        } else {
            val wikiClient = checkNotNull(wiki) { "dump mode requires a WikiClient" }
            val source = runCatching { wikiClient.rawPageSource(wikiTitle) }.getOrNull()
            if (source.isNullOrBlank()) {
                return skipped(resolvedRow, "wiki page not found: $wikiTitle")
            }

            shopName =
                ShopSpecialHandlers.resolveShopDisplayName(
                    resolvedRow.inv,
                    WikiShopInfoboxParser.parseShopInfobox(wikiTitle, source)?.infoboxName,
                )

            table =
                WikiShopStoreParser.skillcapeTrimmed(resolvedRow.inv)?.let { trimmed ->
                    WikiShopStoreParser.parseSkillcapeShop(source, trimmed)
                }
                    ?: WikiShopStoreParser.parseSelectedTable(
                        source,
                        resolvedRow.wikiStore.takeIf { it.isNotBlank() },
                    )
        }

        if (table == null || table.lines.isEmpty()) {
            return skipped(
                resolvedRow,
                "no store table matched (wiki_store=${resolvedRow.wikiStore.ifBlank { "default" }})",
            )
        }

        val stockLines =
            table.lines.filter { ShopSpecialHandlers.shouldIncludeStockLine(resolvedRow, it.stock) }
        if (stockLines.isEmpty()) {
            return skipped(resolvedRow, "no in-stock lines after filtering stock=0")
        }

        itemLookup?.prewarm(
            stockLines.flatMap { line -> listOfNotNull(line.name, line.lookupName) }
        )

        val stock = mutableListOf<ResolvedShopStock>()
        val unresolved = linkedSetOf<String>()

        for (line in stockLines) {
            val objKey =
                if (buckets != null) {
                    resolveItemViaBuckets(buckets, line)
                } else {
                    val lookup = checkNotNull(itemLookup) { "dump mode requires an ItemWikiLookup" }
                    objLookup.resolveWikiItem(lookup, line.name)
                        ?: line.lookupName?.let { objLookup.resolveWikiItem(lookup, it) }
                }
            if (objKey == null) {
                unresolved += line.name
                log.warn("${resolvedRow.inv} unresolved item '${line.name}'")
                continue
            }
            stock +=
                ResolvedShopStock(
                    objKey = objKey,
                    count = line.stock,
                    restockCycles = line.restockCycles,
                )
        }

        if (stock.isEmpty()) {
            return ShopDumpResult(
                inv = resolvedRow.inv,
                wikiTitle = wikiTitle,
                outputFile = null,
                stock = emptyList(),
                unresolvedItems = unresolved.toList(),
                skippedReason = "no resolved stock lines",
            )
        }

        return ShopDumpResult(
            inv = resolvedRow.inv,
            wikiTitle = wikiTitle,
            outputFile = null,
            stock = stock,
            unresolvedItems = unresolved.toList(),
            skippedReason = null,
            table = table.copy(lines = stockLines),
            shopName = shopName,
        )
    }

    /**
     * Bucket-mode item resolution when the wiki fallback ([ItemWikiLookup]) is unavailable: the
     * wiki's `item_id` bucket for the store line's page title first (authoritative — mirrors
     * [ObjRscmLookup]'s documented priority of a real wiki item id over name heuristics), then
     * local heuristics. Verified live this ordering matters: "Skirt (blue)" on Agmundi Quality
     * Clothes resolves correctly via its page's item id (`dwarf_skirt3`), but the local
     * display-name heuristic guesses the wrong, unrelated `blue_skirt` key first if tried before
     * it.
     */
    private suspend fun resolveItemViaBuckets(
        buckets: ShopBuckets,
        line: ParsedStoreLine,
    ): String? {
        resolveViaBucketItemPage(buckets, line.name)?.let {
            return it
        }
        line.lookupName
            ?.let { resolveViaBucketItemPage(buckets, it) }
            ?.let {
                return it
            }
        resolveViaItemVersions(buckets, line.name)?.let {
            return it
        }
        line.lookupName
            ?.let { resolveViaItemVersions(buckets, it) }
            ?.let {
                return it
            }
        objLookup.resolveByDisplayName(line.name)?.let {
            return it
        }
        return line.lookupName?.let { objLookup.resolveByDisplayName(it) }
    }

    /**
     * `name` may carry a trimmed-cape `(t)` suffix that is not itself a wiki page — untrimmed and
     * trimmed cape variants share a single page (verified live: `item_id` for "Thieving cape"
     * returns two ids, one per variant, while "Thieving cape(t)" has no page at all). Strip the
     * suffix before querying. A page with more than one item id is only resolved when those ids are
     * *exactly* a base/`_trimmed` obj-key pair — anything else (an unrelated multi-variant page, or
     * a pair that isn't a base/trimmed relationship) is intentionally left unresolved rather than
     * guessed at, since a wrong guess here silently mis-stocks a shop.
     */
    private suspend fun resolveViaBucketItemPage(buckets: ShopBuckets, name: String): String? {
        val trimmed = name.trim().endsWith("(t)", ignoreCase = true)
        val pageTitle = if (trimmed) name.trim().dropLast("(t)".length).trim() else name.trim()
        val candidates = buckets.itemPageIds(pageTitle).mapNotNull { objLookup.toRscm(it) }

        if (candidates.size == 1) {
            return candidates.single()
        }
        if (candidates.size != 2) {
            return null
        }
        val (first, second) = candidates
        return when {
            second == "${first}_trimmed" -> if (trimmed) second else first
            first == "${second}_trimmed" -> if (trimmed) first else second
            else -> null
        }
    }

    /**
     * Resolves items that [resolveViaBucketItemPage] can't — a shared wiki page with multiple
     * dose/lit-state/etc versions — via `infobox_item`'s per-version rows, which is exactly the
     * structured version of what dump-mode's [ItemWikiLookup] anchor resolution does. Never
     * guesses: every step below only returns a result when it's unambiguous (exactly one distinct
     * id).
     *
     * Tier 1 — exact item-name lookup ([ShopBuckets.itemVersionsByName]): `infobox_item` carries
     * the literal per-version display name, so this handles every shape without needing to derive a
     * base page title, including names that don't share a stem with their page at all — verified
     * live for `Unlit torch` (page `Torch`), `Unlit bug lantern` (page `Bug lantern`), `Bronze
     * spear(kp)` (page `Bronze spear`), `Antipoison(3)`/`Waterskin(4)` (dose pages), and suffixless
     * multi-version names like `Karambwan vessel`/`Candle`.
     *
     * Tier 2 — parenthetical-suffix fallback ([ShopBuckets.itemVersions] on the base
     * page + [ShopBuckets.matchVersionBySuffix]): for the rarer case where the exact item-name
     * match misses (e.g. a formatting difference between the storeline's `sold_item` text and the
     * infobox's `item_name`) but `Name(sfx)` still parses and the base page's `version_anchor`
     * still matches the suffix.
     */
    private suspend fun resolveViaItemVersions(buckets: ShopBuckets, name: String): String? {
        val trimmedName = name.trim()

        resolveUnambiguousVersion(buckets, buckets.itemVersionsByName(trimmedName))?.let {
            return it
        }

        val suffixMatch = Regex("""^(.+?)\s*\((.+)\)$""").find(trimmedName) ?: return null
        val base = suffixMatch.groupValues[1].trim()
        val suffix = suffixMatch.groupValues[2].trim()
        val versions = buckets.itemVersions(base)
        return buckets.matchVersionBySuffix(versions, suffix)?.id?.let { objLookup.toRscm(it) }
    }

    private fun resolveUnambiguousVersion(
        buckets: ShopBuckets,
        versions: List<ItemVersion>,
    ): String? {
        if (versions.isEmpty()) {
            return null
        }
        val distinctIds = versions.map { it.id }.distinct()
        if (distinctIds.size == 1) {
            return objLookup.toRscm(distinctIds.single())
        }
        return buckets.defaultVersion(versions)?.id?.let { objLookup.toRscm(it) }
    }

    /**
     * Mirrors [WikiShopStoreParser.parseSkillcapeShop]'s union semantics (regular stock + cape-only
     * lines) using bucket-sourced tables. Unlike the wikitext version, the wiki's Bucket API does
     * not split skill-cape lines into their own `store_notes` group — verified live against `Martin
     * Thwait's Lost and Found.` and `Aubury's Rune Shop.`, both trimmed and untrimmed cape lines
     * sit in the same table as the regular stock, every row's `store_notes` blank. So the cape line
     * is instead identified by name (`"... cape"`, optionally `"... cape(t)"`) within the resolved
     * base table, and only the requested trim variant is spliced back in alongside the regular
     * stock.
     */
    private fun selectSkillcapeFromBuckets(
        tables: List<ParsedStoreTable>,
        trimmed: Boolean,
    ): ParsedStoreTable? {
        val baseTable =
            tables.firstOrNull { !it.hiddenStock && it.lines.isNotEmpty() && it.nameNotes == null }
                ?: tables.firstOrNull { !it.hiddenStock && it.lines.isNotEmpty() }
                ?: return null

        val (capeLines, regularLines) = baseTable.lines.partition { isCapeLine(it.name) }
        val selectedCapeLines =
            capeLines.filter { it.name.contains("(t)", ignoreCase = true) == trimmed }
        if (selectedCapeLines.isEmpty()) {
            return null
        }

        return baseTable.copy(lines = regularLines + selectedCapeLines)
    }

    private fun isCapeLine(name: String): Boolean {
        val trimmedName = name.trim()
        return trimmedName.endsWith("cape", ignoreCase = true) ||
            trimmedName.endsWith("cape(t)", ignoreCase = true) ||
            trimmedName.endsWith("cape (t)", ignoreCase = true)
    }

    /**
     * Counterpart to [selectSkillcapeFromBuckets]: a *base* (non-`_skillcape`) row's table can be
     * the exact same bucket-merged group a skillcape row would splice cape lines out of (bucket
     * data never separates them by `store_notes` — see [selectSkillcapeFromBuckets]'s doc). The
     * wikitext-mode dumper never has this problem because cape lines live in a separate `===Skill
     * cape===` heading entirely outside the main stock table. Only applied when
     * [capeSiblingArticles] confirms this page actually has a registered skillcape CSV row —
     * unconditionally stripping any line named "... cape" would wrongly delete real stock from
     * shops that legitimately sell ordinary capes (`Blue cape`, `Hunter's cape`, etc, confirmed
     * live in the checked-in corpus).
     */
    private fun excludeCapeSiblingLines(
        wikiTitle: String,
        table: ParsedStoreTable,
    ): ParsedStoreTable {
        if (wikiTitle !in capeSiblingArticles) {
            return table
        }
        return table.copy(lines = table.lines.filterNot { isCapeLine(it.name) })
    }

    fun writeToml(result: ShopDumpResult, outputDir: Path): Path {
        val table = result.table ?: error("missing table for ${result.inv}")
        val output = outputDir.resolve("${result.inv}.toml")
        output.parent?.createDirectories()
        output.writeText(
            formatToml(result.inv, result.shopName, table, result.stock, result.unresolvedItems)
        )
        return output
    }

    private fun skipped(row: ShopNameMapper.ShopCsvEntry, reason: String): ShopDumpResult =
        ShopDumpResult(
            inv = row.inv,
            wikiTitle = ShopNameMapper.stripWikiBrackets(row.wikiArticle),
            outputFile = null,
            stock = emptyList(),
            unresolvedItems = emptyList(),
            skippedReason = reason,
        )

    private fun formatToml(
        inv: String,
        shopName: String?,
        table: ParsedStoreTable,
        stock: List<ResolvedShopStock>,
        unresolved: List<String>,
    ): String = buildString {
        appendLine("[[inventory]]")
        appendLine("isServerOnly = true")
        appendLine("id = \"inv.$inv\"")
        shopName?.let { appendLine("name = \"${tomlEscape(it)}\"") }
        appendLine()
        appendLine("scope = \"Shared\"")
        appendLine("stack = \"Always\"")
        appendLine()
        table.sellMultiplier?.let { appendLine("sellMultiplier = $it") }
        table.buyMultiplier?.let { appendLine("buyMultiplier = $it") }
        table.delta?.let { appendLine("delta = $it") }
        if (table.sellMultiplier != null || table.buyMultiplier != null || table.delta != null) {
            appendLine()
        }
        appendLine("size = ${stock.size.coerceAtLeast(1)}")
        appendLine()
        appendLine("protect = false")
        appendLine("runWeight = false")
        appendLine("restock = true")
        appendLine("allStock = false")
        appendLine("placeholders = false")
        appendLine()

        for (unresolvedName in unresolved) {
            appendLine("# unresolved: $unresolvedName")
        }
        if (unresolved.isNotEmpty()) {
            appendLine()
        }

        for (line in stock) {
            appendLine("[[inventory.stock]]")
            appendLine("obj = \"${line.objKey}\"")
            appendLine("count = ${line.count}")
            appendLine("restockCycles = ${line.restockCycles}")
            appendLine()
        }
    }

    private fun tomlEscape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}

private fun defaultOutputDir(rootDir: String?): Path {
    val root = rootDir?.let { Path(it) } ?: GameValLoader.resolveRootOrNull()
    return root?.resolve(DEFAULT_OUTPUT_RELATIVE) ?: Path(DEFAULT_OUTPUT_RELATIVE)
}

private fun defaultMappingsPath(rootDir: String?): Path {
    val root = rootDir?.let { Path(it) } ?: GameValLoader.resolveRootOrNull()
    return root?.resolve(DEFAULT_MAPPINGS_RELATIVE) ?: Path(DEFAULT_MAPPINGS_RELATIVE)
}

private const val BUCKET_CACHE_DIR = ".data/cache/wiki-live/bucket"

/**
 * `--source=auto|bucket|dump` selects where shop/item wikitext comes from. `auto` (the default)
 * uses a local dump if [WikiClient.resolveDumpDirectory] resolves to an existing non-empty
 * directory, otherwise falls back to the wiki's live Bucket API.
 */
internal fun resolveUseBucket(sourceFlag: String?, wikiDumpDir: String?): Boolean =
    when (sourceFlag) {
        null,
        "auto" -> !dumpDirAvailable(wikiDumpDir)
        "bucket" -> true
        "dump" -> false
        else -> error("invalid --source=$sourceFlag (expected one of: auto, bucket, dump)")
    }

private fun dumpDirAvailable(wikiDumpDir: String?): Boolean {
    val dir = WikiClient.resolveDumpDirectory(wikiDumpDir)
    return dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
}

/**
 * Anchors the bucket disk cache to the repo root, matching [defaultOutputDir] /
 * [defaultMappingsPath].
 */
internal fun bucketCacheDir(rootDir: String?): Path {
    val root = rootDir?.let { Path(it) } ?: GameValLoader.resolveRootOrNull()
    return root?.resolve(BUCKET_CACHE_DIR) ?: Path(BUCKET_CACHE_DIR)
}

private suspend fun runDump(
    dumper: ShopWikiDumper,
    rows: List<ShopNameMapper.ShopCsvEntry>,
    outputDir: Path,
    log: DropDumpLog,
) {
    outputDir.createDirectories()

    var written = 0
    var skipped = 0

    for (row in rows) {
        val result = dumper.dumpShop(row)
        if (result.skippedReason != null) {
            skipped++
            log.warn("${row.inv} skipped — ${result.skippedReason}")
            continue
        }
        val out = dumper.writeToml(result, outputDir)
        written++
        log.info(
            "${row.inv} -> $out (${result.stock.size} item(s), ${result.unresolvedItems.size} unresolved)"
        )
    }

    println()
    println("Wrote $written shop TOML file(s) to $outputDir ($skipped skipped)")
}

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val verbose = flags.contains("--verbose") || flags.contains("-v")
    val offline = flags.contains("--offline")
    val limit = args.firstOrNull { it.startsWith("--limit=") }?.substringAfter('=')?.toIntOrNull()
    val invFilter = args.firstOrNull { it.startsWith("--inv=") }?.substringAfter('=')?.trim()
    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter('=')
            ?: System.getProperty("RSPS_ROOT")
    val wikiDumpDir =
        flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val sourceFlag =
        flags.firstOrNull { it.startsWith("--source=") }?.substringAfter('=')?.trim()?.lowercase()
    val mappingsPath =
        flags.firstOrNull { it.startsWith("--mappings=") }?.substringAfter('=')?.let { Path(it) }
            ?: defaultMappingsPath(rootDir)
    val outputDir =
        flags.firstOrNull { it.startsWith("--out-dir=") }?.substringAfter('=')?.let { Path(it) }
            ?: defaultOutputDir(rootDir)
    val log = DropDumpLog(quiet = quiet, verbose = verbose)
    val useBucket = resolveUseBucket(sourceFlag, wikiDumpDir)

    runBlocking {
        val elapsed = measureTimeMillis {
            GameValLoader.ensureLoaded(rootDir)
            val allRows = ShopNameMapper.loadDumpableRowsFromCsv(mappingsPath)
            val rows =
                allRows
                    .let { list ->
                        when {
                            invFilter.isNullOrBlank() -> list
                            else -> list.filter { it.inv.equals(invFilter, ignoreCase = true) }
                        }
                    }
                    .let { list -> if (limit != null) list.take(limit) else list }

            if (rows.isEmpty()) {
                System.err.println("No dumpable shop rows found in $mappingsPath")
                exitProcess(1)
            }

            log.info("dumping ${rows.size} shop(s) -> $outputDir")

            if (useBucket) {
                val bucketSource = BucketSource(bucketCacheDir(rootDir), offline = offline)
                // Computed from the UNFILTERED CSV load, not `rows` — a `--inv=`/`--limit=`
                // single-shop run must still see a base row's skillcape siblings even though
                // those sibling rows themselves aren't part of this run's `rows`.
                val capeSiblingArticles =
                    allRows
                        .filter { WikiShopStoreParser.skillcapeTrimmed(it.inv) != null }
                        .map {
                            ShopNameMapper.stripWikiBrackets(
                                ShopSpecialHandlers.resolveRow(it).wikiArticle
                            )
                        }
                        .toSet()
                val dumper =
                    ShopWikiDumper(
                        wiki = null,
                        objLookup = ObjRscmLookup(),
                        itemLookup = null,
                        log = log,
                        buckets = ShopBuckets(bucketSource),
                        capeSiblingArticles = capeSiblingArticles,
                    )
                runDump(dumper, rows, outputDir, log)
            } else {
                WikiClient.open(wikiDumpDir, onPageFetch = { title -> log.verbose("wiki: $title") })
                    .use { wiki ->
                        val dumper =
                            ShopWikiDumper(wiki, ObjRscmLookup(), ItemWikiLookup(wiki, log), log)
                        runDump(dumper, rows, outputDir, log)
                    }
            }
        }
        log.info("done in ${elapsed}ms")
    }
}
