package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Smithing {

    const val COL_OUTPUT = 0
    const val COL_LEVEL = 1
    const val COL_SMELT_XP = 2
    const val COL_SMITH_XP = 3
    const val COL_SMELT_XP_ALTERNATE = 4
    const val COL_INPUT_PRIMARY = 5
    const val COL_INPUT_SECONDARY = 6
    const val COL_INPUT_PRIMARY_AMT = 7
    const val COL_INPUT_SECONDARY_AMT = 8
    const val COL_INPUT_PREFIX = 9

    const val COL_CANNONBALL_BAR = 0
    const val COL_CANNONBALL_OUTPUT = 1
    const val COL_CANNONBALL_LEVEL = 2
    const val COL_CANNONBALL_XP = 3

    fun cannonBalls() = dbTable("tables.smithing_cannon_balls",serverOnly = true) {
        column("bar", COL_CANNONBALL_BAR, VarType.OBJ)
        column("output", COL_CANNONBALL_OUTPUT, VarType.OBJ)
        column("level", COL_CANNONBALL_LEVEL, VarType.INT)
        column("xp", COL_CANNONBALL_XP, VarType.INT)

        row("dbrows.bronze_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.bronze_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.bronze_cannonball")
            column(COL_CANNONBALL_LEVEL,5)
            column(COL_CANNONBALL_XP,9)
        }

        row("dbrows.iron_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.bronze_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.iron_cannonball")
            column(COL_CANNONBALL_LEVEL,20)
            column(COL_CANNONBALL_XP,17)
        }

        row("dbrows.steel_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.steel_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.mcannonball")
            column(COL_CANNONBALL_LEVEL,35)
            column(COL_CANNONBALL_XP,27)
        }

        row("dbrows.mithril_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.mithril_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.mithril_cannonball")
            column(COL_CANNONBALL_LEVEL,55)
            column(COL_CANNONBALL_XP,34)
        }

        row("dbrows.adamantite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.adamantite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.adamant_cannonball")
            column(COL_CANNONBALL_LEVEL,75)
            column(COL_CANNONBALL_XP,43)
        }

        row("dbrows.runite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"items.runite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"items.rune_cannonball")
            column(COL_CANNONBALL_LEVEL,90)
            column(COL_CANNONBALL_XP,51)
        }


    }

    const val COL_DRAGON_OUTPUT = 0
    const val COL_DRAGON_OUTPUT_AMT = 1
    const val COL_DRAGON_LEVEL = 2
    const val COL_DRAGON_XP = 3
    const val COL_DRAGON_INPUT_PRIMARY = 4
    const val COL_DRAGON_INPUT_PRIMARY_AMT = 5


    fun dragonForge() = dbTable("tables.smithing_dragon_forge",serverOnly = true) {
        column("output", COL_DRAGON_OUTPUT, VarType.OBJ)
        column("output_amt", COL_DRAGON_OUTPUT_AMT, VarType.INT)
        column("level", COL_DRAGON_LEVEL, VarType.INT)
        column("xp", COL_DRAGON_XP, VarType.INT)
        column("input_primary", COL_DRAGON_INPUT_PRIMARY, VarType.OBJ)
        column("input_primary_amt", COL_DRAGON_INPUT_PRIMARY_AMT, VarType.INT)

        row("dbrows.dragon_keel_parts") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.sailing_boat_keel_part_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 94)
            column(COL_DRAGON_XP, 700)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.dragon_sheet")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 2)
        }

        row("dbrows.dragon_key") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.dragonkin_key")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 70)
            column(COL_DRAGON_XP, 0)
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.dragonkin_key_frem","items.dragonkin_key_mory", "items.dragonkin_key_zeah","items.dragonkin_key_karam")
        }

        row("dbrows.dragon_kiteshield") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.dragon_kiteshield")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 75)
            column(COL_DRAGON_XP, 1000)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.dragon_sq_shield","items.dragon_slice", "items.dragon_shard")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
        }

        row("dbrows.dragon_nails") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.nails_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 15)
            column(COL_DRAGON_LEVEL, 92)
            column(COL_DRAGON_XP, 350)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.dragon_sheet")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
        }

        row("dbrows.dragon_platebody") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.dragon_platebody")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 90)
            column(COL_DRAGON_XP, 2000)
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.dragon_chainbody","items.dragon_lump", "items.dragon_shard")
        }

        row("dbrows.large_dragon_keel_parts") {
            columnRSCM(COL_DRAGON_OUTPUT, "items.sailing_boat_large_keel_part_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 94)
            column(COL_DRAGON_XP, 500)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "items.sailing_boat_keel_part_dragon")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 2)
        }
    }

    fun bars() = dbTable("tables.smithing_bars",serverOnly = true) {

        column("output", COL_OUTPUT, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("smeltXp", COL_SMELT_XP, VarType.INT)
        column("smithXp", COL_SMITH_XP, VarType.INT)
        column("smithXpAlternate", COL_SMELT_XP_ALTERNATE, VarType.INT)
        column("input_primary", COL_INPUT_PRIMARY, VarType.OBJ)
        column("input_secondary", COL_INPUT_SECONDARY, VarType.OBJ)
        column("input_primary_amt", COL_INPUT_PRIMARY_AMT, VarType.INT)
        column("input_secondary_amt", COL_INPUT_SECONDARY_AMT, VarType.INT)
        column("prefix", COL_INPUT_PREFIX, VarType.STRING)

        row("dbrows.bronze") {
            columnRSCM(COL_OUTPUT,"items.bronze_bar")
            column(COL_LEVEL,1)
            column(COL_SMELT_XP,6)
            column(COL_SMITH_XP,12)
            columnRSCM(COL_INPUT_PRIMARY,"items.tin_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,1)
            column(COL_INPUT_PREFIX,"bronze")

        }

        row("dbrows.blurite") {
            columnRSCM(COL_OUTPUT,"items.blurite_bar")
            column(COL_LEVEL,13)
            column(COL_SMELT_XP,8)
            column(COL_SMELT_XP_ALTERNATE,10)
            column(COL_SMITH_XP,17)
            columnRSCM(COL_INPUT_PRIMARY,"items.blurite_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"blurite")
        }

        row("dbrows.iron") {
            columnRSCM(COL_OUTPUT,"items.iron_bar")
            column(COL_LEVEL,15)
            column(COL_SMELT_XP,12)
            column(COL_SMITH_XP,25)
            columnRSCM(COL_INPUT_PRIMARY,"items.iron_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"iron")
        }

        row("dbrows.silver") {
            columnRSCM(COL_OUTPUT,"items.silver_bar")
            column(COL_LEVEL,20)
            column(COL_SMELT_XP,14)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"items.silver_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"silver")
        }

        row("dbrows.lead") {
            columnRSCM(COL_OUTPUT,"items.lead_bar")
            column(COL_LEVEL,25)
            column(COL_SMELT_XP,15)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"items.lead_ore")
            column(COL_INPUT_PRIMARY_AMT,2)
            column(COL_INPUT_PREFIX,"lead")
        }

        row("dbrows.steel") {
            columnRSCM(COL_OUTPUT,"items.steel_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,17)
            column(COL_SMITH_XP,37)
            columnRSCM(COL_INPUT_PRIMARY,"items.iron_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"steel")
        }

        row("dbrows.gold") {
            columnRSCM(COL_OUTPUT,"items.gold_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,22)
            column(COL_SMELT_XP_ALTERNATE,56)
            column(COL_SMITH_XP,90)
            columnRSCM(COL_INPUT_PRIMARY,"items.gold_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"gold")
        }

        row("dbrows.lovakite") {
            columnRSCM(COL_OUTPUT,"items.lovakite_bar")
            column(COL_LEVEL,45)
            column(COL_SMELT_XP,20)
            column(COL_SMITH_XP,60)
            columnRSCM(COL_INPUT_PRIMARY,"items.lovakite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"shayzien")
        }

        row("dbrows.mithril") {
            columnRSCM(COL_OUTPUT,"items.mithril_bar")
            column(COL_LEVEL,50)
            column(COL_SMELT_XP,30)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"items.mithril_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,4)
            column(COL_INPUT_PREFIX,"mithril")
        }

        row("dbrows.adamantite") {
            columnRSCM(COL_OUTPUT,"items.adamantite_bar")
            column(COL_LEVEL,70)
            column(COL_SMELT_XP,37)
            column(COL_SMITH_XP,62)
            columnRSCM(COL_INPUT_PRIMARY,"items.adamantite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,6)
            column(COL_INPUT_PREFIX,"adamant")
        }

        row("dbrows.cupronickel") {
            columnRSCM(COL_OUTPUT,"items.cupronickel_bar")
            column(COL_LEVEL,74)
            column(COL_SMELT_XP,42)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"items.nickel_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"cupronickel")
        }

        row("dbrows.runite") {
            columnRSCM(COL_OUTPUT,"items.runite_bar")
            column(COL_LEVEL,85)
            column(COL_SMELT_XP,50)
            column(COL_SMITH_XP,75)
            columnRSCM(COL_INPUT_PRIMARY,"items.runite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"items.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,8)
            column(COL_INPUT_PREFIX,"rune")
        }
    }

    const val COL_CRYSTAL_OUTPUT = 0
    const val COL_CRYSTAL_XP = 1
    const val COL_CRYSTAL_LEVEL = 2
    const val COL_CRYSTAL_MATERIALS = 3
    const val COL_CRYSTAL_MATERIALS_AMT = 4
    const val COL_CRYSTAL_MATERIALS_SHORT_NAME = 5

    fun crystalSinging() = dbTable("tables.smithing_crystal_singing",serverOnly = true) {
        column("output", COL_CRYSTAL_OUTPUT, VarType.OBJ)
        column("xp", COL_CRYSTAL_XP, VarType.INT)
        column("level", COL_CRYSTAL_LEVEL, VarType.INT)
        column("materials", COL_CRYSTAL_MATERIALS, VarType.OBJ)
        column("materialsCount", COL_CRYSTAL_MATERIALS_AMT, VarType.INT)
        column("shortName", COL_CRYSTAL_MATERIALS_SHORT_NAME, VarType.STRING)

        row("dbrows.crystal_celestial_signet") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.celestial_signet")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 70)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.star_dust", "items.celestial_ring", "items.elven_signet")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1000, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "ring")
        }

        row("dbrows.crystal_helm") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_helmet")
            column(COL_CRYSTAL_XP, 2500)
            column(COL_CRYSTAL_LEVEL, 70)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 50, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "helmet")
        }

        row("dbrows.crystal_legs") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_platelegs")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 72)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 2)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "platelegs")
        }

        row("dbrows.crystal_body") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_chestplate")
            column(COL_CRYSTAL_XP, 7500)
            column(COL_CRYSTAL_LEVEL, 74)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 150, 3)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "platelegs")
        }

        row("dbrows.crystal_axe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_axe")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_tool_seed", "items.dragon_axe")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "axe")
        }

        row("dbrows.crystal_felling_axe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_axe_2h")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_tool_seed", "items.dragon_axe_2h")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "axe")
        }

        row("dbrows.crystal_harpoon") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_harpoon")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_tool_seed", "items.dragon_harpoon")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "harpoon")
        }

        row("dbrows.crystal_pickaxe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_pickaxe")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_tool_seed", "items.dragon_pickaxe")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "pickaxe")
        }

        row("dbrows.crystal_bow") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_bow")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "bow")
        }

        row("dbrows.crystal_halberd") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_halberd")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "halberd")
        }

        row("dbrows.crystal_shield") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.crystal_shield")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "halberd")
        }

        row("dbrows.enhanced_crystal_key") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.prif_crystal_key")
            column(COL_CRYSTAL_XP, 500)
            column(COL_CRYSTAL_LEVEL, 80)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.crystal_key")
            column(COL_CRYSTAL_MATERIALS_AMT, 10, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "key")
        }

        row("dbrows.eternal_teleport_crystal") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.prif_teleport_crystal")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 80)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_teleport_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
        }

        row("dbrows.blade_of_saeldor") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.blade_of_saeldor")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_weapon_seed_enhanced")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "saeldor")
        }

        row("dbrows.bow_of_faerdhinen") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.bow_of_faerdhinen")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.prif_weapon_seed_enhanced")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "bow")
        }

        row("dbrows.blade_of_saeldor_charged") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.blade_of_saeldor_infinite")
            column(COL_CRYSTAL_XP, 0)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.blade_of_saeldor_inactive")
            column(COL_CRYSTAL_MATERIALS_AMT, 1000, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "blade")
        }

        row("dbrows.bow_of_faerdhinen_charged") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "items.bow_of_faerdhinen_infinite")
            column(COL_CRYSTAL_XP, 0)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "items.prif_crystal_shard", "items.bow_of_faerdhinen_inactive")
            column(COL_CRYSTAL_MATERIALS_AMT, 2000, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "saeldor")
        }
    }

}