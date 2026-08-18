package org.rsmod.tools.wiki.dumping.dropfill

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import org.rsmod.tools.wiki.dumping.GameValLoader

/**
 * Fills the dumper's `Unknown wiki drop rate` markers from the wiki's Dropsline Bucket API — the
 * post-template-expansion form of the same `{{DropsLine}}` rows the wikitext parser reads, which
 * carries numeric rarities where the wikitext shows an unexpanded `{{#expr}}` template.
 *
 * Every anchor is either filled with an exact fraction taken verbatim from Dropsline, or
 * quarantined with a stated reason. Nothing is guessed: rate templates are evaluated only to
 * *choose between* candidate rows, never to produce the emitted number.
 *
 *     ./gradlew :tools:wiki-dumping:fillUnknownDropRates
 *     ./gradlew :tools:wiki-dumping:fillUnknownDropRates --args="--dry-run"
 *
 * Flags: --root=<tree> --cache=<dir> --report=<tsv> --offline --dry-run --quiet
 */
fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }
    fun flagValue(name: String): String? =
        flags.firstOrNull { it.startsWith("--$name=") }?.substringAfter("--$name=")

    val root =
        flagValue("root")?.let { Path.of(it) }
            ?: GameValLoader.resolveRootOrNull()
            ?: error("cannot locate repo root — pass --root=<tree>")
    val cacheDir =
        flagValue("cache")?.let { Path.of(it) } ?: root.resolve(".data/cache/wiki-dropfill")
    val offline = "--offline" in flags
    val dryRun = "--dry-run" in flags
    val quiet = "--quiet" in flags

    fun info(message: String) {
        if (!quiet) println(message)
    }

    val tablesDir = root.resolve("content/drops/src/main/resources/drops/tables")
    val monstersDir = tablesDir.resolve("monsters")
    val kotlinDir =
        root.resolve("content/drops/src/main/kotlin/org/rsmod/content/drops/tables/monsters")
    val manifestFile = tablesDir.resolve("_unknown_drop_rates.txt")
    check(monstersDir.exists() && kotlinDir.exists() && manifestFile.exists()) {
        "drop-table tree not found under $root"
    }

    GameValLoader.ensureLoaded(root.absolutePathString())

    val manifest = DropRateAnchors.parseManifest(manifestFile.readText())
    val manifestPages = manifest.filter { it.mechanical }.map { it.page }.toSet()
    val tomlAnchors =
        DropRateAnchors.scanTomlAnchors(monstersDir, manifestPages).filter { it.mechanical }
    val kotlinAnchors = DropRateAnchors.scanKotlinAnchors(kotlinDir).filter { it.mechanical }
    info(
        "fillUnknownDropRates: ${tomlAnchors.size} toml + ${kotlinAnchors.size} kotlin mechanical anchors"
    )

    val decisions = mutableListOf<FillDecision>()
    val fetchFailures = mutableListOf<String>()

    runBlocking {
        BucketClient(cacheDir, offline).use { bucket ->
            val itemRowCache = mutableMapOf<String, List<InfoboxItemRow>>()
            val resolver = StrictObjResolver { item ->
                itemRowCache.getOrPut(item) { bucket.itemRowsForPageName(item) }
            }
            // getOrPut cannot cache a null result, so failures are stored explicitly
            // rather than re-fetched (and re-reported) for every file on the same page.
            val dropsByPage = mutableMapOf<String, List<DropsRow>?>()
            suspend fun dropsFor(page: String): List<DropsRow>? {
                if (page in dropsByPage) {
                    return dropsByPage[page]
                }
                val drops =
                    try {
                        bucket.dropsForPage(page)
                    } catch (e: Exception) {
                        fetchFailures += "fetch failed for $page: ${e.message}"
                        null
                    }
                dropsByPage[page] = drops
                return drops
            }

            suspend fun processFormat(
                anchors: List<DropAnchor>,
                decide: suspend (DropAnchor, List<DropsRow>, Map<String, Double>) -> FillDecision,
                patch: (String, List<Fill>) -> String,
            ) {
                for ((file, fileAnchors) in anchors.groupBy { it.file }.toSortedMap()) {
                    val page = fileAnchors[0].page
                    if (page.isEmpty()) {
                        decisions +=
                            fileAnchors.map {
                                FillDecision(
                                    it,
                                    FillDecision.Outcome.QUARANTINE,
                                    "file has no derivable wiki page (no id field)",
                                )
                            }
                        continue
                    }
                    val drops = dropsFor(page)
                    if (drops == null) {
                        // Do NOT fall through: the decision logic would report the page
                        // as empty, which reads as "the wiki has no data" when the truth
                        // is that we never got to ask.
                        decisions +=
                            fileAnchors.map {
                                FillDecision(
                                    it,
                                    FillDecision.Outcome.QUARANTINE,
                                    "Dropsline fetch failed for $page",
                                )
                            }
                        continue
                    }
                    val vars = DropRateDecisions.solveFileVariables(fileAnchors, drops)
                    var fileDecisions = fileAnchors.map { decide(it, drops, vars) }
                    val eliminated = DropRateDecisions.eliminateRemaining(fileDecisions, drops)
                    fileDecisions =
                        fileDecisions.map { decision ->
                            val row = eliminated[decision.anchor] ?: return@map decision
                            when (
                                val resolved = resolver.resolve(decision.anchor.item, row.noted)
                            ) {
                                is StrictObjResolver.Result.Fail -> decision
                                is StrictObjResolver.Result.Ok ->
                                    FillDecision(
                                        anchor = decision.anchor,
                                        outcome = FillDecision.Outcome.FILL,
                                        reason = "by elimination: siblings matched every other row",
                                        sourceRarity = row.rarity,
                                        fill =
                                            Fill(
                                                decision.anchor.line,
                                                resolved.symbol,
                                                RarityFraction.parse(row.rarity)!!,
                                                row.qtyLow,
                                                row.qtyHigh,
                                            ),
                                    )
                            }
                        }
                    decisions += fileDecisions
                    val fills =
                        fileDecisions
                            .filter { it.outcome == FillDecision.Outcome.FILL }
                            .map { it.fill!! }
                    if (fills.isEmpty()) {
                        continue
                    }
                    val patched = patch(file.readText(), fills)
                    if (!dryRun) {
                        file.writeText(patched)
                    }
                    info("  ${file.name}: ${fills.size} filled")
                }
            }

            processFormat(
                anchors = tomlAnchors,
                decide = { anchor, drops, vars ->
                    DropRateDecisions.decideToml(anchor, manifest, drops, resolver, vars)
                },
                patch = DropRatePatcher::patchToml,
            )
            processFormat(
                anchors = kotlinAnchors,
                decide = { anchor, drops, vars ->
                    DropRateDecisions.decideKotlin(anchor, drops, resolver, vars)
                },
                patch = DropRatePatcher::patchKotlin,
            )
        }
    }

    val filled = decisions.filter { it.outcome == FillDecision.Outcome.FILL }
    val quarantined = decisions.filter { it.outcome == FillDecision.Outcome.QUARANTINE }
    val mechanical = tomlAnchors.size + kotlinAnchors.size
    val reconciled = mechanical == filled.size + quarantined.size

    info(
        "filled ${filled.size}, quarantined ${quarantined.size} of $mechanical mechanical anchors" +
            (if (dryRun) " (dry run — nothing written)" else "")
    )
    for (decision in quarantined) {
        info(
            "  quarantine ${decision.anchor.file.name}:${decision.anchor.line} ${decision.anchor.item} — ${decision.reason}"
        )
    }

    flagValue("report")?.let { reportPath ->
        val report = buildString {
            appendLine(
                "file\tline\titem\tpage\toutcome\tobj\tnumerator\tdenominator\tqtyLow\tqtyHigh\tsourceRarity\treason"
            )
            for (d in
                decisions.sortedWith(compareBy({ it.anchor.file.name }, { it.anchor.line }))) {
                appendLine(
                    listOf(
                            d.anchor.file.name,
                            d.anchor.line,
                            d.anchor.item,
                            d.anchor.page,
                            d.outcome.name,
                            d.fill?.objSymbol ?: "",
                            d.fill?.rate?.numerator ?: "",
                            d.fill?.rate?.denominator ?: "",
                            d.fill?.qtyLow ?: "",
                            d.fill?.qtyHigh ?: "",
                            d.sourceRarity ?: "",
                            d.reason ?: "",
                        )
                        .joinToString("\t")
                )
            }
        }
        val path = Path.of(reportPath)
        path.parent?.let(Files::createDirectories)
        path.writeText(report)
        info("report → $reportPath")
    }

    if (!reconciled) {
        System.err.println(
            "RECONCILIATION BROKEN: $mechanical != ${filled.size} + ${quarantined.size}"
        )
    }
    if (fetchFailures.isNotEmpty()) {
        fetchFailures.forEach { System.err.println("FETCH FAILURE: $it") }
    }
    if (!reconciled || fetchFailures.isNotEmpty()) {
        exitProcess(1)
    }
}
