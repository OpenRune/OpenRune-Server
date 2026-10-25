package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.farming.FarmingPatchRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/** One patch: its persisted [varp] and every loc that renders it (redwood has 19). */
data class PatchDef(
    val varp: String,
    val kind: PatchKind,
    val site: String,
    val locs: List<String>,
    val coords: List<CoordGrid>,
)

/** A per-site rule on a varbit or varp; see `SiteRule` in the pack for the value semantics. */
data class SiteRule(val site: String, val variable: String, val value: Int)

object FarmingPatches {
    private class Kind(
        val patches: List<PatchDef>,
        val diseaseFree: List<SiteRule>,
        val xpBonus: List<SiteRule>,
        val yieldBonus: List<SiteRule>,
        val access: Map<String, List<SiteAccess>>,
    )

    private val kinds: Map<PatchKind, Kind> by lazy {
        FarmingPatchRow.all().associate { row ->
            val kind = PatchKind.of(row.kind)
            val patches =
                row.patches
                    .groupBy { it.t1 }
                    .map { (varp, parts) ->
                        PatchDef(
                            varp = RSCM.getReverseMapping(RSCMType.VARP, varp),
                            kind = kind,
                            site = parts.first().t2,
                            locs = parts.map { it.t0.internalName },
                            coords = parts.map { it.t3 },
                        )
                    }
            kind to
                Kind(
                    patches = patches,
                    diseaseFree = row.diseaseFree.map { SiteRule(it.t0, it.t1, it.t2) },
                    xpBonus = row.xpBonus.map { SiteRule(it.t0, it.t1, it.t2) },
                    yieldBonus = row.yieldBonus.map { SiteRule(it.t0, it.t1, it.t2) },
                    access = row.access.map { SiteAccess(it.t0, it.t1, it.t2, it.t3, it.t4) }.groupBy(SiteAccess::site),
                )
        }
    }

    val all: List<PatchDef> by lazy { kinds.values.flatMap(Kind::patches) }

    private val byLoc: Map<String, PatchDef> by lazy {
        all.flatMap { patch -> patch.locs.map { it to patch } }.toMap()
    }

    private val bySite: Map<String, List<PatchDef>> by lazy { all.groupBy(PatchDef::site) }

    fun byLoc(loc: String): PatchDef? = byLoc[loc]

    fun bySite(site: String): List<PatchDef> = bySite[site].orEmpty()

    internal fun access(patch: PatchDef): List<SiteAccess> =
        kinds.getValue(patch.kind).access[patch.site].orEmpty()
    fun diseaseFree(player: Player, patch: PatchDef): Boolean =
        kinds.getValue(patch.kind).diseaseFree.any {
            it.site == patch.site && (it.variable.isEmpty() || player.vars[it.variable] >= it.value)
        }

    fun xpBonusPercent(player: Player, patch: PatchDef): Int =
        bonus(player, patch, kinds.getValue(patch.kind).xpBonus)

    fun yieldBonus(player: Player, patch: PatchDef): Int =
        bonus(player, patch, kinds.getValue(patch.kind).yieldBonus)

    /** Diary tiers stack as alternatives: the best completed tier applies. */
    private fun bonus(player: Player, patch: PatchDef, rules: List<SiteRule>): Int =
        rules.filter { it.site == patch.site && player.vars[it.variable] >= 1 }.maxOfOrNull { it.value } ?: 0
}
