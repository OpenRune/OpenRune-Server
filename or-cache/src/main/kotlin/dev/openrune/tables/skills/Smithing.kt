package dev.openrune.tables.skills

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

    fun cannonBalls() = dbTable("dbtable.smithing_cannon_balls",serverOnly = true) {
        column("bar", COL_CANNONBALL_BAR, VarType.OBJ)
        column("output", COL_CANNONBALL_OUTPUT, VarType.OBJ)
        column("level", COL_CANNONBALL_LEVEL, VarType.INT)
        column("xp", COL_CANNONBALL_XP, VarType.INT)

        row("dbrow.bronze_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.bronze_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.bronze_cannonball")
            column(COL_CANNONBALL_LEVEL,5)
            column(COL_CANNONBALL_XP,9)
        }

        row("dbrow.iron_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.iron_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.iron_cannonball")
            column(COL_CANNONBALL_LEVEL,20)
            column(COL_CANNONBALL_XP,17)
        }

        row("dbrow.steel_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.steel_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.mcannonball")
            column(COL_CANNONBALL_LEVEL,35)
            column(COL_CANNONBALL_XP,27)
        }

        row("dbrow.mithril_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.mithril_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.mithril_cannonball")
            column(COL_CANNONBALL_LEVEL,55)
            column(COL_CANNONBALL_XP,34)
        }

        row("dbrow.adamantite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.adamantite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.adamant_cannonball")
            column(COL_CANNONBALL_LEVEL,75)
            column(COL_CANNONBALL_XP,43)
        }

        row("dbrow.runite_cannon_ball") {
            columnRSCM(COL_CANNONBALL_BAR,"obj.runite_bar")
            columnRSCM(COL_CANNONBALL_OUTPUT,"obj.rune_cannonball")
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


    fun dragonForge() = dbTable("dbtable.smithing_dragon_forge",serverOnly = true) {
        column("output", COL_DRAGON_OUTPUT, VarType.OBJ)
        column("output_amt", COL_DRAGON_OUTPUT_AMT, VarType.INT)
        column("level", COL_DRAGON_LEVEL, VarType.INT)
        column("xp", COL_DRAGON_XP, VarType.INT)
        column("input_primary", COL_DRAGON_INPUT_PRIMARY, VarType.OBJ)
        column("input_primary_amt", COL_DRAGON_INPUT_PRIMARY_AMT, VarType.INT)

        row("dbrow.dragon_keel_parts") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.sailing_boat_keel_part_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 94)
            column(COL_DRAGON_XP, 700)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.dragon_sheet")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 2)
        }

        row("dbrow.dragon_key") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.dragonkin_key")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 70)
            column(COL_DRAGON_XP, 0)
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.dragonkin_key_frem","obj.dragonkin_key_mory", "obj.dragonkin_key_zeah","obj.dragonkin_key_karam")
        }

        row("dbrow.dragon_kiteshield") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.dragon_kiteshield")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 75)
            column(COL_DRAGON_XP, 1000)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.dragon_sq_shield","obj.dragon_slice", "obj.dragon_shard")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
        }

        row("dbrow.dragon_nails") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.nails_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 15)
            column(COL_DRAGON_LEVEL, 92)
            column(COL_DRAGON_XP, 350)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.dragon_sheet")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
        }

        row("dbrow.dragon_platebody") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.dragon_platebody")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 90)
            column(COL_DRAGON_XP, 2000)
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 1)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.dragon_chainbody","obj.dragon_lump", "obj.dragon_shard")
        }

        row("dbrow.large_dragon_keel_parts") {
            columnRSCM(COL_DRAGON_OUTPUT, "obj.sailing_boat_large_keel_part_dragon")
            column(COL_DRAGON_OUTPUT_AMT, 1)
            column(COL_DRAGON_LEVEL, 94)
            column(COL_DRAGON_XP, 500)
            columnRSCM(COL_DRAGON_INPUT_PRIMARY, "obj.sailing_boat_keel_part_dragon")
            column(COL_DRAGON_INPUT_PRIMARY_AMT, 2)
        }
    }

    fun bars() = dbTable("dbtable.smithing_bars",serverOnly = true) {

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

        row("dbrow.bronze") {
            columnRSCM(COL_OUTPUT,"obj.bronze_bar")
            column(COL_LEVEL,1)
            column(COL_SMELT_XP,6)
            column(COL_SMITH_XP,12)
            columnRSCM(COL_INPUT_PRIMARY,"obj.tin_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,1)
            column(COL_INPUT_PREFIX,"bronze")

        }

        row("dbrow.blurite") {
            columnRSCM(COL_OUTPUT,"obj.blurite_bar")
            column(COL_LEVEL,13)
            column(COL_SMELT_XP,8)
            column(COL_SMELT_XP_ALTERNATE,10)
            column(COL_SMITH_XP,17)
            columnRSCM(COL_INPUT_PRIMARY,"obj.blurite_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"blurite")
        }

        row("dbrow.iron") {
            columnRSCM(COL_OUTPUT,"obj.iron_bar")
            column(COL_LEVEL,15)
            column(COL_SMELT_XP,12)
            column(COL_SMITH_XP,25)
            columnRSCM(COL_INPUT_PRIMARY,"obj.iron_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"iron")
        }

        row("dbrow.silver") {
            columnRSCM(COL_OUTPUT,"obj.silver_bar")
            column(COL_LEVEL,20)
            column(COL_SMELT_XP,14)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"obj.silver_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"silver")
        }

        row("dbrow.lead") {
            columnRSCM(COL_OUTPUT,"obj.lead_bar")
            column(COL_LEVEL,25)
            column(COL_SMELT_XP,15)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"obj.lead_ore")
            column(COL_INPUT_PRIMARY_AMT,2)
            column(COL_INPUT_PREFIX,"lead")
        }

        row("dbrow.steel") {
            columnRSCM(COL_OUTPUT,"obj.steel_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,17)
            column(COL_SMITH_XP,37)
            columnRSCM(COL_INPUT_PRIMARY,"obj.iron_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"steel")
        }

        row("dbrow.gold") {
            columnRSCM(COL_OUTPUT,"obj.gold_bar")
            column(COL_LEVEL,30)
            column(COL_SMELT_XP,22)
            column(COL_SMELT_XP_ALTERNATE,56)
            column(COL_SMITH_XP,90)
            columnRSCM(COL_INPUT_PRIMARY,"obj.gold_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_PREFIX,"gold")
        }

        row("dbrow.lovakite") {
            columnRSCM(COL_OUTPUT,"obj.lovakite_bar")
            column(COL_LEVEL,45)
            column(COL_SMELT_XP,20)
            column(COL_SMITH_XP,60)
            columnRSCM(COL_INPUT_PRIMARY,"obj.lovakite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"shayzien")
        }

        row("dbrow.mithril") {
            columnRSCM(COL_OUTPUT,"obj.mithril_bar")
            column(COL_LEVEL,50)
            column(COL_SMELT_XP,30)
            column(COL_SMITH_XP,50)
            columnRSCM(COL_INPUT_PRIMARY,"obj.mithril_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,4)
            column(COL_INPUT_PREFIX,"mithril")
        }

        row("dbrow.adamantite") {
            columnRSCM(COL_OUTPUT,"obj.adamantite_bar")
            column(COL_LEVEL,70)
            column(COL_SMELT_XP,37)
            column(COL_SMITH_XP,62)
            columnRSCM(COL_INPUT_PRIMARY,"obj.adamantite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.coal")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,6)
            column(COL_INPUT_PREFIX,"adamant")
        }

        row("dbrow.cupronickel") {
            columnRSCM(COL_OUTPUT,"obj.cupronickel_bar")
            column(COL_LEVEL,74)
            column(COL_SMELT_XP,42)
            column(COL_SMITH_XP,0)
            columnRSCM(COL_INPUT_PRIMARY,"obj.nickel_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.copper_ore")
            column(COL_INPUT_PRIMARY_AMT,1)
            column(COL_INPUT_SECONDARY_AMT,2)
            column(COL_INPUT_PREFIX,"cupronickel")
        }

        row("dbrow.runite") {
            columnRSCM(COL_OUTPUT,"obj.runite_bar")
            column(COL_LEVEL,85)
            column(COL_SMELT_XP,50)
            column(COL_SMITH_XP,75)
            columnRSCM(COL_INPUT_PRIMARY,"obj.runite_ore")
            columnRSCM(COL_INPUT_SECONDARY,"obj.coal")
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

    fun crystalSinging() = dbTable("dbtable.smithing_crystal_singing",serverOnly = true) {
        column("output", COL_CRYSTAL_OUTPUT, VarType.OBJ)
        column("xp", COL_CRYSTAL_XP, VarType.INT)
        column("level", COL_CRYSTAL_LEVEL, VarType.INT)
        column("materials", COL_CRYSTAL_MATERIALS, VarType.OBJ)
        column("materialsCount", COL_CRYSTAL_MATERIALS_AMT, VarType.INT)
        column("shortName", COL_CRYSTAL_MATERIALS_SHORT_NAME, VarType.STRING)

        row("dbrow.crystal_celestial_signet") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.celestial_signet")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 70)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.star_dust", "obj.celestial_ring", "obj.elven_signet")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1000, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "ring")
        }

        row("dbrow.crystal_helm") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_helmet")
            column(COL_CRYSTAL_XP, 2500)
            column(COL_CRYSTAL_LEVEL, 70)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 50, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "helmet")
        }

        row("dbrow.crystal_legs") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_platelegs")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 72)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 2)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "platelegs")
        }

        row("dbrow.crystal_body") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_chestplate")
            column(COL_CRYSTAL_XP, 7500)
            column(COL_CRYSTAL_LEVEL, 74)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_armour_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 150, 3)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "platelegs")
        }

        row("dbrow.crystal_axe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_axe")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_tool_seed", "obj.dragon_axe")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "axe")
        }

        row("dbrow.crystal_felling_axe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_axe_2h")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_tool_seed", "obj.dragon_axe_2h")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "axe")
        }

        row("dbrow.crystal_harpoon") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_harpoon")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_tool_seed", "obj.dragon_harpoon")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "harpoon")
        }

        row("dbrow.crystal_pickaxe") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_pickaxe")
            column(COL_CRYSTAL_XP, 6000)
            column(COL_CRYSTAL_LEVEL, 76)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_tool_seed", "obj.dragon_pickaxe")
            column(COL_CRYSTAL_MATERIALS_AMT, 120, 1, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "pickaxe")
        }

        row("dbrow.crystal_bow") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_bow")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "bow")
        }

        row("dbrow.crystal_halberd") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_halberd")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "halberd")
        }

        row("dbrow.crystal_shield") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.crystal_shield")
            column(COL_CRYSTAL_XP, 2000)
            column(COL_CRYSTAL_LEVEL, 78)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.crystal_seed_old")
            column(COL_CRYSTAL_MATERIALS_AMT, 40, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "halberd")
        }

        row("dbrow.enhanced_crystal_key") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.prif_crystal_key")
            column(COL_CRYSTAL_XP, 500)
            column(COL_CRYSTAL_LEVEL, 80)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.crystal_key")
            column(COL_CRYSTAL_MATERIALS_AMT, 10, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "key")
        }

        row("dbrow.eternal_teleport_crystal") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.prif_teleport_crystal")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 80)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_teleport_seed")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
        }

        row("dbrow.blade_of_saeldor") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.blade_of_saeldor")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_weapon_seed_enhanced")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "saeldor")
        }

        row("dbrow.bow_of_faerdhinen") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.bow_of_faerdhinen")
            column(COL_CRYSTAL_XP, 5000)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.prif_weapon_seed_enhanced")
            column(COL_CRYSTAL_MATERIALS_AMT, 100, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "bow")
        }

        row("dbrow.blade_of_saeldor_charged") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.blade_of_saeldor_infinite")
            column(COL_CRYSTAL_XP, 0)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.blade_of_saeldor_inactive")
            column(COL_CRYSTAL_MATERIALS_AMT, 1000, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "blade")
        }

        row("dbrow.bow_of_faerdhinen_charged") {
            columnRSCM(COL_CRYSTAL_OUTPUT, "obj.bow_of_faerdhinen_infinite")
            column(COL_CRYSTAL_XP, 0)
            column(COL_CRYSTAL_LEVEL, 82)
            columnRSCM(COL_CRYSTAL_MATERIALS, "obj.prif_crystal_shard", "obj.bow_of_faerdhinen_inactive")
            column(COL_CRYSTAL_MATERIALS_AMT, 2000, 1)
            column(COL_CRYSTAL_MATERIALS_SHORT_NAME, "saeldor")
        }
    }

}
