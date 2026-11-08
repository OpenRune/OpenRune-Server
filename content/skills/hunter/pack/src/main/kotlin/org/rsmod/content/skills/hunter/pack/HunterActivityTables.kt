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

            row("dbrow.hunter_monkey_trap") {
                column(0, 60)
                column(1, 10000)
                column(2, -79)
                column(3, 227)
                column(4, 5000)
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

            row("dbrow.hunter_crystal_impling") {
                column(0, 80)
                column(1, 280)
                column(2, 8)
                column(3, 141)
                column(4, 28)
                column(5, 161)
            }
        }

    fun stymphike() =
        dbTable("dbtable.hunter_stymphike", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("peek_xp_tenths", 1, VarType.INT)
            column("kill_xp_tenths", 2, VarType.INT)
            column("alerted_xp_tenths", 3, VarType.INT)

            row("dbrow.hunter_stymphike") {
                column(0, 82)
                column(1, 1250)
                column(2, 13500)
                column(3, 8910)
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

            row("dbrow.hunter_herbiboar") {
                column(0, 80)
                column(1, 31)
                column(2, 500)
                column(3, 250)
                column(4, 10)
                column(5, 6500)
                column(6, 3500)
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
            }
        }

    fun mossLizard() =
        dbTable("dbtable.hunter_moss_lizard", serverOnly = true) {
            column("level", 0, VarType.INT)
            column("extra_lizard_levels", 1, VarType.INT)
            column("xp_tenths_per_level", 2, VarType.INT)
            column("xp_cap_tenths", 3, VarType.INT)

            row("dbrow.hunter_moss_lizard") {
                column(0, 20)
                column(1, listOf(50, 80))
                column(2, 9)
                column(3, 900)
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
}
