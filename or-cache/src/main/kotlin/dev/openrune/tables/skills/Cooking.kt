package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Cooking {

    const val COL_ALE_INGREDIENT = 0
    const val COL_ALE_INGREDIENT_COUNT = 1
    const val COL_ALE_RESULT = 2
    const val COL_ALE_MATURE_RESULT = 3
    const val COL_ALE_LEVEL = 4
    const val COL_ALE_XP = 5
    const val COL_ALE_VAT_OFFSET = 6
    const val COL_ALE_BARREL_OFFSET = 7

    fun ales() = dbTable("dbtable.cooking_ales", serverOnly = true) {
        column("ingredient", COL_ALE_INGREDIENT, VarType.OBJ)
        column("ingredient_count", COL_ALE_INGREDIENT_COUNT, VarType.INT)
        column("result", COL_ALE_RESULT, VarType.OBJ)
        column("mature_result", COL_ALE_MATURE_RESULT, VarType.OBJ)
        column("level", COL_ALE_LEVEL, VarType.INT)
        column("xp", COL_ALE_XP, VarType.INT)
        column("vat_offset", COL_ALE_VAT_OFFSET, VarType.INT)
        column("barrel_offset", COL_ALE_BARREL_OFFSET, VarType.INT)

        row("dbrow.ale_dwarven_stout") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.hammerstone_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.dwarven_stout")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_dwarven_stout")
            column(COL_ALE_LEVEL, 19)
            column(COL_ALE_XP, 215)
            column(COL_ALE_VAT_OFFSET, 4)
            column(COL_ALE_BARREL_OFFSET, 4)
        }
        row("dbrow.ale_asgarnian_ale") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.hammerstone_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.asgarnian_ale")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_asgarnian_ale")
            column(COL_ALE_LEVEL, 24)
            column(COL_ALE_XP, 248)
            column(COL_ALE_VAT_OFFSET, 10)
            column(COL_ALE_BARREL_OFFSET, 6)
        }
        row("dbrow.ale_greenmans_ale") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.hammerstone_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.greenmans_ale")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_greenmans_ale")
            column(COL_ALE_LEVEL, 29)
            column(COL_ALE_XP, 281)
            column(COL_ALE_VAT_OFFSET, 16)
            column(COL_ALE_BARREL_OFFSET, 8)
        }
        row("dbrow.ale_wizards_mind_bomb") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.yanillian_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.wizards_mind_bomb")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_wizards_mind_bomb")
            column(COL_ALE_LEVEL, 34)
            column(COL_ALE_XP, 314)
            column(COL_ALE_VAT_OFFSET, 22)
            column(COL_ALE_BARREL_OFFSET, 10)
        }
        row("dbrow.ale_dragon_bitter") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.krandorian_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.dragon_bitter")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_dragon_bitter")
            column(COL_ALE_LEVEL, 39)
            column(COL_ALE_XP, 347)
            column(COL_ALE_VAT_OFFSET, 28)
            column(COL_ALE_BARREL_OFFSET, 12)
        }
        row("dbrow.ale_moonlight_mead") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.bittercap_mushroom")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.moonlight_mead")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_moonlight_mead")
            column(COL_ALE_LEVEL, 44)
            column(COL_ALE_XP, 380)
            column(COL_ALE_VAT_OFFSET, 34)
            column(COL_ALE_BARREL_OFFSET, 14)
        }
        row("dbrow.ale_axemans_folly") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.oak_roots")
            column(COL_ALE_INGREDIENT_COUNT, 1)
            columnRSCM(COL_ALE_RESULT, "obj.axemans_folly")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_axemans_folly")
            column(COL_ALE_LEVEL, 49)
            column(COL_ALE_XP, 413)
            column(COL_ALE_VAT_OFFSET, 40)
            column(COL_ALE_BARREL_OFFSET, 16)
        }
        row("dbrow.ale_chefs_delight") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.chocolate_dust")
            column(COL_ALE_INGREDIENT_COUNT, 2)
            columnRSCM(COL_ALE_RESULT, "obj.chefs_delight")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_chefs_delight")
            column(COL_ALE_LEVEL, 54)
            column(COL_ALE_XP, 446)
            column(COL_ALE_VAT_OFFSET, 46)
            column(COL_ALE_BARREL_OFFSET, 18)
        }
        row("dbrow.ale_slayers_respite") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.wildblood_hops")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.slayers_respite")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_slayers_respite")
            column(COL_ALE_LEVEL, 59)
            column(COL_ALE_XP, 479)
            column(COL_ALE_VAT_OFFSET, 52)
            column(COL_ALE_BARREL_OFFSET, 20)
        }
        row("dbrow.ale_cider") {
            columnRSCM(COL_ALE_INGREDIENT, "obj.apple_mush")
            column(COL_ALE_INGREDIENT_COUNT, 4)
            columnRSCM(COL_ALE_RESULT, "obj.cider")
            columnRSCM(COL_ALE_MATURE_RESULT, "obj.mature_cider")
            column(COL_ALE_LEVEL, 14)
            column(COL_ALE_XP, 182)
            column(COL_ALE_VAT_OFFSET, 58)
            column(COL_ALE_BARREL_OFFSET, 22)
        }
    }

    const val COL_RAW = 0
    const val COL_COOKED = 1
    const val COL_BURNT = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4
    const val COL_STOP_BURN_FIRE = 5
    const val COL_STOP_BURN_RANGE = 6
    const val COL_LOW = 7
    const val COL_HIGH = 8
    const val COL_GAUNTLET = 9

    fun foods() = dbTable("dbtable.cooking_foods", serverOnly = true) {

        column("raw", COL_RAW, VarType.OBJ)
        column("cooked", COL_COOKED, VarType.OBJ)
        column("burnt", COL_BURNT, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("stop_burn_fire", COL_STOP_BURN_FIRE, VarType.INT)
        column("stop_burn_range", COL_STOP_BURN_RANGE, VarType.INT)
        column("low", COL_LOW, VarType.INT)
        column("high", COL_HIGH, VarType.INT)
        column("supports_gauntlet", COL_GAUNTLET, VarType.BOOLEAN)

        row("dbrow.cooking_shrimp") {
            columnRSCM(COL_RAW, "obj.raw_shrimp")
            columnRSCM(COL_COOKED, "obj.shrimp")
            columnRSCM(COL_BURNT, "obj.burnt_shrimp")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_anchovies") {
            columnRSCM(COL_RAW, "obj.raw_anchovies")
            columnRSCM(COL_COOKED, "obj.anchovies")
            columnRSCM(COL_BURNT, "obj.burntfish1")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_sardine") {
            columnRSCM(COL_RAW, "obj.raw_sardine")
            columnRSCM(COL_COOKED, "obj.sardine")
            columnRSCM(COL_BURNT, "obj.burntfish1")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_STOP_BURN_FIRE, 38)
            column(COL_STOP_BURN_RANGE, 38)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_herring") {
            columnRSCM(COL_RAW, "obj.raw_herring")
            columnRSCM(COL_COOKED, "obj.herring")
            columnRSCM(COL_BURNT, "obj.burntfish2")
            column(COL_LEVEL, 5)
            column(COL_XP, 50)
            column(COL_STOP_BURN_FIRE, 41)
            column(COL_STOP_BURN_RANGE, 41)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_mackerel") {
            columnRSCM(COL_RAW, "obj.raw_mackerel")
            columnRSCM(COL_COOKED, "obj.mackerel")
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_LEVEL, 10)
            column(COL_XP, 60)
            column(COL_STOP_BURN_FIRE, 45)
            column(COL_STOP_BURN_RANGE, 45)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_trout") {
            columnRSCM(COL_RAW, "obj.raw_trout")
            columnRSCM(COL_COOKED, "obj.trout")
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_LEVEL, 15)
            column(COL_XP, 70)
            column(COL_STOP_BURN_FIRE, 49)
            column(COL_STOP_BURN_RANGE, 49)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_cod") {
            columnRSCM(COL_RAW, "obj.raw_cod")
            columnRSCM(COL_COOKED, "obj.cod")
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_LEVEL, 18)
            column(COL_XP, 75)
            column(COL_STOP_BURN_FIRE, 52)
            column(COL_STOP_BURN_RANGE, 52)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_pike") {
            columnRSCM(COL_RAW, "obj.raw_pike")
            columnRSCM(COL_COOKED, "obj.pike")
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_LEVEL, 20)
            column(COL_XP, 80)
            column(COL_STOP_BURN_FIRE, 64)
            column(COL_STOP_BURN_RANGE, 64)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_salmon") {
            columnRSCM(COL_RAW, "obj.raw_salmon")
            columnRSCM(COL_COOKED, "obj.salmon")
            columnRSCM(COL_BURNT, "obj.burntfish3")
            column(COL_LEVEL, 25)
            column(COL_XP, 90)
            column(COL_STOP_BURN_FIRE, 58)
            column(COL_STOP_BURN_RANGE, 58)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_tuna") {
            columnRSCM(COL_RAW, "obj.raw_tuna")
            columnRSCM(COL_COOKED, "obj.tuna")
            columnRSCM(COL_BURNT, "obj.burntfish4")
            column(COL_LEVEL, 30)
            column(COL_XP, 100)
            column(COL_STOP_BURN_FIRE, 63)
            column(COL_STOP_BURN_RANGE, 63)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_lobster") {
            columnRSCM(COL_RAW, "obj.raw_lobster")
            columnRSCM(COL_COOKED, "obj.lobster")
            columnRSCM(COL_BURNT, "obj.burnt_lobster")
            column(COL_LEVEL, 40)
            column(COL_XP, 120)
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_bass") {
            columnRSCM(COL_RAW, "obj.raw_bass")
            columnRSCM(COL_COOKED, "obj.bass")
            columnRSCM(COL_BURNT, "obj.burntfish5")
            column(COL_LEVEL, 43)
            column(COL_XP, 130)
            column(COL_STOP_BURN_FIRE, 80)
            column(COL_STOP_BURN_RANGE, 80)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_swordfish") {
            columnRSCM(COL_RAW, "obj.raw_swordfish")
            columnRSCM(COL_COOKED, "obj.swordfish")
            columnRSCM(COL_BURNT, "obj.burnt_swordfish")
            column(COL_LEVEL, 45)
            column(COL_XP, 140)
            column(COL_STOP_BURN_FIRE, 86)
            column(COL_STOP_BURN_RANGE, 81)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_monkfish") {
            columnRSCM(COL_RAW, "obj.raw_monkfish")
            columnRSCM(COL_COOKED, "obj.monkfish")
            columnRSCM(COL_BURNT, "obj.burnt_monkfish")
            column(COL_LEVEL, 62)
            column(COL_XP, 150)
            column(COL_STOP_BURN_FIRE, 92)
            column(COL_STOP_BURN_RANGE, 90)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_shark") {
            columnRSCM(COL_RAW, "obj.raw_shark")
            columnRSCM(COL_COOKED, "obj.shark")
            columnRSCM(COL_BURNT, "obj.burnt_shark")
            column(COL_LEVEL, 80)
            column(COL_XP, 210)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 94)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_manta_ray") {
            columnRSCM(COL_RAW, "obj.raw_mantaray")
            columnRSCM(COL_COOKED, "obj.mantaray")
            columnRSCM(COL_BURNT, "obj.burnt_mantaray")
            column(COL_LEVEL, 91)
            column(COL_XP, 216)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_sea_turtle") {
            columnRSCM(COL_RAW, "obj.raw_seaturtle")
            columnRSCM(COL_COOKED, "obj.seaturtle")
            columnRSCM(COL_BURNT, "obj.burnt_seaturtle")
            column(COL_LEVEL, 82)
            column(COL_XP, 211)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_dark_crab") {
            columnRSCM(COL_RAW, "obj.raw_dark_crab")
            columnRSCM(COL_COOKED, "obj.dark_crab")
            columnRSCM(COL_BURNT, "obj.burnt_dark_crab")
            column(COL_LEVEL, 90)
            column(COL_XP, 215)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_anglerfish") {
            columnRSCM(COL_RAW, "obj.raw_anglerfish")
            columnRSCM(COL_COOKED, "obj.anglerfish")
            columnRSCM(COL_BURNT, "obj.burnt_anglerfish")
            column(COL_LEVEL, 84)
            column(COL_XP, 230)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
            column(COL_GAUNTLET, true)
        }

        row("dbrow.cooking_chicken") {
            columnRSCM(COL_RAW, "obj.raw_chicken")
            columnRSCM(COL_COOKED, "obj.cooked_chicken")
            columnRSCM(COL_BURNT, "obj.burnt_chicken")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_meat") {
            columnRSCM(COL_RAW, "obj.raw_beef")
            columnRSCM(COL_COOKED, "obj.cooked_meat")
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_bear_meat") {
            columnRSCM(COL_RAW, "obj.raw_bear_meat")
            columnRSCM(COL_COOKED, "obj.cooked_meat")
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_rat_meat") {
            columnRSCM(COL_RAW, "obj.raw_rat_meat")
            columnRSCM(COL_COOKED, "obj.cooked_meat")
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_rabbit") {
            columnRSCM(COL_RAW, "obj.raw_rabbit")
            columnRSCM(COL_COOKED, "obj.cooked_rabbit")
            columnRSCM(COL_BURNT, "obj.burnt_meat")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_cave_eel") {
            columnRSCM(COL_RAW, "obj.raw_cave_eel")
            columnRSCM(COL_COOKED, "obj.cave_eel")
            columnRSCM(COL_BURNT, "obj.burnt_cave_eel")
            column(COL_LEVEL, 38)
            column(COL_XP, 115)
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 74)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_lava_eel") {
            columnRSCM(COL_RAW, "obj.raw_lava_eel")
            columnRSCM(COL_COOKED, "obj.lava_eel")
            columnRSCM(COL_BURNT, "obj.burnt_eel")
            column(COL_LEVEL, 53)
            column(COL_XP, 30)
            column(COL_STOP_BURN_FIRE, 53)
            column(COL_STOP_BURN_RANGE, 53)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_karambwan") {
            columnRSCM(COL_RAW, "obj.tbwt_raw_karambwan")
            columnRSCM(COL_COOKED, "obj.tbwt_cooked_karambwan")
            columnRSCM(COL_BURNT, "obj.tbwt_burnt_karambwan")
            column(COL_LEVEL, 30)
            column(COL_XP, 190)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_bread") {
            columnRSCM(COL_RAW, "obj.bread_dough")
            columnRSCM(COL_COOKED, "obj.bread")
            columnRSCM(COL_BURNT, "obj.burnt_bread")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_STOP_BURN_FIRE, 34)
            column(COL_STOP_BURN_RANGE, 34)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_redberry_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_redberry_pie")
            columnRSCM(COL_COOKED, "obj.redberry_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 10)
            column(COL_XP, 78)
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_meat_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_meat_pie")
            columnRSCM(COL_COOKED, "obj.meat_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 20)
            column(COL_XP, 110)
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_apple_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_apple_pie")
            columnRSCM(COL_COOKED, "obj.apple_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 30)
            column(COL_XP, 130)
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_pizza") {
            columnRSCM(COL_RAW, "obj.uncooked_pizza")
            columnRSCM(COL_COOKED, "obj.plain_pizza")
            columnRSCM(COL_BURNT, "obj.burnt_pizza")
            column(COL_LEVEL, 35)
            column(COL_XP, 143)
            column(COL_STOP_BURN_FIRE, 68)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_mud_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_mud_pie")
            columnRSCM(COL_COOKED, "obj.mud_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 29)
            column(COL_XP, 128)
            column(COL_STOP_BURN_FIRE, 50)
            column(COL_STOP_BURN_RANGE, 50)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_garden_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_garden_pie")
            columnRSCM(COL_COOKED, "obj.garden_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 34)
            column(COL_XP, 138)
            column(COL_STOP_BURN_FIRE, 68)
            column(COL_STOP_BURN_RANGE, 68)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_fish_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_fish_pie")
            columnRSCM(COL_COOKED, "obj.fish_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 47)
            column(COL_XP, 164)
            column(COL_STOP_BURN_FIRE, 80)
            column(COL_STOP_BURN_RANGE, 80)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_botanical_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_botanical_pie")
            columnRSCM(COL_COOKED, "obj.botanical_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 52)
            column(COL_XP, 180)
            column(COL_STOP_BURN_FIRE, 85)
            column(COL_STOP_BURN_RANGE, 85)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_mushroom_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_mushroom_pie")
            columnRSCM(COL_COOKED, "obj.mushroom_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 60)
            column(COL_XP, 200)
            column(COL_STOP_BURN_FIRE, 90)
            column(COL_STOP_BURN_RANGE, 90)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_admiral_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_admiral_pie")
            columnRSCM(COL_COOKED, "obj.admiral_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 70)
            column(COL_XP, 210)
            column(COL_STOP_BURN_FIRE, 94)
            column(COL_STOP_BURN_RANGE, 94)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_dragonfruit_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_dragonfruit_pie")
            columnRSCM(COL_COOKED, "obj.dragonfruit_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 73)
            column(COL_XP, 210)
            column(COL_STOP_BURN_FIRE, 97)
            column(COL_STOP_BURN_RANGE, 97)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_wild_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_wild_pie")
            columnRSCM(COL_COOKED, "obj.wild_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 85)
            column(COL_XP, 240)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_summer_pie") {
            columnRSCM(COL_RAW, "obj.uncooked_summer_pie")
            columnRSCM(COL_COOKED, "obj.summer_pie")
            columnRSCM(COL_BURNT, "obj.burnt_pie")
            column(COL_LEVEL, 95)
            column(COL_XP, 260)
            column(COL_STOP_BURN_FIRE, 99)
            column(COL_STOP_BURN_RANGE, 99)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_stew") {
            columnRSCM(COL_RAW, "obj.uncooked_stew")
            columnRSCM(COL_COOKED, "obj.stew")
            columnRSCM(COL_BURNT, "obj.burnt_stew")
            column(COL_LEVEL, 25)
            column(COL_XP, 117)
            column(COL_STOP_BURN_FIRE, 58)
            column(COL_STOP_BURN_RANGE, 58)
            column(COL_LOW, 50)
            column(COL_HIGH, 256)
        }

        row("dbrow.cooking_cake") {
            columnRSCM(COL_RAW, "obj.uncooked_cake")
            columnRSCM(COL_COOKED, "obj.cake")
            columnRSCM(COL_BURNT, "obj.burnt_cake")
            column(COL_LEVEL, 40)
            column(COL_XP, 180)
            column(COL_STOP_BURN_FIRE, 74)
            column(COL_STOP_BURN_RANGE, 74)
            column(COL_LOW, 38)
            column(COL_HIGH, 332)
        }
    }
}
