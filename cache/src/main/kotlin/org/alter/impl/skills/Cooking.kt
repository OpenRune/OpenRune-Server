package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Cooking {

    const val COL_RAW_ITEM = 0
    const val COL_COOKED_ITEM = 1
    const val COL_BURNT_ITEM = 2
    const val COL_LEVEL = 3
    const val COL_XP = 4
    const val COL_BURN_STOP_FIRE = 5
    const val COL_BURN_STOP_RANGE = 6
    const val COL_METHOD = 7

    fun cookingRecipes() = dbTable("tables.cooking_recipes", serverOnly = true) {

        column("raw_item", COL_RAW_ITEM, VarType.OBJ)
        column("cooked_item", COL_COOKED_ITEM, VarType.OBJ)
        column("burnt_item", COL_BURNT_ITEM, VarType.OBJ)
        column("level", COL_LEVEL, VarType.INT)
        column("xp", COL_XP, VarType.INT)
        column("burn_stop_fire", COL_BURN_STOP_FIRE, VarType.INT)
        column("burn_stop_range", COL_BURN_STOP_RANGE, VarType.INT)
        column("method", COL_METHOD, VarType.INT)

        // Shrimps (level 1)
        row("dbrows.cooking_shrimps") {
            columnRSCM(COL_RAW_ITEM, "items.raw_shrimp")
            columnRSCM(COL_COOKED_ITEM, "items.shrimp")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_shrimp")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_BURN_STOP_FIRE, 34)
            column(COL_BURN_STOP_RANGE, 30)
            column(COL_METHOD, 0)
        }

        // Meat (level 1)
        row("dbrows.cooking_meat") {
            columnRSCM(COL_RAW_ITEM, "items.raw_beef")
            columnRSCM(COL_COOKED_ITEM, "items.cooked_meat")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_meat")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_BURN_STOP_FIRE, 34)
            column(COL_BURN_STOP_RANGE, 30)
            column(COL_METHOD, 0)
        }

        // Chicken (level 1)
        row("dbrows.cooking_chicken") {
            columnRSCM(COL_RAW_ITEM, "items.raw_chicken")
            columnRSCM(COL_COOKED_ITEM, "items.cooked_chicken")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_chicken")
            column(COL_LEVEL, 1)
            column(COL_XP, 30)
            column(COL_BURN_STOP_FIRE, 34)
            column(COL_BURN_STOP_RANGE, 30)
            column(COL_METHOD, 0)
        }

        // Sardine (level 1)
        row("dbrows.cooking_sardine") {
            columnRSCM(COL_RAW_ITEM, "items.raw_sardine")
            columnRSCM(COL_COOKED_ITEM, "items.sardine")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish2")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_BURN_STOP_FIRE, 38)
            column(COL_BURN_STOP_RANGE, 35)
            column(COL_METHOD, 0)
        }

        // Herring (level 5)
        row("dbrows.cooking_herring") {
            columnRSCM(COL_RAW_ITEM, "items.raw_herring")
            columnRSCM(COL_COOKED_ITEM, "items.herring")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish2")
            column(COL_LEVEL, 5)
            column(COL_XP, 50)
            column(COL_BURN_STOP_FIRE, 41)
            column(COL_BURN_STOP_RANGE, 37)
            column(COL_METHOD, 0)
        }

        // Mackerel (level 10)
        row("dbrows.cooking_mackerel") {
            columnRSCM(COL_RAW_ITEM, "items.raw_mackerel")
            columnRSCM(COL_COOKED_ITEM, "items.mackerel")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish3")
            column(COL_LEVEL, 10)
            column(COL_XP, 60)
            column(COL_BURN_STOP_FIRE, 45)
            column(COL_BURN_STOP_RANGE, 40)
            column(COL_METHOD, 0)
        }

        // Trout (level 15)
        row("dbrows.cooking_trout") {
            columnRSCM(COL_RAW_ITEM, "items.raw_trout")
            columnRSCM(COL_COOKED_ITEM, "items.trout")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish2")
            column(COL_LEVEL, 15)
            column(COL_XP, 70)
            column(COL_BURN_STOP_FIRE, 50)
            column(COL_BURN_STOP_RANGE, 46)
            column(COL_METHOD, 0)
        }

        // Cod (level 18)
        row("dbrows.cooking_cod") {
            columnRSCM(COL_RAW_ITEM, "items.raw_cod")
            columnRSCM(COL_COOKED_ITEM, "items.cod")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish3")
            column(COL_LEVEL, 18)
            column(COL_XP, 75)
            column(COL_BURN_STOP_FIRE, 52)
            column(COL_BURN_STOP_RANGE, 48)
            column(COL_METHOD, 0)
        }

        // Pike (level 20)
        row("dbrows.cooking_pike") {
            columnRSCM(COL_RAW_ITEM, "items.raw_pike")
            columnRSCM(COL_COOKED_ITEM, "items.pike")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish2")
            column(COL_LEVEL, 20)
            column(COL_XP, 80)
            column(COL_BURN_STOP_FIRE, 55)
            column(COL_BURN_STOP_RANGE, 50)
            column(COL_METHOD, 0)
        }

        // Salmon (level 25)
        row("dbrows.cooking_salmon") {
            columnRSCM(COL_RAW_ITEM, "items.raw_salmon")
            columnRSCM(COL_COOKED_ITEM, "items.salmon")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish2")
            column(COL_LEVEL, 25)
            column(COL_XP, 90)
            column(COL_BURN_STOP_FIRE, 58)
            column(COL_BURN_STOP_RANGE, 53)
            column(COL_METHOD, 0)
        }

        // Tuna (level 30)
        row("dbrows.cooking_tuna") {
            columnRSCM(COL_RAW_ITEM, "items.raw_tuna")
            columnRSCM(COL_COOKED_ITEM, "items.tuna")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish5")
            column(COL_LEVEL, 30)
            column(COL_XP, 100)
            column(COL_BURN_STOP_FIRE, 64)
            column(COL_BURN_STOP_RANGE, 58)
            column(COL_METHOD, 0)
        }

        // Lobster (level 40)
        row("dbrows.cooking_lobster") {
            columnRSCM(COL_RAW_ITEM, "items.raw_lobster")
            columnRSCM(COL_COOKED_ITEM, "items.lobster")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_lobster")
            column(COL_LEVEL, 40)
            column(COL_XP, 120)
            column(COL_BURN_STOP_FIRE, 74)
            column(COL_BURN_STOP_RANGE, 68)
            column(COL_METHOD, 0)
        }

        // Bass (level 43)
        row("dbrows.cooking_bass") {
            columnRSCM(COL_RAW_ITEM, "items.raw_bass")
            columnRSCM(COL_COOKED_ITEM, "items.bass")
            columnRSCM(COL_BURNT_ITEM, "items.burntfish4")
            column(COL_LEVEL, 43)
            column(COL_XP, 130)
            column(COL_BURN_STOP_FIRE, 80)
            column(COL_BURN_STOP_RANGE, 73)
            column(COL_METHOD, 0)
        }

        // Swordfish (level 45)
        row("dbrows.cooking_swordfish") {
            columnRSCM(COL_RAW_ITEM, "items.raw_swordfish")
            columnRSCM(COL_COOKED_ITEM, "items.swordfish")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_swordfish")
            column(COL_LEVEL, 45)
            column(COL_XP, 140)
            column(COL_BURN_STOP_FIRE, 86)
            column(COL_BURN_STOP_RANGE, 79)
            column(COL_METHOD, 0)
        }

        // Monkfish (level 62)
        row("dbrows.cooking_monkfish") {
            columnRSCM(COL_RAW_ITEM, "items.raw_monkfish")
            columnRSCM(COL_COOKED_ITEM, "items.monkfish")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_monkfish")
            column(COL_LEVEL, 62)
            column(COL_XP, 150)
            column(COL_BURN_STOP_FIRE, 92)
            column(COL_BURN_STOP_RANGE, 87)
            column(COL_METHOD, 0)
        }

        // Shark (level 80)
        row("dbrows.cooking_shark") {
            columnRSCM(COL_RAW_ITEM, "items.raw_shark")
            columnRSCM(COL_COOKED_ITEM, "items.shark")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_shark")
            column(COL_LEVEL, 80)
            column(COL_XP, 210)
            column(COL_BURN_STOP_FIRE, 99)
            column(COL_BURN_STOP_RANGE, 94)
            column(COL_METHOD, 0)
        }

        // Sea turtle (level 82)
        row("dbrows.cooking_sea_turtle") {
            columnRSCM(COL_RAW_ITEM, "items.raw_seaturtle")
            columnRSCM(COL_COOKED_ITEM, "items.seaturtle")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_seaturtle")
            column(COL_LEVEL, 82)
            column(COL_XP, 211)
            column(COL_BURN_STOP_FIRE, 99)
            column(COL_BURN_STOP_RANGE, 99)
            column(COL_METHOD, 0)
        }

        // Anglerfish (level 84)
        row("dbrows.cooking_anglerfish") {
            columnRSCM(COL_RAW_ITEM, "items.raw_anglerfish")
            columnRSCM(COL_COOKED_ITEM, "items.anglerfish")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_anglerfish")
            column(COL_LEVEL, 84)
            column(COL_XP, 230)
            column(COL_BURN_STOP_FIRE, 99)
            column(COL_BURN_STOP_RANGE, 99)
            column(COL_METHOD, 0)
        }

        // Manta ray (level 91)
        row("dbrows.cooking_manta_ray") {
            columnRSCM(COL_RAW_ITEM, "items.raw_mantaray")
            columnRSCM(COL_COOKED_ITEM, "items.mantaray")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_mantaray")
            column(COL_LEVEL, 91)
            column(COL_XP, 216)
            column(COL_BURN_STOP_FIRE, 99)
            column(COL_BURN_STOP_RANGE, 99)
            column(COL_METHOD, 0)
        }

        // Dark crab (level 90)
        row("dbrows.cooking_dark_crab") {
            columnRSCM(COL_RAW_ITEM, "items.raw_dark_crab")
            columnRSCM(COL_COOKED_ITEM, "items.dark_crab")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_dark_crab")
            column(COL_LEVEL, 90)
            column(COL_XP, 215)
            column(COL_BURN_STOP_FIRE, 99)
            column(COL_BURN_STOP_RANGE, 99)
            column(COL_METHOD, 0)
        }

        // Bread (level 1, range only)
        row("dbrows.cooking_bread") {
            columnRSCM(COL_RAW_ITEM, "items.bread_dough")
            columnRSCM(COL_COOKED_ITEM, "items.bread")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_bread")
            column(COL_LEVEL, 1)
            column(COL_XP, 40)
            column(COL_BURN_STOP_FIRE, 35)
            column(COL_BURN_STOP_RANGE, 32)
            column(COL_METHOD, 1)
        }

        // Stew (level 25)
        row("dbrows.cooking_stew") {
            columnRSCM(COL_RAW_ITEM, "items.uncooked_stew")
            columnRSCM(COL_COOKED_ITEM, "items.stew")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_stew")
            column(COL_LEVEL, 25)
            column(COL_XP, 117)
            column(COL_BURN_STOP_FIRE, 58)
            column(COL_BURN_STOP_RANGE, 53)
            column(COL_METHOD, 0)
        }

        // Meat pie (level 20, range only)
        row("dbrows.cooking_meat_pie") {
            columnRSCM(COL_RAW_ITEM, "items.pie_shell")
            columnRSCM(COL_COOKED_ITEM, "items.meat_pie")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_pie")
            column(COL_LEVEL, 20)
            column(COL_XP, 110)
            column(COL_BURN_STOP_FIRE, 55)
            column(COL_BURN_STOP_RANGE, 50)
            column(COL_METHOD, 1)
        }

        // Redberry pie (level 10, range only)
        row("dbrows.cooking_redberry_pie") {
            columnRSCM(COL_RAW_ITEM, "items.pie_shell")
            columnRSCM(COL_COOKED_ITEM, "items.redberry_pie")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_pie")
            column(COL_LEVEL, 10)
            column(COL_XP, 78)
            column(COL_BURN_STOP_FIRE, 45)
            column(COL_BURN_STOP_RANGE, 40)
            column(COL_METHOD, 1)
        }

        // Apple pie (level 30, range only)
        row("dbrows.cooking_apple_pie") {
            columnRSCM(COL_RAW_ITEM, "items.pie_shell")
            columnRSCM(COL_COOKED_ITEM, "items.apple_pie")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_pie")
            column(COL_LEVEL, 30)
            column(COL_XP, 130)
            column(COL_BURN_STOP_FIRE, 64)
            column(COL_BURN_STOP_RANGE, 58)
            column(COL_METHOD, 1)
        }

        // Plain pizza (level 35, range only)
        row("dbrows.cooking_plain_pizza") {
            columnRSCM(COL_RAW_ITEM, "items.pizza_base")
            columnRSCM(COL_COOKED_ITEM, "items.plain_pizza")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_pizza")
            column(COL_LEVEL, 35)
            column(COL_XP, 143)
            column(COL_BURN_STOP_FIRE, 68)
            column(COL_BURN_STOP_RANGE, 62)
            column(COL_METHOD, 1)
        }

        // Cake (level 40, range only)
        row("dbrows.cooking_cake") {
            columnRSCM(COL_RAW_ITEM, "items.cake_tin")
            columnRSCM(COL_COOKED_ITEM, "items.cake")
            columnRSCM(COL_BURNT_ITEM, "items.burnt_cake")
            column(COL_LEVEL, 40)
            column(COL_XP, 180)
            column(COL_BURN_STOP_FIRE, 74)
            column(COL_BURN_STOP_RANGE, 68)
            column(COL_METHOD, 1)
        }
    }
}
