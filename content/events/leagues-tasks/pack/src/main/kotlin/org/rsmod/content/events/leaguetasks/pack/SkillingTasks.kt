package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.AREA
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNTER
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.STAT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.TOOL
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE

internal fun TriggerRows.skillingTasks() {
    spell(
        "league_task_cast_home_teleport",
        "obj.48_home_teleport",
        "obj.01_zaros_home_tele",
        "obj.01_lunar_home_tele",
        "obj.deadman_level99_lamp",
        "obj.league_6_home_teleport_standard",
        "obj.league_6_home_teleport_ancient",
        "obj.league_6_home_teleport_lunar",
        "obj.league_6_home_teleport_arceuus",
    )
    spell("action_cast_low_level_alchemy", "obj.21_low_alchemy")
    trigger("dbrow.action_alchemy_500", LeagueTaskTrigger.Alchemy) { column(VALUE, 500) }

    objTask(
        "league_task_bury_6_bones",
        LeagueTaskTrigger.Bury,
        times = 6,
        counter = "varbit.league_task_bones_counter",
    )
    objTask(
        "league_task_bury_some_wyvern_or_dragon_bones",
        LeagueTaskTrigger.Bury,
        "obj.wyvern_bones",
        "obj.dragon_bones",
    )
    objTask(
        "league_task_bury_some_lava_dragon_bones",
        LeagueTaskTrigger.Bury,
        "obj.lava_dragon_bones",
    )
    objTask(
        "league_task_sacrifice_some_bones_at_the_chaos_temple",
        LeagueTaskTrigger.Offer,
        area = "area.chaos_temple",
    )
    objTask(
        "league_task_sacrifice_some_dragon_bones_at_the_chaos_temple",
        LeagueTaskTrigger.Offer,
        "obj.dragon_bones",
        area = "area.chaos_temple",
    )

    objTask("league_task_eat_an_onion", LeagueTaskTrigger.Consume, "obj.onion")
    objTask("league_task_eat_a_rabbit", LeagueTaskTrigger.Consume, "obj.cooked_rabbit")
    objTask(
        "league_task_drink_a_strength_potion",
        LeagueTaskTrigger.Consume,
        "obj.strength4",
        "obj.3dose1strength",
        "obj.2dose1strength",
        "obj.1dose1strength",
        "obj.4dosemoonlightpotion",
        "obj.3dosemoonlightpotion",
        "obj.2dosemoonlightpotion",
        "obj.1dosemoonlightpotion",
    )
    objTask("league_task_bury_ourg_bone", LeagueTaskTrigger.Bury, "obj.zogre_ancestral_bones_ourg")
}

/**
 * Met by making one of [objs] (any product of [stat] when none are given) with any of [tools], in
 * any of [areas].
 */
internal fun TriggerRows.product(
    task: String,
    stat: String,
    vararg objs: String,
    times: Int = 1,
    counter: String? = null,
    tools: List<String> = emptyList(),
    areas: List<String> = emptyList(),
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Product) {
        columnRSCM(STAT, stat)
        if (objs.isNotEmpty()) {
            columnRSCM(OBJ, objs.asList())
        }
        if (tools.isNotEmpty()) {
            columnRSCM(TOOL, tools)
        }
        if (areas.isNotEmpty()) {
            columnRSCM(AREA, areas)
        }
        times(times, counter)
    }
}

internal fun TriggerRows.spell(task: String, vararg spells: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Spell) { columnRSCM(OBJ, spells.asList()) }
}

/** A [trigger] row met by any of [objs], or by any item when none are given. */
internal fun TriggerRows.objTask(
    task: String,
    trigger: LeagueTaskTrigger,
    vararg objs: String,
    times: Int = 1,
    counter: String? = null,
    area: String? = null,
) {
    trigger("dbrow.$task", trigger) {
        if (objs.isNotEmpty()) {
            columnRSCM(OBJ, objs.asList())
        }
        if (area != null) {
            columnRSCM(AREA, area)
        }
        times(times, counter)
    }
}

internal fun DBRowBuilder.times(times: Int, counter: String?) {
    if (times != 1) {
        column(COUNT, times)
    }
    if (counter != null) {
        columnRSCM(COUNTER, counter)
    }
}
