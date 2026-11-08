package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterActivityTables {
    fun monkeyTrap() =
        dbTable("dbtable.hunter_monkey_trap", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("xp_tenths", 1, VarType.INT)
            column("low", 2, VarType.INT)
            column("high", 3, VarType.INT)
            column("perfect_tail_rate", 4, VarType.INT)
            column("collapse_ticks", 5, VarType.INT)
            column("regrow_ticks", 6, VarType.INT)

            row("dbrow.hunter_monkey_trap") {
                column(0, 60)
                column(1, 10000)
                column(2, -79)
                column(3, 227)
                column(4, 5000)
                column(5, 100)
                column(6, 50)
            }
        }

    fun crystalImpling() =
        dbTable("dbtable.hunter_crystal_impling", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("xp", 1, VarType.INT)
            column("net_low", 2, VarType.INT)
            column("net_high", 3, VarType.INT)
            column("bonus_low", 4, VarType.INT)
            column("bonus_high", 5, VarType.INT)
            column("visible_after_minutes", 6, VarType.INT)

            row("dbrow.hunter_crystal_impling") {
                column(0, 80)
                column(1, 280)
                column(2, 8)
                column(3, 141)
                column(4, 28)
                column(5, 161)
                column(6, 3)
            }
        }

    fun stymphike() =
        dbTable("dbtable.hunter_stymphike", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("peek_xp_tenths", 1, VarType.INT)
            column("kill_xp_tenths", 2, VarType.INT)
            column("alerted_xp_tenths", 3, VarType.INT)
            column("pile_take", 4, VarType.INT)
            column("bait_extra_rate", 5, VarType.INT)
            column("spawn_ticks", 6, VarType.INT)
            column("peek_ticks", 7, VarType.INT)
            column("bird_ticks", 8, VarType.INT)
            column("alerted_damage", 9, VarType.INT)
            column("loot_duration", 10, VarType.INT)

            row("dbrow.hunter_stymphike") {
                column(0, 82)
                column(1, 1250)
                column(2, 13500)
                column(3, 8910)
                column(4, 10)
                column(5, 3)
                column(6, 65)
                column(7, 20)
                column(8, 100)
                column(9, 6)
                column(10, 200)
            }
        }

    fun aerialRates() =
        dbTable("dbtable.hunter_aerial_rates", serverOnly = true) {
            column("tench_pearls", 0, VarType.INT)
            column("whole_fish_cost", 1, VarType.INT)
            column("golden_tench_rate", 2, VarType.INT)
            column("heron_rate", 3, VarType.INT)
            column("spirit_flakes_chance", 4, VarType.INT)
            column("pearl_base_tenths", 5, VarType.INT)
            column("pearl_x_offset", 6, VarType.INT)
            column("pearl_slope", 7, VarType.INT)
            column("pearl_slope_divisor", 8, VarType.INT)
            column("move_min", 9, VarType.INT)
            column("move_max", 10, VarType.INT)
            column("frenzy_chance", 11, VarType.INT)
            column("frenzy_ticks", 12, VarType.INT)
            column("offcut_every_min", 13, VarType.INT)
            column("offcut_every_max", 14, VarType.INT)

            row("dbrow.hunter_aerial_rates") {
                column(0, 100)
                column(1, 20)
                column(2, 20_000)
                column(3, 636_833)
                column(4, 50)
                column(5, 666)
                column(6, 40)
                column(7, 25)
                column(8, 59)
                column(9, 12)
                column(10, 20)
                column(11, 50)
                column(12, 28)
                column(13, 3)
                column(14, 4)
            }
        }

    fun herbiboar() =
        dbTable("dbtable.hunter_herbiboar", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("herb_level", 1, VarType.INT)
            column("inspect_xp_tenths", 2, VarType.INT)
            column("herb_xp_tenths", 3, VarType.INT)
            column("horn_chance", 4, VarType.INT)
            column("pet_rate", 5, VarType.INT)
            column("fossil_rate", 6, VarType.INT)
            column("min_legs", 7, VarType.INT)
            column("max_legs", 8, VarType.INT)

            row("dbrow.hunter_herbiboar") {
                column(0, 80)
                column(1, 31)
                column(2, 500)
                column(3, 250)
                column(4, 10)
                column(5, 6500)
                column(6, 3500)
                column(7, 3)
                column(8, 6)
            }
        }

    fun falconryFees() =
        dbTable("dbtable.hunter_falconry_fees", serverOnly = true) {
            column("fee", 0, VarType.INT)
            column("investment", 1, VarType.INT)

            row("dbrow.hunter_falconry_fees") {
                column(0, 500)
                column(1, 500_000)
            }
        }

    fun sunlightCrossbow() =
        dbTable("dbtable.hunter_sunlight_crossbow", serverOnly = true) {
            column("level", 0, VarType.INT)

            row("dbrow.hunter_sunlight_crossbow") { column(0, 74) }
        }

    fun crabTrapBuild() =
        dbTable("dbtable.hunter_crab_trap_build", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("xp_tenths", 1, VarType.INT)
            column("nails", 2, VarType.OBJ)
            column("nails_needed", 3, VarType.INT)

            row("dbrow.hunter_crab_trap_build") {
                column(0, 10)
                column(1, 300)
                column(
                    2,
                    listOf(
                            "obj.nails_bronze",
                            "obj.nails_iron",
                            "obj.nails",
                            "obj.nails_black",
                            "obj.nails_mithril",
                            "obj.nails_adamant",
                            "obj.nails_rune",
                        )
                        .map { ConstantProvider.getMapping(it) },
                )
                column(3, 2)
            }
        }

    fun mossLizard() =
        dbTable("dbtable.hunter_moss_lizard", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("extra_lizard_levels", 1, VarType.INT)
            column("xp_tenths_per_level", 2, VarType.INT)
            column("xp_cap_tenths", 3, VarType.INT)
            column("lizard_life", 4, VarType.INT)
            column("lizard_obj_duration", 5, VarType.INT)

            row("dbrow.hunter_moss_lizard") {
                column(0, 20)
                column(1, listOf(50, 80))
                column(2, 9)
                column(3, 900)
                column(4, 30)
                column(5, 100)
            }
        }

    fun outfit() =
        dbTable("dbtable.hunter_outfit", serverOnly = true) {
            column("pieces", 0, VarType.OBJ)
            column("bonus_tenths", 1, VarType.INT)
            column("set_bonus_tenths", 2, VarType.INT)

            row("dbrow.hunter_outfit") {
                column(
                    0,
                    listOf("obj.hg_hunter_hood", "obj.hg_hunter_top", "obj.hg_hunter_legs", "obj.hg_hunter_boots")
                        .map { ConstantProvider.getMapping(it) },
                )
                column(1, listOf(2, 8, 6, 4))
                column(2, 5)
            }
        }

    fun goatPit() =
        dbTable("dbtable.hunter_goat_pit", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("pet_rate", 1, VarType.INT)
            column("prod_xp_tenths", 2, VarType.INT)
            column("lure_xp_tenths", 3, VarType.INT)
            column("push_tiles", 4, VarType.INT)
            column("goat_respawn_ticks", 5, VarType.INT)
            column("dark_lure_cooldown_ticks", 6, VarType.INT)

            row("dbrow.hunter_goat_pit") {
                column(0, 60)
                column(1, 40000)
                column(2, 200)
                column(3, 100)
                column(4, 5)
                column(5, 10)
                column(6, 17)
            }
        }

    fun netTrap() =
        dbTable("dbtable.hunter_net_trap", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("full_ticks", 1, VarType.INT)
            column("obj_duration", 2, VarType.INT)

            row("dbrow.hunter_net_trap") {
                column(0, 29)
                column(1, 100)
                column(2, 100)
            }
        }

    fun rabbitSnare() =
        dbTable("dbtable.hunter_rabbit_snare", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("empty_ticks", 1, VarType.INT)
            column("ferret_low", 2, VarType.INT)
            column("ferret_high", 3, VarType.INT)

            row("dbrow.hunter_rabbit_snare") {
                column(0, 27)
                column(1, 50)
                column(2, 192)
                column(3, 256)
            }
        }

    fun trapRules() =
        dbTable("dbtable.hunter_trap_rules", serverOnly = true) {
            column("smoke_level", 0, VarType.INT)
            column("bait_bonus", 1, VarType.INT)
            column("smoke_bonus", 2, VarType.INT)
            column("collapse_ticks", 3, VarType.INT)
            column("obj_duration", 4, VarType.INT)
            column("laid_obj_duration", 5, VarType.INT)
            column("laid_obj_reveal", 6, VarType.INT)

            row("dbrow.hunter_trap_rules") {
                column(0, 39)
                column(1, 3)
                column(2, 2)
                column(3, 100)
                column(4, 100)
                column(5, 300)
                column(6, 100)
            }
        }

    fun cape() =
        dbTable("dbtable.hunter_cape", serverOnly = true) {
            column("guild_level", 0, VarType.INT)
            column("daily_teleports", 1, VarType.INT)

            row("dbrow.hunter_cape") {
                column(0, 46)
                column(1, 5)
            }
        }

    fun lootSackRules() =
        dbTable("dbtable.hunter_loot_sack_rules", serverOnly = true) {
            column("enhanced_milestone", 0, VarType.INT)
            column("perfected_milestone", 1, VarType.INT)
            column("unique_rate", 2, VarType.INT)
            column("pet_rate", 3, VarType.INT)
            column("pet_rate_max_xp", 4, VarType.INT)

            row("dbrow.hunter_loot_sack_rules") {
                column(0, 100)
                column(1, 250)
                column(2, 50)
                column(3, 1000)
                column(4, 67)
            }
        }

    fun trackingRules() =
        dbTable("dbtable.hunter_tracking_rules", serverOnly = true) {
            column("fail_levels", 0, VarType.INT)
            column("pursuit_charges", 1, VarType.INT)

            row("dbrow.hunter_tracking_rules") {
                column(0, 2)
                column(1, 10)
            }
        }

    fun birdHouse() =
        dbTable("dbtable.hunter_bird_house", serverOnly = true) {
            column("clue_nest_rate", 0, VarType.INT)
            column("seeds_needed", 1, VarType.INT)
            column("full_minutes", 2, VarType.INT)
            column("birds", 3, VarType.INT)
            column("nest_rolls", 4, VarType.INT)

            row("dbrow.hunter_bird_house") {
                column(0, 1500)
                column(1, 10)
                column(2, 50)
                column(3, 10)
                column(4, 5)
            }
        }

    fun deadfallRules() =
        dbTable("dbtable.hunter_deadfall_rules", serverOnly = true) {
            column("limit", 0, VarType.INT)
            column("log_keep_rate", 1, VarType.INT)
            column("collapse_ticks", 2, VarType.INT)

            row("dbrow.hunter_deadfall_rules") {
                column(0, 2)
                column(1, 4)
                column(2, 100)
            }
        }

    fun pitfallRules() =
        dbTable("dbtable.hunter_pitfall_rules", serverOnly = true) {
            column("spear_bonus", 0, VarType.INT)
            column("fur_low", 1, VarType.INT)
            column("fur_high", 2, VarType.INT)
            column("spiked_collapse_ticks", 3, VarType.INT)
            column("full_collapse_ticks", 4, VarType.INT)

            row("dbrow.hunter_pitfall_rules") {
                column(0, 13)
                column(1, 96)
                column(2, 320)
                column(3, 300)
                column(4, 142)
            }
        }

    fun falconryRules() =
        dbTable("dbtable.hunter_falconry_rules", serverOnly = true) {
            column("leave_prey_ticks", 0, VarType.INT)
            column("kebbit_respawn_ticks", 1, VarType.INT)

            row("dbrow.hunter_falconry_rules") {
                column(0, 100)
                column(1, 15)
            }
        }

    fun butterflyRules() =
        dbTable("dbtable.hunter_butterfly_rules", serverOnly = true) {
            column("barehanded_extra_levels", 0, VarType.INT)
            column("respawn_ticks", 1, VarType.INT)
            column("sunlight_run_percent", 2, VarType.INT)
            column("max_shared", 3, VarType.INT)

            row("dbrow.hunter_butterfly_rules") {
                column(0, 10)
                column(1, 7)
                column(2, 5)
                column(3, 3)
            }
        }

    fun implingRules() =
        dbTable("dbtable.hunter_impling_rules", serverOnly = true) {
            column("barehanded_extra_levels", 0, VarType.INT)
            column("max_attempts", 1, VarType.INT)
            column("cycle_ticks", 2, VarType.INT)
            column("invisible_ticks", 3, VarType.INT)
            column("high_rolls", 4, VarType.INT)
            column("min_rolls", 5, VarType.INT)
            column("max_rolls", 6, VarType.INT)
            column("jar_break_num", 7, VarType.INT)
            column("jar_break_den", 8, VarType.INT)

            row("dbrow.hunter_impling_rules") {
                column(0, 10)
                column(1, 20)
                column(2, 3000)
                column(3, 200)
                column(4, 4)
                column(5, 5)
                column(6, 10)
                column(7, 1)
                column(8, 10)
            }
        }
}
