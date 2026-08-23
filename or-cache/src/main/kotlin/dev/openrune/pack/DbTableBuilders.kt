package dev.openrune.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import org.rsmod.map.CoordGrid

fun DBRowBuilder.columnCoord(id: Int, coord: CoordGrid) {
    column(id, coord.packed)
}
