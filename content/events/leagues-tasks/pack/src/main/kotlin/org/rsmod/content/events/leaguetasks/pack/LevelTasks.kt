package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.STAT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE

private val MELEE_RANGED_MAGIC =
    listOf(
        "stat.attack",
        "stat.strength",
        "stat.defence",
        "stat.magic",
        "stat.ranged",
        "stat.hitpoints",
    )

private val COMBAT = MELEE_RANGED_MAGIC + "stat.prayer"

private val NON_COMBAT =
    listOf(
        "stat.cooking",
        "stat.woodcutting",
        "stat.fletching",
        "stat.fishing",
        "stat.firemaking",
        "stat.crafting",
        "stat.smithing",
        "stat.mining",
        "stat.herblore",
        "stat.agility",
        "stat.thieving",
        "stat.slayer",
        "stat.farming",
        "stat.runecrafting",
        "stat.hunter",
        "stat.construction",
    )

private val ALL = COMBAT + NON_COMBAT

/** Skill name as it appears in the task dbrow names, which say `runecraft` for Runecrafting. */
private val TASK_SKILL_NAMES =
    ALL.associateWith { it.removePrefix("stat.").replace("runecrafting", "runecraft") }

internal fun TriggerRows.levelTasks() {
    anyLevel("league_task_achieve_your_first_level_up", 2, ALL - "stat.hitpoints")
    anyLevel(
        "league_task_achieve_your_first_level_5",
        5,
        ALL - listOf("stat.agility", "stat.hitpoints", "stat.runecrafting"),
    )
    anyLevel(
        "league_task_achieve_your_first_level_10",
        10,
        ALL - listOf("stat.agility", "stat.hitpoints"),
    )
    for (level in listOf(20, 30, 40, 50, 60, 70, 80, 90, 95)) {
        anyLevel("league_task_achieve_your_first_level_$level", level, ALL)
    }
    for (stat in ALL) {
        anyLevel("league_task_reach_level_99_${TASK_SKILL_NAMES.getValue(stat)}", 99, listOf(stat))
    }

    for (level in listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95)) {
        valueTask("league_task_reach_base_level_$level", LeagueTaskTrigger.BaseLevel, level)
    }
    for (level in listOf(100, 250, 750, 1000, 1250, 1500, 1750, 2000, 2100, 2200, 2277)) {
        valueTask("league_task_reach_total_level_$level", LeagueTaskTrigger.TotalLevel, level)
    }
    valueTask("action_total_level_666", LeagueTaskTrigger.TotalLevel, 666)
    for (level in listOf(25, 50, 75, 100, 110, 120, 126)) {
        valueTask("league_task_reach_combat_level_$level", LeagueTaskTrigger.CombatLevel, level)
    }

    for (stat in NON_COMBAT) {
        xp("league_task_obtain_25_million_${TASK_SKILL_NAMES.getValue(stat)}_xp", 25, listOf(stat))
        xp("league_task_obtain_35_million_${TASK_SKILL_NAMES.getValue(stat)}_xp", 35, listOf(stat))
    }
    xp("league_task_obtain_25_million_prayer_xp", 25, listOf("stat.prayer"))
    xp("league_task_obtain_35_million_prayer_xp", 35, listOf("stat.prayer"))
    for (stat in ALL) {
        xp("league_task_obtain_50_million_${TASK_SKILL_NAMES.getValue(stat)}_xp", 50, listOf(stat))
    }
    xp("league_task_obtain_25_million_xp_combat_skill", 25, COMBAT)
    xp("league_task_obtain_35_million_xp_combat_skill", 35, MELEE_RANGED_MAGIC)
    xp("league_task_obtain_100_million_xp_combat_skill", 100, COMBAT)
    xp("league_task_obtain_200_million_xp_combat_skill", 200, MELEE_RANGED_MAGIC)
    xp("league_task_obtain_100_million_xp_noncombat_skill", 100, NON_COMBAT)
    xp("league_task_obtain_200_million_xp_noncombat_skill", 200, NON_COMBAT)
    xp("league_task_obtain_25_million_xp_5_noncombat_skills", 25, NON_COMBAT, skills = 5)
    xp("league_task_obtain_35_million_xp_3_noncombat_skills", 35, NON_COMBAT, skills = 3)
    xp("league_task_obtain_50_million_xp_3_noncombat_skills", 50, NON_COMBAT + "stat.prayer", skills = 3)
}

private fun TriggerRows.anyLevel(task: String, level: Int, stats: List<String>) {
    trigger("dbrow.$task", LeagueTaskTrigger.Level) {
        columnRSCM(STAT, stats)
        column(VALUE, level)
    }
}

private fun TriggerRows.valueTask(task: String, trigger: LeagueTaskTrigger, value: Int) {
    trigger("dbrow.$task", trigger) { column(VALUE, value) }
}

private fun TriggerRows.xp(task: String, millions: Int, stats: List<String>, skills: Int = 1) {
    trigger("dbrow.$task", LeagueTaskTrigger.Xp) {
        columnRSCM(STAT, stats)
        column(VALUE, millions * 1_000_000)
        column(COUNT, skills)
    }
}
