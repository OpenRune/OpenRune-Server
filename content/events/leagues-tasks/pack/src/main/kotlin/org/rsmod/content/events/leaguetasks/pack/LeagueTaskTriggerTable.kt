package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * How each league task completes. One row per requirement: a task completes once every row for its
 * `action` dbrow is met. Rows describe the task, not a league, so every league listing the task
 * shares them.
 */
object LeagueTaskTriggerTable {
    const val TASK = 0
    const val TRIGGER = 1
    const val STAT = 2
    const val VALUE = 3
    const val COUNT = 4
    const val OBJ = 5
    const val NPC = 6
    const val CATEGORY = 7
    const val SLAYER_TASK = 8
    const val COUNTER = 9
    const val AREA = 10
    const val TOOL = 11
    const val VARP = 12
    const val VARBIT = 13
    const val INTERFACE = 14

    fun triggers() =
        dbTable("dbtable.league_task_trigger", serverOnly = true) {
            column("task", TASK, VarType.DBROW)
            column("trigger", TRIGGER, VarType.INT)
            column("stat", STAT, VarType.STAT)
            column("value", VALUE, VarType.INT)
            column("count", COUNT, VarType.INT)
            column("obj", OBJ, VarType.OBJ)
            column("npc", NPC, VarType.NPC)
            column("category", CATEGORY, VarType.CATEGORY)
            column("slayer_task", SLAYER_TASK, VarType.INT)
            column("counter", COUNTER, VarType.INT)
            column("area", AREA, VarType.AREA)
            column("tool", TOOL, VarType.OBJ)
            column("varp", VARP, VarType.INT)
            column("varbit", VARBIT, VarType.INT)
            column("interface", INTERFACE, VarType.INT)

            val rows = TriggerRows(this)
            rows.levelTasks()
            rows.equipTasks()
            rows.killTasks()
            rows.skillingTasks()
            rows.woodcuttingTasks()
            rows.miningTasks()
            rows.fishingTasks()
            rows.cookingTasks()
            rows.firemakingTasks()
            rows.herbloreTasks()
            rows.smithingTasks()
            rows.craftingTasks()
            rows.questTasks()
            rows.bossTasks()
            rows.combatKillTasks()
            rows.gearTasks()
            rows.setTasks()
            rows.worldTasks()
            rows.itemTasks()
        }
}

/**
 * Hands out row ids from the block reserved for this table. Nothing refers to a trigger row by id,
 * so the rows need no gameval symbols.
 */
class TriggerRows(private val table: DBTableBuilder) {
    private var next = FIRST_ROW

    fun trigger(task: String, trigger: LeagueTaskTrigger, block: DBRowBuilder.() -> Unit = {}) {
        check(next <= LAST_ROW) { "dbtable.league_task_trigger ran out of row ids at $LAST_ROW." }
        table.row(next++) {
            columnRSCM(LeagueTaskTriggerTable.TASK, task)
            column(LeagueTaskTriggerTable.TRIGGER, trigger.id)
            block()
        }
    }

    private companion object {
        const val FIRST_ROW = 40000
        const val LAST_ROW = 42999
    }
}
