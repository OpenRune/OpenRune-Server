package org.rsmod.api.combatachievements

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

@Singleton
public class CombatAchievements @Inject public constructor(
    private val catalogue: CombatAchievementCatalogue,
) {
    public fun recordBarrowsChest(player: Player, allBrothersKilled: Boolean) {
        val count = increment(player, BARROWS_COUNT, 25)
        if (count >= 10) complete(player, BARROWS_NOVICE)
        if (count >= 25) complete(player, BARROWS_CHAMPION)
    }

    public fun recordKrakenKill(
        player: Player,
        eligible: Boolean,
        allTentaclesKilled: Boolean = false,
    ) {
        if (!eligible) return
        val count = increment(player, KRAKEN_COUNT, 20)
        if (count >= 20) complete(player, KRAKEN_ADEPT)
        if (allTentaclesKilled) complete(player, UNNECESSARY_OPTIMISATION)
    }

    public fun isCompleted(player: Player, id: Int): Boolean {
        catalogue.task(id)
        return player.vars["varp.ca_task_completed_${id / 32}"] and (1 shl (id % 32)) != 0
    }

    public fun points(player: Player): Int = completed(player).sumOf { it.tier }

    public fun earnedTier(player: Player): Int {
        val points = points(player)
        return catalogue.thresholds(groupSize(player)).count { it <= points }
    }

    public fun refresh(player: Player) {
        val completed = completed(player)
        val thresholds = catalogue.thresholds(groupSize(player))
        val points = completed.sumOf { it.tier }
        VarPlayerIntMapSetter.set(player, "varbit.ca_points", points)
        for ((index, tier) in CombatAchievementCatalogue.TIERS.withIndex()) {
            VarPlayerIntMapSetter.set(player, "varbit.ca_threshold_$tier", thresholds[index])
            VarPlayerIntMapSetter.set(
                player,
                "varbit.ca_total_tasks_completed_$tier",
                completed.count { it.tier == index + 1 },
            )
            val status = "varbit.ca_tier_status_$tier"
            val earned = points >= thresholds[index]
            val value = if (!earned) 0 else if (player.vars[status] == 2) 2 else 1
            VarPlayerIntMapSetter.set(player, status, value)
        }
        for ((boss, variable) in BOSS_COUNTS) {
            VarPlayerIntMapSetter.set(player, variable, completed.count { it.bossId == boss })
        }
    }

    private fun completed(player: Player): List<CombatAchievementTask> {
        val groupSize = groupSize(player)
        return catalogue.tasks.filter { catalogue.isAvailable(it, groupSize) && isCompleted(player, it.id) }
    }

    private fun groupSize(player: Player): Int =
        if (player.vars["varbit.ironman"] in 4..6) player.vars["varbit.gim_groupsize"] else 0

    private fun increment(player: Player, variable: String, target: Int): Int {
        val current = player.vars[variable].coerceIn(0, target)
        val count = (current + 1).coerceAtMost(target)
        VarPlayerIntMapSetter.set(player, variable, count)
        return count
    }

    private fun complete(player: Player, id: Int) {
        if (isCompleted(player, id)) return
        val variable = "varp.ca_task_completed_${id / 32}"
        VarPlayerIntMapSetter.set(player, variable, player.vars[variable] or (1 shl (id % 32)))
        refresh(player)
        val task = catalogue.task(id)
        player.mes("Combat Achievement completed: ${task.name} (${task.tier} points).")
        if (player.vars["varbit.ca_task_popup"] == 0) {
            ClientScripts.notificationDisplay(player, "Combat Achievements", task.name)
        }
    }

    private companion object {
        const val BARROWS_NOVICE = 24
        const val BARROWS_CHAMPION = 25
        const val KRAKEN_ADEPT = 168
        const val UNNECESSARY_OPTIMISATION = 169
        const val BARROWS_COUNT = "varp.ca_legitimate_barrows_chests"
        const val KRAKEN_COUNT = "varp.ca_legitimate_kraken_kills"
        val BOSS_COUNTS = mapOf(
            1 to "varbit.ca_total_tasks_completed_abyssalsire",
            2 to "varbit.ca_total_tasks_completed_hydraboss",
            3 to "varbit.ca_total_tasks_completed_amoxliatl",
            4 to "varbit.ca_total_tasks_completed_araxxor",
            5 to "varbit.ca_total_tasks_completed_barrows",
            7 to "varbit.ca_total_tasks_completed_bryophyta",
            8 to "varbit.ca_total_tasks_completed_callisto",
            9 to "varbit.ca_total_tasks_completed_cerberus",
            10 to "varbit.ca_total_tasks_completed_chaosele",
            11 to "varbit.ca_total_tasks_completed_chaosfanatic",
            12 to "varbit.ca_total_tasks_completed_crazyarchaeologist",
            13 to "varbit.ca_total_tasks_completed_xericchambers",
            14 to "varbit.ca_total_tasks_completed_xericchambers_challenge",
            15 to "varbit.ca_total_tasks_completed_corp",
            16 to "varbit.ca_total_tasks_completed_saradomin",
            17 to "varbit.ca_total_tasks_completed_gauntlet",
            18 to "varbit.ca_total_tasks_completed_gauntlet_hm",
            19 to "varbit.ca_total_tasks_completed_prime",
            20 to "varbit.ca_total_tasks_completed_rex",
            21 to "varbit.ca_total_tasks_completed_supreme",
            22 to "varbit.ca_total_tasks_completed_derangedarchaeologist",
            24 to "varbit.ca_total_tasks_completed_dukesucellus",
            25 to "varbit.ca_total_tasks_completed_colosseum",
            26 to "varbit.ca_total_tasks_completed_bandos",
            27 to "varbit.ca_total_tasks_completed_mole",
            28 to "varbit.ca_total_tasks_completed_gargboss",
            29 to "varbit.ca_total_tasks_completed_hespori",
            30 to "varbit.ca_total_tasks_completed_hueycoatl",
            31 to "varbit.ca_total_tasks_completed_kalphite",
            32 to "varbit.ca_total_tasks_completed_kbd",
            33 to "varbit.ca_total_tasks_completed_kraken_boss",
            34 to "varbit.ca_total_tasks_completed_armadyl",
            35 to "varbit.ca_total_tasks_completed_zamorak",
            36 to "varbit.ca_total_tasks_completed_leviathan",
            39 to "varbit.ca_total_tasks_completed_mimic",
            40 to "varbit.ca_total_tasks_completed_perilous_moons",
            41 to "varbit.ca_total_tasks_completed_nex",
            42 to "varbit.ca_total_tasks_completed_nightmare",
            43 to "varbit.ca_total_tasks_completed_phosanis",
            44 to "varbit.ca_total_tasks_completed_hillgiant_boss",
            45 to "varbit.ca_total_tasks_completed_muspah",
            46 to "varbit.ca_total_tasks_completed_royal_titans",
            47 to "varbit.ca_total_tasks_completed_rat_boss",
            48 to "varbit.ca_total_tasks_completed_sarachnis",
            49 to "varbit.ca_total_tasks_completed_scorpia",
            50 to "varbit.ca_total_tasks_completed_gryphon_boss",
            51 to "varbit.ca_total_tasks_completed_cata_boss",
            52 to "varbit.ca_total_tasks_completed_tempoross",
            53 to "varbit.ca_total_tasks_completed_theatreofblood_story",
            54 to "varbit.ca_total_tasks_completed_theatreofblood",
            55 to "varbit.ca_total_tasks_completed_theatreofblood_hard",
            56 to "varbit.ca_total_tasks_completed_thermy",
            57 to "varbit.ca_total_tasks_completed_tombsofamascut_entry",
            58 to "varbit.ca_total_tasks_completed_tombsofamascut",
            59 to "varbit.ca_total_tasks_completed_tombsofamascut_expert",
            60 to "varbit.ca_total_tasks_completed_tzhaarketrak",
            61 to "varbit.ca_total_tasks_completed_zuk",
            62 to "varbit.ca_total_tasks_completed_jad",
            63 to "varbit.ca_total_tasks_completed_vardorvis",
            64 to "varbit.ca_total_tasks_completed_venenatis",
            65 to "varbit.ca_total_tasks_completed_vetion",
            66 to "varbit.ca_total_tasks_completed_vorkath",
            67 to "varbit.ca_total_tasks_completed_whisperer",
            68 to "varbit.ca_total_tasks_completed_wintertodt",
            69 to "varbit.ca_total_tasks_completed_yama",
            23 to "varbit.ca_total_tasks_completed_dom",
            70 to "varbit.ca_total_tasks_completed_zalcano",
            71 to "varbit.ca_total_tasks_completed_snakeboss",
            6 to "varbit.ca_total_tasks_completed_cowboss",
            38 to "varbit.ca_total_tasks_completed_maggotking",
            37 to "varbit.ca_total_tasks_completed_mad_angel",
        )
    }
}
