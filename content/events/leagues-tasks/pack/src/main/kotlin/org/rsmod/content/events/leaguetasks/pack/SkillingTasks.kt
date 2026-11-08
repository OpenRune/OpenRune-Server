package org.rsmod.content.events.leaguetasks.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.AREA
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNTER
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.STAT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE

internal fun TriggerRows.skillingTasks() {
    product("league_task_chop_some_logs", "stat.woodcutting", "obj.logs")
    product(
        "league_task_chop_100_yew_logs",
        "stat.woodcutting",
        "obj.yew_logs",
        times = 100,
        counter = "varbit.league_task_chop_yew_logs",
    )
    product("action_mine_some_clay", "stat.mining", "obj.clay")
    product(
        "league_task_mine_50_mithril_ore",
        "stat.mining",
        "obj.mithril_ore",
        times = 50,
        counter = "varbit.league_task_mine_mithril_ore",
    )
    product("league_task_catch_a_shrimp", "stat.fishing", "obj.raw_shrimp")
    product("league_task_cook_shrimp", "stat.cooking", "obj.shrimp")
    product("league_task_clean_a_grimy_guam", "stat.herblore", "obj.guam_leaf")
    product(
        "league_task_clean_25_grimy_guam_leafs",
        "stat.herblore",
        "obj.guam_leaf",
        times = 25,
        counter = "varbit.league_task_clean_grimy_guam_leafs",
    )

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

    objTask("league_task_burn_some_normal_logs", LeagueTaskTrigger.Firemaking, "obj.logs")
    objTask("league_task_burn_some_oak_logs", LeagueTaskTrigger.Firemaking, "obj.oak_logs")
    objTask(
        "league_task_burn_25_maple_logs",
        LeagueTaskTrigger.Firemaking,
        "obj.maple_logs",
        times = 25,
        counter = "varbit.league_task_burn_maple_logs",
    )

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
    objTask("league_task_bury_some_lava_dragon_bones", LeagueTaskTrigger.Bury, "obj.lava_dragon_bones")
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
    )
}

private fun TriggerRows.product(
    task: String,
    stat: String,
    vararg objs: String,
    times: Int = 1,
    counter: String? = null,
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Product) {
        columnRSCM(STAT, stat)
        columnRSCM(OBJ, objs.asList())
        times(times, counter)
    }
}

private fun TriggerRows.spell(task: String, vararg spells: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Spell) { columnRSCM(OBJ, spells.asList()) }
}

/** A [trigger] row met by any of [objs], or by any item when none are given. */
private fun TriggerRows.objTask(
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

private fun DBRowBuilder.times(times: Int, counter: String?) {
    if (times != 1) {
        column(COUNT, times)
    }
    if (counter != null) {
        columnRSCM(COUNTER, counter)
    }
}
