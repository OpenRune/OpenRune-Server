package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.SequenceServerType
import org.rsmod.api.table.ToaPathsRow
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class ToaPath private constructor(row: ToaPathsRow, val ordinal: Int) {
    val pathName: String = row.name
    val hudPath: Int = row.hudPath
    val levelVarbit: VarBitType = ServerCacheManager.getVarbit(row.levelVarbit)!!
    val door: CoordGrid = row.door
    val doorAngle: LocAngle = LocAngle.valueOf(row.doorAngle)
    val doorOpen: String = row.doorOpen.internalName
    val doorUnselected: String = row.doorUnselected.internalName
    val doorClosed: String = row.doorClosed.internalName
    val puzzle: ToaRoom = ToaRoom.of(row.puzzleRoom)
    val boss: ToaRoom = ToaRoom.of(row.bossRoom)
    val returnTile: CoordGrid = row.returnTile
    val returnSpreadZ: Int = row.returnSpreadZ
    val returnFacing: Direction = Direction.valueOf(row.returnFacing)
    val preloadSeqs: List<SequenceServerType> = row.preloadSeqs

    companion object {
        val entries: List<ToaPath> by lazy {
            ToaPathsRow.all().mapIndexed { index, row -> ToaPath(row, index) }
        }

        fun of(room: ToaRoom): ToaPath? = entries.firstOrNull { it.puzzle == room || it.boss == room }

        fun of(symbol: String): ToaPath {
            val rowId = ToaPathsRow.getRow(symbol).rowId
            return entries.first { it.rowId == rowId }
        }

        val SCABARAS by lazy { of("dbrow.toa_path_scabaras") }
        val HET by lazy { of("dbrow.toa_path_het") }
        val APMEKEN by lazy { of("dbrow.toa_path_apmeken") }
        val CRONDIS by lazy { of("dbrow.toa_path_crondis") }
    }

    private val rowId: Int = row.rowId
}
