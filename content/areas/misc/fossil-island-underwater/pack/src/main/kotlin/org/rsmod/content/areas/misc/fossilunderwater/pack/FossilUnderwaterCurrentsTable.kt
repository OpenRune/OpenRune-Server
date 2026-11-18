package org.rsmod.content.areas.misc.fossilunderwater.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import org.rsmod.map.CoordGrid

/**
 * The tiles a current carries a player off, each with the tile it carries them onto. The current
 * locs only draw the streams; the tiles are the ones live was seen pushing a player from.
 */
object FossilUnderwaterCurrentsTable {
    const val PUSHES = 0

    fun currents() =
        dbTable("dbtable.fossil_underwater_currents", serverOnly = true) {
            column("pushes", PUSHES, VarType.COORDGRID, VarType.COORDGRID)

            // TODO(content): live pushes from more tiles than these; the rest of the streams.
            row("dbrow.fossil_underwater_currents") {
                column(
                    PUSHES,
                    listOf(
                            west(3773, 10291),
                            west(3779, 10297),
                            east(3784, 10259),
                            east(3785, 10259),
                            west(3785, 10294),
                            east(3786, 10259),
                            east(3792, 10274),
                            east(3794, 10291),
                            east(3795, 10259),
                            east(3795, 10261),
                            east(3796, 10250),
                            north(3796, 10266),
                            north(3796, 10267),
                            east(3796, 10268),
                            east(3797, 10266),
                            east(3797, 10267),
                            north(3798, 10266),
                            east(3798, 10267),
                            west(3800, 10258),
                            north(3800, 10259),
                            south(3802, 10257),
                            south(3802, 10258),
                            north(3803, 10268),
                            south(3804, 10259),
                            west(3804, 10260),
                            south(3805, 10283),
                            south(3806, 10282),
                            south(3813, 10279),
                            south(3813, 10280),
                            south(3815, 10280),
                            east(3832, 10273),
                            west(3832, 10286),
                            west(3833, 10287),
                            south(3834, 10288),
                        )
                        .flatten(),
                )
            }
        }

    private fun north(x: Int, z: Int): List<Int> = push(x, z, 0, 1)

    private fun east(x: Int, z: Int): List<Int> = push(x, z, 1, 0)

    private fun south(x: Int, z: Int): List<Int> = push(x, z, 0, -1)

    private fun west(x: Int, z: Int): List<Int> = push(x, z, -1, 0)

    private fun push(x: Int, z: Int, dx: Int, dz: Int): List<Int> =
        listOf(CoordGrid(x, z, LEVEL).packed, CoordGrid(x + dx, z + dz, LEVEL).packed)

    private const val LEVEL = 1
}
