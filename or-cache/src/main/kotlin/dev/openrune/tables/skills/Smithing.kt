package dev.openrune.tables.skills

import dev.openrune.definition.util.VarType
import dev.openrune.tables.production.productionTable

object Smithing {

    const val COL_SMITH_XP = 7
    const val COL_SMELT_XP_ALTERNATE = 8
    const val COL_PREFIX = 9

    const val COL_SHORT_NAME = 7

    fun cannonBalls() = productionTable("dbtable.smithing_cannon_balls", serverOnly = true, defaultCategory = "Smelt") {
        row("dbrow.bronze_cannon_ball") {
            production {
                input("obj.bronze_bar")
                statReq("stat.smithing", 5)
                xp(9)
                output("obj.bronze_cannonball")
            }
        }
        row("dbrow.iron_cannon_ball") {
            production {
                input("obj.iron_bar")
                statReq("stat.smithing", 20)
                xp(17)
                output("obj.iron_cannonball")
            }
        }
        row("dbrow.steel_cannon_ball") {
            production {
                input("obj.steel_bar")
                statReq("stat.smithing", 35)
                xp(27)
                output("obj.mcannonball")
            }
        }
        row("dbrow.mithril_cannon_ball") {
            production {
                input("obj.mithril_bar")
                statReq("stat.smithing", 55)
                xp(34)
                output("obj.mithril_cannonball")
            }
        }
        row("dbrow.adamantite_cannon_ball") {
            production {
                input("obj.adamantite_bar")
                statReq("stat.smithing", 75)
                xp(43)
                output("obj.adamant_cannonball")
            }
        }
        row("dbrow.runite_cannon_ball") {
            production {
                input("obj.runite_bar")
                statReq("stat.smithing", 90)
                xp(51)
                output("obj.rune_cannonball")
            }
        }

    }

    fun dragonForge() = productionTable("dbtable.smithing_dragon_forge", serverOnly = true, defaultCategory = "Forge") {
        row("dbrow.dragon_keel_parts") {
            production {
                input("obj.dragon_sheet", 2)
                statReq("stat.smithing", 94)
                xp(700)
                output("obj.sailing_boat_keel_part_dragon", 1)
            }
        }
        row("dbrow.dragon_key") {
            production {
                input("obj.dragonkin_key_frem", 1)
                input("obj.dragonkin_key_mory", 1)
                input("obj.dragonkin_key_zeah", 1)
                input("obj.dragonkin_key_karam", 1)
                statReq("stat.smithing", 70)
                xp(0)
                output("obj.dragonkin_key", 1)
            }
        }
        row("dbrow.dragon_kiteshield") {
            production {
                input("obj.dragon_sq_shield", 1)
                input("obj.dragon_slice", 1)
                input("obj.dragon_shard", 1)
                statReq("stat.smithing", 75)
                xp(1000)
                output("obj.dragon_kiteshield", 1)
            }
        }
        row("dbrow.dragon_nails") {
            production {
                input("obj.dragon_sheet", 1)
                statReq("stat.smithing", 92)
                xp(350)
                output("obj.nails_dragon", 15)
            }
        }
        row("dbrow.dragon_platebody") {
            production {
                input("obj.dragon_chainbody", 1)
                input("obj.dragon_lump", 1)
                input("obj.dragon_shard", 1)
                statReq("stat.smithing", 90)
                xp(2000)
                output("obj.dragon_platebody", 1)
            }
        }
        row("dbrow.large_dragon_keel_parts") {
            production {
                input("obj.sailing_boat_keel_part_dragon", 2)
                statReq("stat.smithing", 94)
                xp(500)
                output("obj.sailing_boat_large_keel_part_dragon", 1)
            }
        }

    }

    fun bars() = productionTable(
        "dbtable.smithing_bars",
        serverOnly = true,
        defaultCategory = "Smelt",
        extraColumns = {
            column("smithXp", COL_SMITH_XP, VarType.INT)
            column("smithXpAlternate", COL_SMELT_XP_ALTERNATE, VarType.INT)
            column("prefix", COL_PREFIX, VarType.STRING)
        },
    ) {
        row("dbrow.bronze") {
            production {
                input("obj.tin_ore", 1)
                input("obj.copper_ore", 1)
                statReq("stat.smithing", 1)
                xp(6)
                output("obj.bronze_bar")
            }
            column(COL_SMITH_XP, 12)
            column(COL_PREFIX, "bronze")
        }
        row("dbrow.blurite") {
            production {
                input("obj.blurite_ore", 1)
                statReq("stat.smithing", 13)
                xp(8)
                output("obj.blurite_bar")
            }
            column(COL_SMITH_XP, 17)
            column(COL_SMELT_XP_ALTERNATE, 10)
            column(COL_PREFIX, "blurite")
        }
        row("dbrow.iron") {
            production {
                input("obj.iron_ore", 1)
                statReq("stat.smithing", 15)
                xp(12)
                output("obj.iron_bar")
            }
            column(COL_SMITH_XP, 25)
            column(COL_PREFIX, "iron")
        }
        row("dbrow.silver") {
            production {
                input("obj.silver_ore", 1)
                statReq("stat.smithing", 20)
                xp(14)
                output("obj.silver_bar")
            }
            column(COL_SMITH_XP, 50)
            column(COL_PREFIX, "silver")
        }
        row("dbrow.lead") {
            production {
                input("obj.lead_ore", 2)
                statReq("stat.smithing", 25)
                xp(15)
                output("obj.lead_bar")
            }
            column(COL_SMITH_XP, 0)
            column(COL_PREFIX, "lead")
        }
        row("dbrow.steel") {
            production {
                input("obj.iron_ore", 1)
                input("obj.coal", 2)
                statReq("stat.smithing", 30)
                xp(17)
                output("obj.steel_bar")
            }
            column(COL_SMITH_XP, 37)
            column(COL_PREFIX, "steel")
        }
        row("dbrow.gold") {
            production {
                input("obj.gold_ore", 1)
                statReq("stat.smithing", 30)
                xp(22)
                output("obj.gold_bar")
            }
            column(COL_SMITH_XP, 90)
            column(COL_SMELT_XP_ALTERNATE, 56)
            column(COL_PREFIX, "gold")
        }
        row("dbrow.lovakite") {
            production {
                input("obj.lovakite_ore", 1)
                input("obj.coal", 2)
                statReq("stat.smithing", 45)
                xp(20)
                output("obj.lovakite_bar")
            }
            column(COL_SMITH_XP, 60)
            column(COL_PREFIX, "shayzien")
        }
        row("dbrow.mithril") {
            production {
                input("obj.mithril_ore", 1)
                input("obj.coal", 4)
                statReq("stat.smithing", 50)
                xp(30)
                output("obj.mithril_bar")
            }
            column(COL_SMITH_XP, 50)
            column(COL_PREFIX, "mithril")
        }
        row("dbrow.adamantite") {
            production {
                input("obj.adamantite_ore", 1)
                input("obj.coal", 6)
                statReq("stat.smithing", 70)
                xp(37)
                output("obj.adamantite_bar")
            }
            column(COL_SMITH_XP, 62)
            column(COL_PREFIX, "adamant")
        }
        row("dbrow.cupronickel") {
            production {
                input("obj.nickel_ore", 1)
                input("obj.copper_ore", 2)
                statReq("stat.smithing", 74)
                xp(42)
                output("obj.cupronickel_bar")
            }
            column(COL_SMITH_XP, 0)
            column(COL_PREFIX, "cupronickel")
        }
        row("dbrow.runite") {
            production {
                input("obj.runite_ore", 1)
                input("obj.coal", 8)
                statReq("stat.smithing", 85)
                xp(50)
                output("obj.runite_bar")
            }
            column(COL_SMITH_XP, 75)
            column(COL_PREFIX, "rune")
        }

    }

    fun crystalSinging() = productionTable(
        "dbtable.smithing_crystal_singing",
        serverOnly = true,
        defaultCategory = "Sing",
        extraColumns = {
            column("shortName", COL_SHORT_NAME, VarType.STRING)
        },
    ) {
        row("dbrow.crystal_celestial_signet") {
            production {
                input("obj.prif_crystal_shard", 100)
                input("obj.star_dust", 1000)
                input("obj.celestial_ring", 1)
                input("obj.elven_signet", 1)
                statReq("stat.smithing", 70)
                xp(5000)
                output("obj.celestial_signet")
            }
            column(COL_SHORT_NAME, "ring")
        }
        row("dbrow.crystal_helm") {
            production {
                input("obj.prif_crystal_shard", 50)
                input("obj.prif_armour_seed", 1)
                statReq("stat.smithing", 70)
                xp(2500)
                output("obj.crystal_helmet")
            }
            column(COL_SHORT_NAME, "helmet")
        }
        row("dbrow.crystal_legs") {
            production {
                input("obj.prif_crystal_shard", 100)
                input("obj.prif_armour_seed", 2)
                statReq("stat.smithing", 72)
                xp(5000)
                output("obj.crystal_platelegs")
            }
            column(COL_SHORT_NAME, "platelegs")
        }
        row("dbrow.crystal_body") {
            production {
                input("obj.prif_crystal_shard", 150)
                input("obj.prif_armour_seed", 3)
                statReq("stat.smithing", 74)
                xp(7500)
                output("obj.crystal_chestplate")
            }
            column(COL_SHORT_NAME, "platelegs")
        }
        row("dbrow.crystal_axe") {
            production {
                input("obj.prif_crystal_shard", 120)
                input("obj.prif_tool_seed", 1)
                input("obj.dragon_axe", 1)
                statReq("stat.smithing", 76)
                xp(6000)
                output("obj.crystal_axe")
            }
            column(COL_SHORT_NAME, "axe")
        }
        row("dbrow.crystal_felling_axe") {
            production {
                input("obj.prif_crystal_shard", 120)
                input("obj.prif_tool_seed", 1)
                input("obj.dragon_axe_2h", 1)
                statReq("stat.smithing", 76)
                xp(6000)
                output("obj.crystal_axe_2h")
            }
            column(COL_SHORT_NAME, "axe")
        }
        row("dbrow.crystal_harpoon") {
            production {
                input("obj.prif_crystal_shard", 120)
                input("obj.prif_tool_seed", 1)
                input("obj.dragon_harpoon", 1)
                statReq("stat.smithing", 76)
                xp(6000)
                output("obj.crystal_harpoon")
            }
            column(COL_SHORT_NAME, "harpoon")
        }
        row("dbrow.crystal_pickaxe") {
            production {
                input("obj.prif_crystal_shard", 120)
                input("obj.prif_tool_seed", 1)
                input("obj.dragon_pickaxe", 1)
                statReq("stat.smithing", 76)
                xp(6000)
                output("obj.crystal_pickaxe")
            }
            column(COL_SHORT_NAME, "pickaxe")
        }
        row("dbrow.crystal_bow") {
            production {
                input("obj.prif_crystal_shard", 40)
                input("obj.crystal_seed_old", 1)
                statReq("stat.smithing", 78)
                xp(2000)
                output("obj.crystal_bow")
            }
            column(COL_SHORT_NAME, "bow")
        }
        row("dbrow.crystal_halberd") {
            production {
                input("obj.prif_crystal_shard", 40)
                input("obj.crystal_seed_old", 1)
                statReq("stat.smithing", 78)
                xp(2000)
                output("obj.crystal_halberd")
            }
            column(COL_SHORT_NAME, "halberd")
        }
        row("dbrow.crystal_shield") {
            production {
                input("obj.prif_crystal_shard", 40)
                input("obj.crystal_seed_old", 1)
                statReq("stat.smithing", 78)
                xp(2000)
                output("obj.crystal_shield")
            }
            column(COL_SHORT_NAME, "halberd")
        }
        row("dbrow.enhanced_crystal_key") {
            production {
                input("obj.prif_crystal_shard", 10)
                input("obj.crystal_key", 1)
                statReq("stat.smithing", 80)
                xp(500)
                output("obj.prif_crystal_key")
            }
            column(COL_SHORT_NAME, "key")
        }
        row("dbrow.eternal_teleport_crystal") {
            production {
                input("obj.prif_crystal_shard", 100)
                input("obj.prif_teleport_seed", 1)
                statReq("stat.smithing", 80)
                xp(5000)
                output("obj.prif_teleport_crystal")
            }
        }
        row("dbrow.blade_of_saeldor") {
            production {
                input("obj.prif_crystal_shard", 100)
                input("obj.prif_weapon_seed_enhanced", 1)
                statReq("stat.smithing", 82)
                xp(5000)
                output("obj.blade_of_saeldor")
            }
            column(COL_SHORT_NAME, "saeldor")
        }
        row("dbrow.bow_of_faerdhinen") {
            production {
                input("obj.prif_crystal_shard", 100)
                input("obj.prif_weapon_seed_enhanced", 1)
                statReq("stat.smithing", 82)
                xp(5000)
                output("obj.bow_of_faerdhinen")
            }
            column(COL_SHORT_NAME, "bow")
        }
        row("dbrow.blade_of_saeldor_charged") {
            production {
                input("obj.prif_crystal_shard", 1000)
                input("obj.blade_of_saeldor_inactive", 1)
                statReq("stat.smithing", 82)
                xp(0)
                output("obj.blade_of_saeldor_infinite")
            }
            column(COL_SHORT_NAME, "blade")
        }
        row("dbrow.bow_of_faerdhinen_charged") {
            production {
                input("obj.prif_crystal_shard", 2000)
                input("obj.bow_of_faerdhinen_inactive", 1)
                statReq("stat.smithing", 82)
                xp(0)
                output("obj.bow_of_faerdhinen_infinite")
            }
            column(COL_SHORT_NAME, "saeldor")
        }
    }

}
