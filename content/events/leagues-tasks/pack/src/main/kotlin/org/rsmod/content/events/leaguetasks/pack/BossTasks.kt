package org.rsmod.content.events.leaguetasks.pack

private val GODWARS_BOSSES =
    arrayOf(
        "npc.godwars_bandos_avatar",
        "npc.godwars_armadyl_avatar",
        "npc.godwars_saradomin_avatar",
        "npc.godwars_zamorak_avatar",
        "npc.nex",
        "npc.nex_deflect",
        "npc.nex_dying",
    )

/** Boss tasks counted by Jagex's own kill count varps, so they also see kills from before. */
internal fun TriggerRows.bossTasks() {
    killCount("league_task_1_tempoross_kill", "tempoross", 1)
    killCount("league_task_10_tempoross_kills", "tempoross", 10)
    killCount("league_task_25_tempoross_kills", "tempoross", 25)
    killCount("league_task_1_wintertodt_kill", "wintertodt", 1)
    killCount("league_task_10_wintertodt_kills", "wintertodt", 10)
    killCount("league_task_25_wintertodt_kills", "wintertodt", 25)
    killCount("league_task_50_wintertodt_kills", "wintertodt", 50)
    killCount("league_task_1_amoxliatl_kill", "amoxliatl", 1)
    killCount("league_task_50_amoxliatl_kills", "amoxliatl", 50)
    killCount("league_task_1_hueycoatl_kill", "huey", 1)
    killCount("league_task_50_hueycoatl_kills", "huey", 50)
    killCount("league_task_1_sarachnis_kill", "sarachnis", 1)
    killCount("league_task_50_sarachnis_kills", "sarachnis", 50)
    killCount("league_task_150_sarachnis_kills", "sarachnis", 150)
    killCount("league_task_300_sarachnis_kills", "sarachnis", 300)
    killCount("league_task_defeat_the_giant_mole_50_times", "mole", 50)
    killCount("league_task_defeat_the_giant_mole_150_times", "mole", 150)
    killCount("league_task_defeat_royal_titans_50_times", "royal_titan", 50)
    killCount("league_task_defeat_vardorvis_50_times", "vardorvis", 50)
    killCount("league_task_defeat_vardorvis_150_times", "vardorvis", 150)
    killCount("league_task_defeat_vardorvis_300_times", "vardorvis", 300)
    killCount("league_task_1_araxxor_kill", "araxxor", 1)
    killCount("league_task_50_araxxor_kills", "araxxor", 50)
    killCount("league_task_150_araxxor_kills", "araxxor", 150)
    killCount("league_task_300_araxxor_kills", "araxxor", 300)
    killCount("league_task_defeat_callisto_50_times", "callisto", 50)
    killCount("league_task_defeat_callisto_150_times", "callisto", 150)
    killCount("league_task_defeat_callisto_300_times", "callisto", 300)
    killCount("league_task_defeat_cerberus_50_times", "cerberus", 50)
    killCount("league_task_defeat_cerberus_150_times", "cerberus", 150)
    killCount("league_task_defeat_cerberus_300_times", "cerberus", 300)
    killCount("league_task_defeat_duke_sucellus_50_times", "duke_sucellus", 50)
    killCount("league_task_defeat_duke_sucellus_150_times", "duke_sucellus", 150)
    killCount("league_task_defeat_duke_sucellus_300_times", "duke_sucellus", 300)
    killCount("league_task_defeat_leviathan_50_times", "leviathan", 50)
    killCount("league_task_defeat_leviathan_150_times", "leviathan", 150)
    killCount("league_task_defeat_leviathan_300_times", "leviathan", 300)
    killCount("league_task_defeat_nex_50_times", "nex", 50)
    killCount("league_task_defeat_nex_100_times", "nex", 100)
    killCount("league_task_defeat_nex_200_times", "nex", 200)
    killCount("league_task_defeat_phantom_muspah_50_times", "muspah", 50)
    killCount("league_task_defeat_phantom_muspah_150_times", "muspah", 150)
    killCount("league_task_defeat_phantom_muspah_300_times", "muspah", 300)
    killCount("league_task_1_skotizo_kill", "cata_boss", 1)
    killCount("league_task_defeat_the_nightmare_25_times", "nightmare", 25)
    killCount("league_task_defeat_the_nightmare_50_times", "nightmare", 50)
    killCount("league_task_defeat_the_nightmare_150_times", "nightmare", 150)
    killCount("league_task_defeat_venenatis_50_times", "venenatis", 50)
    killCount("league_task_defeat_venenatis_150_times", "venenatis", 150)
    killCount("league_task_defeat_venenatis_300_times", "venenatis", 300)
    killCount("league_task_defeat_vetion_50_times", "vetion", 50)
    killCount("league_task_defeat_vetion_150_times", "vetion", 150)
    killCount("league_task_defeat_vetion_300_times", "vetion", 300)
    killCount("league_task_defeat_vorkath_50_times", "vorkath", 50)
    killCount("league_task_defeat_vorkath_150_times", "vorkath", 150)
    killCount("league_task_defeat_vorkath_300_times", "vorkath", 300)
    killCount("league_task_defeat_whisperer_50_times", "whisperer", 50)
    killCount("league_task_defeat_whisperer_150_times", "whisperer", 150)
    killCount("league_task_defeat_whisperer_300_times", "whisperer", 300)
    killCount("league_task_1_yama_kill", "yama", 1)
    killCount("league_task_50_yama_kills", "yama", 50)
    killCount("league_task_150_yama_kills", "yama", 150)
    killCount("league_task_defeat_zalcano_50_times", "zalcano", 50)
    killCount("league_task_defeat_zalcano_100_times", "zalcano", 100)
    killCount("league_task_defeat_zulrah_50_times", "snakeboss", 50)
    killCount("league_task_defeat_zulrah_150_times", "snakeboss", 150)
    killCount("league_task_defeat_zulrah_300_times", "snakeboss", 300)
    killCount("league_task_1_alchemical_hydra_kill", "hydraboss", 1)
    killCount("league_task_50_alchemical_hydra_kills", "hydraboss", 50)
    killCount("league_task_150_alchemical_hydra_kills", "hydraboss", 150)
    killCount("league_task_300_alchemical_hydra_kills", "hydraboss", 300)
    killCount("league_task_defeat_the_corporeal_beast_50_times", "corp", 50)
    killCount("league_task_defeat_the_corporeal_beast_150_times", "corp", 150)
    killCount("league_task_defeat_the_corporeal_beast_250_times", "corp", 250)
    killCount("league_task_defeat_the_grotesque_guardians_50_times", "gargboss", 50)
    killCount("league_task_defeat_the_grotesque_guardians_150_times", "gargboss", 150)
    killCount("league_task_defeat_the_kalphite_queen_50_times", "kalphite", 50)
    killCount("league_task_defeat_the_kalphite_queen_150_times", "kalphite", 150)
    killCount("league_task_defeat_the_kraken_boss_50_times", "kraken_boss", 50)
    killCount("league_task_defeat_the_kraken_boss_150_times", "kraken_boss", 150)
    killCount("league_task_defeat_the_kraken_boss_300_times", "kraken_boss", 300)
    killCount("league_task_1_mimic_kill", "mimic", 1)
    killCount("league_task_5_mimic_kills", "mimic", 5)
    killCount("league_task_defeat_the_abyssal_sire_50_times", "abyssalsire", 50)
    killCount("league_task_defeat_the_abyssal_sire_150_times", "abyssalsire", 150)
    killCount("league_task_defeat_the_abyssal_sire_300_times", "abyssalsire", 300)
    killCount("league_task_defeat_sol_heredit_5_times", "sol", 5)
    killCount("league_task_defeat_sol_heredit_10_times", "sol", 10)
    for (chests in listOf(10, 25, 50)) {
        val task = "league_task_subdue_the_moons_of_peril_${chests}_times"
        variable(task, "varp.total_pmoon_chests", chests)
    }

    for (kills in listOf(50, 150, 300)) {
        val task = "league_task_defeat_each_dagannoth_king_${kills}_times"
        for (king in listOf("prime", "rex", "supreme")) {
            killCount(task, king, kills)
        }
    }

    for (kills in listOf(100, 250, 500)) {
        kill(
            "league_task_defeat_any_god_wars_dungeon_boss_${kills}_times",
            *GODWARS_BOSSES,
            times = kills,
            counter = "varbit.league_counter_kill_godwars_bosses",
        )
    }
}

private fun TriggerRows.killCount(task: String, boss: String, kills: Int) {
    variable(task, "varp.total_${boss}_kills", kills)
}
