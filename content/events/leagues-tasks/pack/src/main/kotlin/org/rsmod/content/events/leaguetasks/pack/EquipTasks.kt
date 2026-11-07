package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ

/**
 * Equip tasks whose `action` row has no `equip_item` list of its own. Tasks with one are read from
 * Jagex's list directly and need no row here.
 */
internal fun TriggerRows.equipTasks() {
    equip(
        "league_task_equip_an_iron_dagger",
        "obj.iron_dagger",
        "obj.iron_dagger_p",
        "obj.iron_dagger_p+",
        "obj.iron_dagger_p++",
    )
    equip(
        "league_task_equip_an_elemental_staff",
        "obj.staff_of_air",
        "obj.staff_of_water",
        "obj.staff_of_earth",
        "obj.staff_of_fire",
    )
}

/** Met when [worn] of [objs] are worn at once; a full set lists one item per slot. */
internal fun TriggerRows.equip(task: String, vararg objs: String, worn: Int = 1) {
    trigger("dbrow.$task", LeagueTaskTrigger.Equip) {
        columnRSCM(OBJ, objs.asList())
        if (worn != 1) {
            column(COUNT, worn)
        }
    }
}
