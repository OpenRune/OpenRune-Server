package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNTER
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.MIN_HIT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.STYLE
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.TOOL
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.UNHURT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.WORN

private const val MELEE_STYLE = 1

private val KERIS_PARTISANS =
    listOf(
        "obj.keris_partisan",
        "obj.keris_partisan_breach",
        "obj.keris_partisan_corruption",
        "obj.keris_partisan_sun",
        "obj.keris_partisan_amascut",
    )

private val ELEMENTAL_SPELLS =
    listOf(
        "obj.01_wind_strike",
        "obj.05_water_strike",
        "obj.09_earth_strike",
        "obj.13_fire_strike",
        "obj.17_wind_bolt",
        "obj.23_water_bolt",
        "obj.29_earth_bolt",
        "obj.35_fire_bolt",
        "obj.41_wind_blast",
        "obj.47_water_blast",
        "obj.53_earth_blast",
        "obj.59_fire_blast",
        "obj.62_wind_wave",
        "obj.65_water_wave",
        "obj.70_earth_wave",
        "obj.75_fire_wave",
        "obj.81_wind_surge",
        "obj.85_water_surge",
        "obj.90_earth_surge",
        "obj.95_fire_surge",
    )

private val KALPHITES =
    listOf(
        "npc.kalphite_worker",
        "npc.kalphite_worker_chamber",
        "npc.kalphite_worker_strongholdcave",
        "npc.kalphite_soldier",
        "npc.kalphite_soldier_strongholdcave",
        "npc.kalphite_lord",
        "npc.kalphite_lord_chamber",
        "npc.kalphite_lord_strongholdcave",
        "npc.kalphite_larva",
        "npc.kalphite_queen",
        "npc.kalphite_flyingqueen",
        "npc.league_6_kalphite_queen",
        "npc.league_6_kalphite_flyingqueen",
    )

private val ABYSSAL_DEMONS =
    listOf(
        "npc.slayer_abyssal",
        "npc.slayer_abyssal_strongholdcave",
        "npc.kourend_abyssal",
        "npc.wild_cave_abyssal",
    )

internal fun TriggerRows.conditionalCombatTasks() {
    kill(
        "league_task_kill_a_kalphite_with_the_keris_partisan",
        *KALPHITES.toTypedArray(),
        tools = KERIS_PARTISANS,
    )
    trigger("dbrow.league_task_kill_a_oryx_with_melee", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC, listOf("npc.varlamore_oryx"))
        column(STYLE, MELEE_STYLE)
    }
    trigger("dbrow.league_task_kill_a_jaguar_without_taking_any_damage", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC, listOf("npc.varlamore_jaguar"))
        column(UNHURT, 1)
    }
    trigger("dbrow.league_task_kill_a_jubster_with_beret", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC, listOf("npc.tol_jubster"))
        columnRSCM(
            WORN,
            listOf("obj.berret_black", "obj.berret_blue", "obj.berret_white", "obj.berret_red"),
        )
    }
    trigger("dbrow.league_task_kill_a_rock_crab_one_hit", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC, listOf("npc.horror_rockcrab", "npc.horror_rockcrab_small"))
        column(MIN_HIT, 50)
    }

    kill(
        "league_task_defeat_an_elf_in_tirannwn",
        "npc.mourning_guard_melee",
        "npc.mourning_guard_melee2",
        "npc.mourning_guard_range",
        "npc.mourning_guard_range2",
        counter = "varbit.league_counter_kill_elves_in_tirannwn",
        area = "area.league_region_tirannwn",
    )
    kill(
        "league_task_defeat_a_tortoise_with_riders_in_kandarin",
        "npc.battle_tortoise",
        area = "area.league_region_kandarin",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_mossgiant",
        counter = "varbit.league_killed_moss_giant_kourend",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_firegiant1",
        "npc.kourend_firegiant2",
        counter = "varbit.league_killed_fire_giant_kourend",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_hillgiant",
        counter = "varbit.league_killed_hill_giant_kourend",
    )

    trigger("dbrow.league_task_slay_250_creatures", LeagueTaskTrigger.SlayerTaskKill) {
        column(COUNT, 250)
        columnRSCM(COUNTER, "varbit.league_counter_slay_task_creatures")
    }
    trigger("dbrow.league_task_slay_an_abyssal_demon", LeagueTaskTrigger.SlayerTaskKill) {
        columnRSCM(NPC, ABYSSAL_DEMONS)
    }

    trigger("dbrow.league_task_hit_150_with_the_keris_partisan", LeagueTaskTrigger.Damage) {
        column(VALUE, 150)
        columnRSCM(TOOL, KERIS_PARTISANS)
    }
    trigger("dbrow.league_task_deal_66_damage_with_elemental_spell", LeagueTaskTrigger.Damage) {
        column(VALUE, 66)
        columnRSCM(TOOL, ELEMENTAL_SPELLS)
    }
}
