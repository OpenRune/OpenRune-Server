package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ

private val EARTH_SPELLS =
    listOf(
        "obj.09_earth_strike",
        "obj.29_earth_bolt",
        "obj.53_earth_blast",
        "obj.70_earth_wave",
        "obj.90_earth_surge",
    )

private val MACES =
    listOf(
        "obj.bronze_mace",
        "obj.iron_mace",
        "obj.steel_mace",
        "obj.black_mace",
        "obj.white_mace",
        "obj.mithril_mace",
        "obj.adamant_mace",
        "obj.rune_mace",
        "obj.dragon_mace",
        "obj.pest_void_knight_mace",
        "obj.ancient_goblin_mace",
        "obj.inquisitors_mace",
        "obj.barronite_mace",
    )

internal fun TriggerRows.combatActionTasks() {
    variable("league_task_use_the_protect_from_melee_prayer", "varbit.prayer_protectfrommelee", 1)
    trigger("dbrow.league_task_perform_a_special_attack", LeagueTaskTrigger.SpecialAttack)
    trigger(
        "dbrow.league_task_use_the_special_attack_of_a_dragon_axe",
        LeagueTaskTrigger.SpecialAttack,
    ) {
        columnRSCM(
            OBJ,
            listOf(
                "obj.dragon_axe",
                "obj.trailblazer_axe_no_infernal",
                "obj.trailblazer_reloaded_axe_no_infernal",
                "obj.dragon_axe_2h",
            ),
        )
    }
    trigger("dbrow.league_task_shoot_6_iron_arrows", LeagueTaskTrigger.Ammo) {
        columnRSCM(
            OBJ,
            listOf("obj.iron_arrow", "obj.iron_arrow_p", "obj.iron_arrow_p+", "obj.iron_arrow_p++"),
        )
        times(6, "varbit.league_task_iron_arrows_shot")
    }

    kill(
        "league_task_kill_an_imp_with_an_earth_spell",
        "npc.godwars_ancient_imp",
        "npc.imp",
        "npc.castlewars_imp",
        "npc.yama_imp",
        "npc.chasm_smithing_imp_noname",
        "npc.magic_hunter_npc",
        "npc.imp_notele",
        "npc.ic_basement_imp",
        "npc.minion_imp_chathead",
        "npc.league6_imp_relic_carry_1",
        "npc.league6_imp_relic_carry_2",
        "npc.league6_imp_relic_carry_3",
        "npc.league6_imp_relic_carry_4",
        "npc.league6_imp_reading_1",
        "npc.league6_imp_reading_2",
        "npc.league6_imp_reading_3",
        "npc.league6_imp_jumping_1",
        "npc.league6_imp_jumping_2",
        "npc.league6_imp_jumping_3",
        "npc.tfa_dagonhai_demon",
        tools = EARTH_SPELLS,
    )
    kill(
        "league_task_kill_a_scorpion_with_a_mithril_spear",
        "npc.sos_pest_scorpion",
        "npc.sos_pest_scorpion2",
        "npc.scorpion",
        "npc.mm_jungle_scorpion",
        "npc.varlamore_scorpion_savannah",
        "npc.wandering_doomscorpion",
        "npc.tinyscorpion",
        tools =
            listOf(
                "obj.mithril_spear",
                "obj.mithril_spear_p",
                "obj.mithril_spear_p+",
                "obj.mithril_spear_p++",
                "obj.tbwt_mithril_spear_kp",
            ),
    )
    kill(
        "league_task_kill_5_creatures_with_a_mace",
        times = 5,
        counter = "varbit.league_task_creatures_with_mace",
        tools = MACES,
    )
}
