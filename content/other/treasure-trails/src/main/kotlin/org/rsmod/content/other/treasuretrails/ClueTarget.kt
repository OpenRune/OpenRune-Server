package org.rsmod.content.other.treasuretrails

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.dbcol.DbHelper
import org.rsmod.api.table.cluehelper.CluehelperTargetCoordRow
import org.rsmod.api.table.cluehelper.CluehelperTargetKeyRow
import org.rsmod.api.table.cluehelper.CluehelperTargetKillRow
import org.rsmod.api.table.cluehelper.CluehelperTargetLocRow
import org.rsmod.api.table.cluehelper.CluehelperTargetMapzoneRow
import org.rsmod.api.table.cluehelper.CluehelperTargetNpcRow

sealed interface ClueTarget {
    data class TalkTo(val row: CluehelperTargetNpcRow) : ClueTarget

    data class Search(val row: CluehelperTargetLocRow) : ClueTarget

    data class Dig(val row: CluehelperTargetCoordRow) : ClueTarget

    data class StandIn(val row: CluehelperTargetMapzoneRow) : ClueTarget

    data class KillForKey(val row: CluehelperTargetKillRow) : ClueTarget

    data class OpenWithKey(val row: CluehelperTargetKeyRow) : ClueTarget

    companion object {
        fun of(rowId: Int): ClueTarget =
            when (val table = dbTableOf(rowId)) {
                "dbtable.cluehelper_target_npc" -> TalkTo(CluehelperTargetNpcRow.getRow(rowId))
                "dbtable.cluehelper_target_loc" -> Search(CluehelperTargetLocRow.getRow(rowId))
                "dbtable.cluehelper_target_coord" -> Dig(CluehelperTargetCoordRow.getRow(rowId))
                "dbtable.cluehelper_target_mapzone" ->
                    StandIn(CluehelperTargetMapzoneRow.getRow(rowId))
                "dbtable.cluehelper_target_kill" ->
                    KillForKey(CluehelperTargetKillRow.getRow(rowId))
                "dbtable.cluehelper_target_key" ->
                    OpenWithKey(CluehelperTargetKeyRow.getRow(rowId))
                else -> error("Row $rowId in $table is not a clue target.")
            }
    }
}

internal fun dbTableOf(rowId: Int): String =
    RSCM.getReverseMapping(RSCMType.DBTABLE, DbHelper.row(rowId).tableId)
