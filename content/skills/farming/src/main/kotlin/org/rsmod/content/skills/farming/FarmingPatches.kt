package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.farming.FarmingPatchRow
import org.rsmod.map.CoordGrid

data class PatchDef(val loc: String, val kind: PatchKind, val varp: String)

/**
 * A farm. All five share the same handful of `farming_transmit_*` varbits, so only one area's
 * patches can be described to the client at a time; [centre] is how the nearest one is picked.
 */
data class FarmingArea(val name: String, val centre: CoordGrid, val patches: List<PatchDef>)

object FarmingPatches {
    // ponytail: area centres are approximate with a generous radius. They only decide which farm's
    // state gets described to the client on approach; interacting with a patch transmits its own
    // area regardless, so a centre being a few tiles out costs nothing.
    const val AREA_RADIUS: Int = 40

    val areas: List<FarmingArea> by lazy {
        FarmingPatchRow.all().groupBy(FarmingPatchRow::area).map { (name, rows) ->
            FarmingArea(
                name = name,
                centre = rows.first().centre,
                patches =
                    rows.map { row ->
                        PatchDef(
                            loc = RSCM.getReverseMapping(RSCMType.LOC, row.loc.id),
                            kind = PatchKind.of(row.kind),
                            varp = RSCM.getReverseMapping(RSCMType.VARP, row.varp),
                        )
                    },
            )
        }
    }

    val all: List<PatchDef> by lazy { areas.flatMap(FarmingArea::patches) }

    fun areaOf(patch: PatchDef): FarmingArea = areas.first { patch in it.patches }

    fun nearest(coords: CoordGrid): FarmingArea? =
        areas.firstOrNull {
            it.centre.level == coords.level &&
                kotlin.math.abs(it.centre.x - coords.x) <= AREA_RADIUS &&
                kotlin.math.abs(it.centre.z - coords.z) <= AREA_RADIUS
        }
}

/**
 * Advance the whole farm against the same snapshot. Split at flower maturity so offline time
 * before that boundary is never protected by a flower which only grows during this update.
 */
internal fun FarmingArea.advance(
    states: List<PatchState>,
    elapsed: Int,
    diseaseRoll: (Double) -> Boolean,
): List<PatchState> {
    val current = states.toMutableList()
    val protection = BooleanArray(patches.size)
    var left = elapsed
    while (left > 0) {
        var step = left
        for (i in patches.indices) {
            val state = current[i]
            val crop = state.crop ?: continue
            if (patches[i].kind == PatchKind.FLOWER && state.health == Health.HEALTHY &&
                !state.grown(crop)
            ) {
                val maturity = state.minutes + (crop.stages - state.stage - 1) * crop.stageMinutes
                step = minOf(step, maturity.coerceAtLeast(1))
            }
        }
        for (i in patches.indices) {
            val crop = current[i].crop
            protection[i] =
                crop != null && crop.protectionIndex != 0 &&
                    patches.indices.any { flower ->
                        val state = current[flower]
                        patches[flower].kind == PatchKind.FLOWER &&
                            state.cropIndex == crop.protectionIndex &&
                            state.health == Health.HEALTHY && state.crop?.let { state.grown(it) } == true
                    }
        }
        for (i in patches.indices) {
            val state = current[i]
            current[i] =
                state.advance(state.crop, step, FarmingComposts.get(state.compost), protection[i], diseaseRoll)
        }
        left -= step
    }
    return current
}
