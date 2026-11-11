package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

internal enum class HetDir(val dx: Int, val dz: Int) {
    West(-1, 0),
    North(0, 1),
    East(1, 0),
    South(0, -1);

    val opposite: HetDir
        get() = entries[(ordinal + 2) % entries.size]

    val vertical: Boolean
        get() = dz != 0

    companion object {
        fun of(dx: Int, dz: Int): HetDir = entries.first { it.dx == dx && it.dz == dz }
    }
}

internal sealed interface HetCell {
    data object Open : HetCell

    data object Wall : HetCell

    data object Target : HetCell

    data class Mirror(val rotation: Int) : HetCell
}

internal fun interface HetField {
    fun cell(x: Int, z: Int): HetCell
}

internal enum class HetBeamGfx {
    Horizontal,
    Vertical,
    NorthEast,
    SouthEast,
    SouthWest,
    NorthWest,
}

internal class HetBeamTile(val x: Int, val z: Int, val gfx: HetBeamGfx)

internal class HetBeamPath(
    val tiles: List<HetBeamTile>,
    val endX: Int,
    val endZ: Int,
    val reachedTarget: Boolean,
)

internal object HetBeam {
    const val MAX_LENGTH = 200

    fun reflect(dir: HetDir, rotation: Int): HetDir =
        if (rotation % 2 == 0) {
            HetDir.of(-dir.dz, -dir.dx)
        } else {
            HetDir.of(dir.dz, dir.dx)
        }

    fun trace(field: HetField, startX: Int, startZ: Int, start: HetDir): HetBeamPath {
        val tiles = ArrayList<HetBeamTile>()
        var x = startX
        var z = startZ
        var dir = start
        repeat(MAX_LENGTH) {
            when (val cell = field.cell(x, z)) {
                HetCell.Open -> tiles += HetBeamTile(x, z, straight(dir))
                is HetCell.Mirror -> {
                    val out = reflect(dir, cell.rotation)
                    tiles += HetBeamTile(x, z, corner(dir, out))
                    dir = out
                }
                HetCell.Wall -> return HetBeamPath(tiles, x, z, reachedTarget = false)
                HetCell.Target -> return HetBeamPath(tiles, x, z, reachedTarget = true)
            }
            x += dir.dx
            z += dir.dz
        }
        return HetBeamPath(tiles, x, z, reachedTarget = false)
    }

    private fun straight(dir: HetDir): HetBeamGfx =
        if (dir.vertical) HetBeamGfx.Vertical else HetBeamGfx.Horizontal

    private fun corner(from: HetDir, to: HetDir): HetBeamGfx {
        val arms = listOf(from.opposite, to)
        val north = arms.any { it == HetDir.North }
        val east = arms.any { it == HetDir.East }
        return when {
            north && east -> HetBeamGfx.NorthEast
            north -> HetBeamGfx.NorthWest
            east -> HetBeamGfx.SouthEast
            else -> HetBeamGfx.SouthWest
        }
    }
}
