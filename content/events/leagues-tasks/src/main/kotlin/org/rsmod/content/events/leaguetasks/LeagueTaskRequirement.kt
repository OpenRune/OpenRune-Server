package org.rsmod.content.events.leaguetasks

import dev.openrune.types.dbcol.DbColumnCodec
import dev.openrune.types.dbcol.DbHelper
import dev.openrune.types.dbcol.column
import dev.openrune.types.dbcol.int
import dev.openrune.types.dbcol.intOptional
import dev.openrune.types.dbcol.slotsOptional
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger

/**
 * One row of `dbtable.league_task_trigger`. Read through [DbHelper] by column name rather than the
 * generated row class: the generator infers list and optional columns from the rows present, so its
 * shape would change as task data is added.
 */
public class LeagueTaskRequirement(
    public val task: Int,
    public val trigger: LeagueTaskTrigger,
    public val stats: List<String>,
    public val value: Int,
    public val count: Int,
) {
    internal companion object {
        fun loadAll(): List<LeagueTaskRequirement> =
            DbHelper.table("dbtable.league_task_trigger").map { row ->
                val trigger = row.int("dbcol.league_task_trigger:trigger")
                val stats =
                    row.slotsOptional("dbcol.league_task_trigger:stat", DbColumnCodec.StatTypeCodec)
                LeagueTaskRequirement(
                    task =
                        row.column("dbcol.league_task_trigger:task", DbColumnCodec.DbRowTypeCodec)
                            .id,
                    trigger =
                        checkNotNull(LeagueTaskTrigger.of(trigger)) {
                            "League task trigger row ${row.id} has unknown trigger $trigger."
                        },
                    stats = stats.map { it.internalName },
                    value = row.intOptional("dbcol.league_task_trigger:value") ?: 0,
                    count = row.intOptional("dbcol.league_task_trigger:count") ?: 1,
                )
            }
    }
}
