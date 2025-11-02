package org.alter.skills.firemaking

import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.game.util.vars.SeqType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

object Logs {

    data class LogData(
        val logItem : Int,
        val initialTicks : Int,
        val xp : Int,
        val level : Int,
        val perLogTicks : Int,
        val animation : String
    )

    val logs: List<LogData> = table("tables.firemaking_logs").map { logTable ->

        val log = logTable.column("columns.firemaking_logs:item", ObjType)
        val initialTicks = logTable.columnOptional("columns.firemaking_logs:forester_initial_ticks", IntType) ?: 0
        val xp = logTable.column("columns.firemaking_logs:xp", IntType)
        val level = logTable.column("columns.firemaking_logs:level", IntType)
        val perLogTicks = logTable.columnOptional("columns.firemaking_logs:forester_log_ticks", IntType) ?: 4
        val animation = logTable.columnOptional("columns.firemaking_logs:forester_animation", SeqType)
            ?.let { RSCM.getReverseMapping(RSCMType.SEQTYPES, it) } ?: RSCM.NONE


        LogData(log, initialTicks,xp,level,perLogTicks,animation)
    }

}

