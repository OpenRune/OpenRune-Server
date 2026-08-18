package org.rsmod.tools.wiki.dumping.dropfill

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import java.util.Locale
import kotlin.math.abs

/**
 * One decision per mechanical anchor: fill it from Dropsline or quarantine it with a stated reason
 * — never guess.
 *
 * TOML anchors carry a subsection and must agree with the dumper's `_unknown_drop_rates.txt`
 * manifest row where one exists. Kotlin anchors carry no subsection, so the anchor's own rate
 * template (with variables solved from the file's unambiguous rows) disambiguates instead. The
 * template never supplies the emitted number — Dropsline does.
 */
data class FillDecision(
    val anchor: DropAnchor,
    val outcome: Outcome,
    val reason: String? = null,
    val fill: Fill? = null,
    val sourceRarity: String? = null,
    /**
     * Set only on the "ambiguous rarities, no usable template expectation" quarantine — the one
     * case where the anchor's own row is knowable by elimination once its siblings have matched.
     */
    val eliminable: Boolean = false,
) {
    enum class Outcome {
        FILL,
        QUARANTINE,
    }
}

data class Fill(
    val anchorLine: Int, // 1-based line of the anchor comment
    val objSymbol: String,
    val rate: RarityFraction,
    val qtyLow: Int,
    val qtyHigh: Int,
)

/**
 * Wiki item name → `obj.*` gameval key. Names never map textually (gamevals keep historical names:
 * "Grimy avantoe" is obj.unidentified_avantoe), so resolution goes name → item id (wiki
 * `infobox_item` bucket) → symbol (reverse RSCM mapping). Any step that is not exactly 1:1 fails,
 * and the caller quarantines. Noted drops use the `cert_` variant of the base symbol.
 *
 * A page defining several versions (the Dragon knife page carries the base knife and its three
 * poisoned forms) is not ambiguous when exactly one version's item name matches the drop row's item
 * text — the drop names the item the game actually rolls, and derived forms are player-made.
 */
class StrictObjResolver(private val itemRows: suspend (String) -> List<InfoboxItemRow>) {
    sealed interface Result {
        data class Ok(val symbol: String) : Result

        data class Fail(val reason: String) : Result
    }

    suspend fun resolve(item: String, noted: Boolean): Result {
        val rows = itemRows(item)
        var ids = rows.flatMap { it.ids }.distinct()
        if (ids.isEmpty()) {
            return Result.Fail("no wiki item ids for \"$item\"")
        }
        if (ids.size > 1) {
            val narrowed = narrowByItemName(rows, item)
            if (narrowed.size != 1) {
                return Result.Fail("ambiguous wiki ids for \"$item\": ${ids.joinToString(", ")}")
            }
            ids = narrowed
        }
        val name =
            reverseObj(ids[0]) ?: return Result.Fail("no obj symbol for id ${ids[0]} (\"$item\")")
        if (noted) {
            val cert = "cert_$name"
            if (!hasObjMapping(cert)) {
                return Result.Fail("no cert (noted) obj symbol $cert for \"$item\"")
            }
            return Result.Ok("obj.$cert")
        }
        return Result.Ok("obj.$name")
    }

    private fun reverseObj(itemId: Int): String? {
        val mapped =
            runCatching { RSCM.getReverseMapping(RSCMType.OBJ, itemId) }
                .getOrNull()
                ?.trim()
                .orEmpty()
        if (mapped.isBlank() || mapped == "-1") {
            return null
        }
        return mapped.removePrefix("obj.")
    }

    private fun hasObjMapping(name: String): Boolean =
        runCatching {
                RSCM.getRSCM("obj.$name")
                true
            }
            .getOrDefault(false)

    companion object {
        /** Ids of the versions whose item name is exactly [item]; empty when none match. */
        fun narrowByItemName(rows: List<InfoboxItemRow>, item: String): List<Int> =
            rows.filter { it.itemName == item }.flatMap { it.ids }.distinct()
    }
}

object DropRateDecisions {
    private const val TOLERANCE = 0.005 // 0.5%

    /**
     * Candidate rows for an item, deduplicated on the REDUCED fraction plus quantity and notedness:
     * the wiki writes the same probability more than one way ("1/181.3" vs "2/362.6"), and keying
     * on text would treat those as rival candidates and quarantine a decidable case. The first
     * spelling seen is kept, so reports cite what the wiki showed first.
     */
    private fun dedupCandidates(drops: List<DropsRow>, item: String): List<DropsRow> {
        val seen = LinkedHashMap<String, DropsRow>()
        for (row in drops) {
            if (row.item != item) continue
            val fraction = RarityFraction.parse(row.rarity) ?: continue
            seen.putIfAbsent("${fraction.key}|${row.qtyLow}|${row.qtyHigh}|${row.noted}", row)
        }
        return seen.values.toList()
    }

    private fun formatExpected(expected: Double): String =
        "1/" + String.format(Locale.ROOT, "%.2f", 1 / expected)

    suspend fun decideToml(
        anchor: DropAnchor,
        manifest: List<ManifestRow>,
        drops: List<DropsRow>,
        resolver: StrictObjResolver,
        vars: Map<String, Double>,
    ): FillDecision {
        fun quarantine(reason: String) =
            FillDecision(anchor, FillDecision.Outcome.QUARANTINE, reason)

        if (anchor.table != "main") {
            return quarantine("anchor table \"${anchor.table}\" is not main")
        }
        val manifestRow =
            manifest.find {
                it.mechanical &&
                    it.page == anchor.page &&
                    it.item == anchor.item &&
                    it.subsection == anchor.subsection
            }
        if (manifestRow != null && manifestRow.rarity != anchor.rarity) {
            return quarantine(
                "manifest rarity differs from anchor: ${manifestRow.rarity} vs ${anchor.rarity}"
            )
        }
        if (drops.isEmpty()) {
            // Distinct from "no numeric row": the whole page came back empty,
            // which points at a page-name/transclusion problem, not missing data.
            return quarantine("page returned 0 Dropsline rows")
        }
        var candidates = dedupCandidates(drops, anchor.item)
        if (candidates.isEmpty()) {
            return quarantine("no numeric Dropsline row for item")
        }
        if (candidates.map { RarityFraction.parse(it.rarity)!!.key }.toSet().size > 1) {
            val expected =
                WikiExpr.evaluate(anchor.rarity, vars)
                    ?: return quarantine(
                            "ambiguous Dropsline rarities: ${candidates.joinToString(" | ") { it.rarity }}"
                        )
                        .copy(eliminable = true)
            val within =
                candidates.filter {
                    abs(RarityFraction.parse(it.rarity)!!.rate - expected) / expected <= TOLERANCE
                }
            if (within.size != 1) {
                return quarantine(
                    "template expectation ${formatExpected(expected)} matched ${within.size} of " +
                        "${candidates.size} candidates: ${candidates.joinToString(" | ") { it.rarity }}"
                )
            }
            candidates = within
        }
        if (candidates.size > 1) {
            return quarantine(
                "multiple quantity/notedness variants: " +
                    candidates.joinToString(" | ") {
                        "${it.qtyLow}..${it.qtyHigh}" + if (it.noted) " (noted)" else ""
                    }
            )
        }
        val row = candidates[0]
        val symbol =
            when (val resolved = resolver.resolve(anchor.item, row.noted)) {
                is StrictObjResolver.Result.Fail -> return quarantine(resolved.reason)
                is StrictObjResolver.Result.Ok -> resolved.symbol
            }
        return FillDecision(
            anchor = anchor,
            outcome = FillDecision.Outcome.FILL,
            sourceRarity = row.rarity,
            fill =
                Fill(
                    anchor.line,
                    symbol,
                    RarityFraction.parse(row.rarity)!!,
                    row.qtyLow,
                    row.qtyHigh,
                ),
        )
    }

    suspend fun decideKotlin(
        anchor: DropAnchor,
        drops: List<DropsRow>,
        resolver: StrictObjResolver,
        vars: Map<String, Double>,
    ): FillDecision {
        val expected = WikiExpr.evaluate(anchor.rarity, vars)

        fun quarantine(reason: String) =
            FillDecision(anchor, FillDecision.Outcome.QUARANTINE, reason)

        if (anchor.table != "main") {
            return quarantine("anchor table \"${anchor.table}\" is not main")
        }
        if (drops.isEmpty()) {
            return quarantine("page returned 0 Dropsline rows")
        }
        val candidates = dedupCandidates(drops, anchor.item)
        if (candidates.isEmpty()) {
            return quarantine("no numeric Dropsline row for item")
        }
        val chosen: DropsRow
        if (candidates.size == 1) {
            chosen = candidates[0]
            if (expected != null) {
                val rate = RarityFraction.parse(chosen.rarity)!!.rate
                if (abs(rate - expected) / expected > TOLERANCE) {
                    return quarantine(
                        "sole candidate ${chosen.rarity} disagrees with template expectation ${formatExpected(expected)}"
                    )
                }
            }
        } else {
            if (expected == null) {
                return quarantine(
                        "ambiguous Dropsline rarities and no usable template expectation: " +
                            candidates.joinToString(" | ") { it.rarity }
                    )
                    .copy(eliminable = true)
            }
            val within =
                candidates.filter {
                    abs(RarityFraction.parse(it.rarity)!!.rate - expected) / expected <= TOLERANCE
                }
            when {
                within.isEmpty() ->
                    return quarantine(
                        "no candidate matches template expectation ${formatExpected(expected)}: " +
                            candidates.joinToString(" | ") { it.rarity }
                    )
                within.size > 1 ->
                    return quarantine(
                        "template expectation matches ${within.size} candidates: " +
                            within.joinToString(" | ") { it.rarity }
                    )
            }
            chosen = within[0]
        }
        val symbol =
            when (val resolved = resolver.resolve(anchor.item, chosen.noted)) {
                is StrictObjResolver.Result.Fail -> return quarantine(resolved.reason)
                is StrictObjResolver.Result.Ok -> resolved.symbol
            }
        return FillDecision(
            anchor = anchor,
            outcome = FillDecision.Outcome.FILL,
            sourceRarity = chosen.rarity,
            fill =
                Fill(
                    anchor.line,
                    symbol,
                    RarityFraction.parse(chosen.rarity)!!,
                    chosen.qtyLow,
                    chosen.qtyHigh,
                ),
        )
    }

    /**
     * Per-file variable observations: an anchor contributes when its item has exactly one numeric
     * candidate (by reduced fraction) and its template holds exactly one variable.
     */
    fun observations(
        anchors: List<DropAnchor>,
        drops: List<DropsRow>,
    ): List<VariableSolver.Observation> =
        anchors.mapNotNull { anchor ->
            val fractions = LinkedHashMap<String, RarityFraction>()
            for (row in drops) {
                if (row.item != anchor.item) continue
                val fraction = RarityFraction.parse(row.rarity) ?: continue
                fractions.putIfAbsent(fraction.key, fraction)
            }
            val candidates = fractions.values.toList()
            if (candidates.size == 1 && WikiExpr.variablesIn(anchor.rarity).size == 1) {
                VariableSolver.Observation(anchor.rarity, candidates[0].rate)
            } else {
                null
            }
        }

    fun solveFileVariables(anchors: List<DropAnchor>, drops: List<DropsRow>): Map<String, Double> {
        val observed = observations(anchors, drops)
        val vars = mutableMapOf<String, Double>()
        for (name in anchors.flatMap { WikiExpr.variablesIn(it.rarity) }.distinct()) {
            VariableSolver.solve(observed, name)?.let { vars[name] = it }
        }
        return vars
    }

    /**
     * Second pass over one file's decisions: an item whose sibling anchors have each matched a
     * distinct Dropsline candidate, leaving exactly one anchor and exactly one candidate, gets that
     * candidate by elimination — the wiki lists the item once per table it sits on, so a
     * fully-claimed row set identifies the leftover. Anything short of an exact pairing (two
     * leftovers, an unclaimed extra candidate, a fill matching zero or two candidates) leaves the
     * quarantine in place.
     */
    fun eliminateRemaining(
        decisions: List<FillDecision>,
        drops: List<DropsRow>,
    ): Map<DropAnchor, DropsRow> {
        val eliminated = mutableMapOf<DropAnchor, DropsRow>()
        for ((item, group) in decisions.groupBy { it.anchor.item }) {
            val leftovers =
                group.filter { it.outcome == FillDecision.Outcome.QUARANTINE && it.eliminable }
            if (leftovers.size != 1) {
                continue
            }
            val fills = group.mapNotNull { it.fill }
            val remaining = dedupCandidates(drops, item).toMutableList()
            if (remaining.size != fills.size + 1) {
                continue
            }
            val paired =
                fills.all { fill ->
                    val match =
                        remaining.filter { row ->
                            RarityFraction.parse(row.rarity)!!.key == fill.rate.key &&
                                row.qtyLow == fill.qtyLow &&
                                row.qtyHigh == fill.qtyHigh
                        }
                    match.size == 1 && remaining.remove(match[0])
                }
            if (paired && remaining.size == 1) {
                eliminated[leftovers[0].anchor] = remaining[0]
            }
        }
        return eliminated
    }
}
