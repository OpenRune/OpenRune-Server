package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.types.dbcol.CategoryIdCodec
import dev.openrune.types.dbcol.DbColumnCodec
import dev.openrune.types.dbcol.DbHelper
import dev.openrune.types.dbcol.column
import dev.openrune.types.dbcol.int
import dev.openrune.types.dbcol.intOptional
import dev.openrune.types.dbcol.slotsOptional
import org.rsmod.api.table.ActionRow
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger

/** One thing a task needs; a task completes once all of its requirements are met. */
public class LeagueTaskRequirement(
    public val task: Int,
    public val trigger: LeagueTaskTrigger,
    public val stats: List<String> = emptyList(),
    public val value: Int = 0,
    public val count: Int = 1,
    public val objs: List<Int> = emptyList(),
    public val npcs: List<Int> = emptyList(),
    public val categories: List<Int> = emptyList(),
    public val slayerTask: Int? = null,
    public val counter: VarBitType? = null,
    /** Where an event must happen; empty means anywhere. */
    public val areas: List<Int> = emptyList(),
) {
    internal companion object {
        /**
         * Rows of `dbtable.league_task_trigger`, plus a requirement read from the `action` row of
         * every task in [tasks] that has no rows but where Jagex filled in what counts.
         */
        fun loadAll(tasks: Collection<Int>): List<LeagueTaskRequirement> {
            val defined = triggerRows()
            val described = defined.mapTo(HashSet()) { it.task }
            return defined + tasks.filter { it !in described }.mapNotNull(::fromAction)
        }

        /**
         * Read through [DbHelper] by column name rather than the generated row class: the generator
         * infers list and optional columns from the rows present, so its shape would change as task
         * data is added.
         */
        private fun triggerRows(): List<LeagueTaskRequirement> =
            DbHelper.table("dbtable.league_task_trigger").map { row ->
                val trigger = row.int("dbcol.league_task_trigger:trigger")
                val stats =
                    row.slotsOptional("dbcol.league_task_trigger:stat", DbColumnCodec.StatTypeCodec)
                val objs =
                    row.slotsOptional(
                        "dbcol.league_task_trigger:obj",
                        DbColumnCodec.ItemServerTypeCodec,
                    )
                val npcs =
                    row.slotsOptional("dbcol.league_task_trigger:npc", DbColumnCodec.NpcTypeCodec)
                val categories =
                    row.slotsOptional("dbcol.league_task_trigger:category", CategoryIdCodec)
                val counter =
                    row.intOptional("dbcol.league_task_trigger:counter")?.let { id ->
                        checkNotNull(ServerCacheManager.getVarbit(id)) {
                            "League task trigger row ${row.id} has unknown counter varbit $id."
                        }
                    }
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
                    objs = objs.map { it.id },
                    npcs = npcs.map { it.id },
                    categories = categories,
                    slayerTask = row.intOptional("dbcol.league_task_trigger:slayer_task"),
                    counter = counter,
                    areas =
                        row.slotsOptional(
                                "dbcol.league_task_trigger:area",
                                DbColumnCodec.AreaTypeCodec,
                            )
                            .map { it.id },
                )
            }

        private fun fromAction(task: Int): LeagueTaskRequirement? {
            val action = ActionRow.getRow(task)
            if (action.equipItem.isNotEmpty()) {
                val objs = action.equipItem.map { it.id }
                return LeagueTaskRequirement(task, LeagueTaskTrigger.Equip, objs = objs)
            }
            val slayerTask = action.npcKillSlayerCategory
            val namesNpcs = action.npcKill.isNotEmpty() || action.npcKillCategory.isNotEmpty()
            if (namesNpcs || slayerTask != null) {
                return LeagueTaskRequirement(
                    task,
                    LeagueTaskTrigger.Kill,
                    npcs = action.npcKill.map { it.id },
                    categories = action.npcKillCategory,
                    slayerTask = slayerTask,
                )
            }
            return null
        }
    }
}
