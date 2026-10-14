package org.rsmod.content.raids.toa.raid.encounter.wardens

import org.rsmod.api.table.ToaWardensObeliskPatternsRow
import org.rsmod.map.CoordGrid

internal object WardensObeliskPatterns {
    val beamSweepA: List<CoordGrid> by lazy { tiles("dbrow.toa_wardens_beam_sweep_a") }
    val beamSweepB: List<CoordGrid> by lazy { tiles("dbrow.toa_wardens_beam_sweep_b") }

    val windmill: List<List<CoordGrid>> by lazy {
        (0 until WINDMILL_SETS).map { tiles("dbrow.toa_wardens_windmill_$it") }
    }

    private fun tiles(row: String): List<CoordGrid> = ToaWardensObeliskPatternsRow.getRow(row).tiles

    private const val WINDMILL_SETS = 5
}
