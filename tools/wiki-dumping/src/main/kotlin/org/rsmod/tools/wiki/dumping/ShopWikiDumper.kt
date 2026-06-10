package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.wiki.WikiClient
import org.rsmod.tools.wiki.dumping.wiki.WikiShopInfoboxParser
import org.rsmod.tools.wiki.dumping.wiki.WikiShopStoreParser

private const val DEFAULT_MAPPINGS_RELATIVE = "tools/wiki-dumping/src/main/resources/shopmappings.csv"
private const val DEFAULT_OUTPUT_RELATIVE = ".data/raw-cache/server/shops"

data class ResolvedShopStock(
    val objKey: String,
    val count: Int,
    val restockCycles: Int,
)

data class ShopDumpResult(
    val inv: String,
    val wikiTitle: String,
    val outputFile: Path?,
    val stock: List<ResolvedShopStock>,
    val unresolvedItems: List<String>,
    val skippedReason: String? = null,
    val table: org.rsmod.tools.wiki.dumping.wiki.ParsedStoreTable? = null,
    val shopName: String? = null,
)

class ShopWikiDumper(
    private val wiki: WikiClient,
    private val objLookup: ObjRscmLookup,
    private val itemLookup: ItemWikiLookup,
    private val log: DropDumpLog,
) {
    suspend fun dumpShop(row: ShopNameMapper.ShopCsvEntry): ShopDumpResult {
        val resolvedRow = ShopSpecialHandlers.resolveRow(row)
        val wikiTitle = ShopNameMapper.stripWikiBrackets(resolvedRow.wikiArticle)
        if (wikiTitle.isBlank()) {
            return skipped(resolvedRow, "no wiki article")
        }

        val source = runCatching { wiki.rawPageSource(wikiTitle) }.getOrNull()
        if (source.isNullOrBlank()) {
            return skipped(resolvedRow, "wiki page not found: $wikiTitle")
        }

        val shopName =
            ShopSpecialHandlers.resolveShopDisplayName(
                resolvedRow.inv,
                WikiShopInfoboxParser.parseShopInfobox(wikiTitle, source)?.infoboxName,
            )

        val table =
            WikiShopStoreParser.skillcapeTrimmed(resolvedRow.inv)?.let { trimmed ->
                WikiShopStoreParser.parseSkillcapeShop(source, trimmed)
            }
                ?: WikiShopStoreParser.parseSelectedTable(
                    source,
                    resolvedRow.wikiStore.takeIf { it.isNotBlank() },
                )
        if (table == null || table.lines.isEmpty()) {
            return skipped(
                resolvedRow,
                "no store table matched (wiki_store=${resolvedRow.wikiStore.ifBlank { "default" }})",
            )
        }

        val stockLines = table.lines.filter { ShopSpecialHandlers.shouldIncludeStockLine(resolvedRow, it.stock) }
        if (stockLines.isEmpty()) {
            return skipped(resolvedRow, "no in-stock lines after filtering stock=0")
        }

        itemLookup.prewarm(stockLines.flatMap { line -> listOfNotNull(line.name, line.lookupName) })

        val stock = mutableListOf<ResolvedShopStock>()
        val unresolved = linkedSetOf<String>()

        for (line in stockLines) {
            val objKey =
                objLookup.resolveWikiItem(itemLookup, line.name)
                    ?: line.lookupName?.let { objLookup.resolveWikiItem(itemLookup, it) }
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

    fun writeToml(result: ShopDumpResult, outputDir: Path): Path {
        val table = result.table ?: error("missing table for ${result.inv}")
        val output = outputDir.resolve("${result.inv}.toml")
        output.parent?.createDirectories()
        output.writeText(formatToml(result.inv, result.shopName, table, result.stock, result.unresolvedItems))
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
        table: org.rsmod.tools.wiki.dumping.wiki.ParsedStoreTable,
        stock: List<ResolvedShopStock>,
        unresolved: List<String>,
    ): String =
        buildString {
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

    private fun tomlEscape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

}

private fun defaultOutputDir(rootDir: String?): Path {
    val root = rootDir?.let { Path(it) } ?: GameValLoader.resolveRootOrNull()
    return root?.resolve(DEFAULT_OUTPUT_RELATIVE) ?: Path(DEFAULT_OUTPUT_RELATIVE)
}

private fun defaultMappingsPath(rootDir: String?): Path {
    val root = rootDir?.let { Path(it) } ?: GameValLoader.resolveRootOrNull()
    return root?.resolve(DEFAULT_MAPPINGS_RELATIVE) ?: Path(DEFAULT_MAPPINGS_RELATIVE)
}

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("-") }.toSet()
    val quiet = flags.contains("--quiet") || flags.contains("-q")
    val verbose = flags.contains("--verbose") || flags.contains("-v")
    val limit = args.firstOrNull { it.startsWith("--limit=") }?.substringAfter('=')?.toIntOrNull()
    val invFilter = args.firstOrNull { it.startsWith("--inv=") }?.substringAfter('=')?.trim()
    val rootDir =
        flags.firstOrNull { it.startsWith("--root=") }?.substringAfter('=')
            ?: System.getProperty("RSPS_ROOT")
    val wikiDumpDir = flags.firstOrNull { it.startsWith("--wiki-dump=") }?.substringAfter("--wiki-dump=")
    val mappingsPath =
        flags.firstOrNull { it.startsWith("--mappings=") }?.substringAfter('=')?.let { Path(it) }
            ?: defaultMappingsPath(rootDir)
    val outputDir =
        flags.firstOrNull { it.startsWith("--out-dir=") }?.substringAfter('=')?.let { Path(it) }
            ?: defaultOutputDir(rootDir)
    val log = DropDumpLog(quiet = quiet, verbose = verbose)

    runBlocking {
        val elapsed =
            measureTimeMillis {
                GameValLoader.ensureLoaded(rootDir)
                val rows =
                    ShopNameMapper
                        .loadDumpableRowsFromCsv(mappingsPath)
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

                WikiClient.open(wikiDumpDir, onPageFetch = { title -> log.verbose("wiki: $title") }).use { wiki ->
                    val dumper = ShopWikiDumper(wiki, ObjRscmLookup(), ItemWikiLookup(wiki, log), log)
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
                        log.info("${row.inv} -> $out (${result.stock.size} item(s), ${result.unresolvedItems.size} unresolved)")
                    }

                    println()
                    println("Wrote $written shop TOML file(s) to $outputDir ($skipped skipped)")
                }
            }
        log.info("done in ${elapsed}ms")
    }
}
