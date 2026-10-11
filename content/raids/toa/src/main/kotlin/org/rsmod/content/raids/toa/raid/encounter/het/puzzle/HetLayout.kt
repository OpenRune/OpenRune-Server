package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import org.rsmod.api.table.ToaHetLayoutsRow
import org.rsmod.map.CoordGrid

internal class HetBarrierSpec(val type: HetBarrierType, val tile: CoordGrid, val rotation: Int)

internal class HetFixedMirrorSpec(val tile: CoordGrid, val rotation: Int)

internal class HetLayout(
    val barriers: List<HetBarrierSpec>,
    val fixedMirrors: List<HetFixedMirrorSpec>,
)

internal object HetLayouts {
    val ALL: List<HetLayout> by lazy { ToaHetLayoutsRow.all().map(::layoutOf) }

    private fun layoutOf(row: ToaHetLayoutsRow): HetLayout {
        val barriers =
            barriers(HetBarrierType.INTERNAL, row.internalTiles, row.internalAngles) +
                barriers(HetBarrierType.END, row.endTiles, row.endAngles) +
                barriers(
                    HetBarrierType.BREAKABLE_INTERNAL,
                    row.breakableInternalTiles,
                    row.breakableInternalAngles,
                ) +
                barriers(
                    HetBarrierType.BREAKABLE_END,
                    row.breakableEndTiles,
                    row.breakableEndAngles,
                )
        val mirrors =
            row.fixedMirrorTiles.zip(row.fixedMirrorAngles) { tile, angle ->
                HetFixedMirrorSpec(tile, angle)
            }
        return HetLayout(barriers, mirrors)
    }

    private fun barriers(
        type: HetBarrierType,
        tiles: List<CoordGrid>,
        angles: List<Int>,
    ): List<HetBarrierSpec> = tiles.zip(angles) { tile, angle -> HetBarrierSpec(type, tile, angle) }
}
