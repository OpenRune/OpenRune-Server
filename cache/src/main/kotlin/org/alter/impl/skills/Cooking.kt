package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Cooking {

    const val RAW_ITEM = 0
    const val COOKED_ITEM = 1
    const val BURNT_ITEM = 2
    const val LEVEL = 3
    const val XP = 4
    const val HEAL = 5
    const val BURN_CHANCE = 6

    fun cookingRecipes() = dbTable("tables.cooking_recipes") {

        column("raw_item", RAW_ITEM, VarType.OBJ)
        column("cooked_item", COOKED_ITEM, VarType.OBJ)
        column("burnt_item", BURNT_ITEM, VarType.OBJ)
        column("level", LEVEL, VarType.INT)
        column("xp", XP, VarType.INT)
        column("heal", HEAL, VarType.INT)
        column("burn_chance", BURN_CHANCE, VarType.INT)

        // Meat recipes - Note: raw_meat doesn't exist, using raw_beef as generic meat
        // row("dbrows.cooking_raw_meat") {
        //     columnRSCM(RAW_ITEM, "items.raw_meat") // TODO: Add items.raw_meat to items.rscm or use specific meats
        //     columnRSCM(COOKED_ITEM, "items.cooked_meat")
        //     columnRSCM(BURNT_ITEM, "items.burnt_meat")
        //     column(LEVEL, 1)
        //     column(XP, 30)
        //     column(HEAL, 3)
        //     column(BURN_CHANCE, 50)
        // }

        row("dbrows.cooking_raw_chicken") {
            columnRSCM(RAW_ITEM, "items.raw_chicken")
            columnRSCM(COOKED_ITEM, "items.cooked_chicken")
            columnRSCM(BURNT_ITEM, "items.burnt_chicken")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_beef") {
            columnRSCM(RAW_ITEM, "items.raw_beef")
            columnRSCM(COOKED_ITEM, "items.cooked_meat")
            columnRSCM(BURNT_ITEM, "items.burnt_meat")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_rat_meat") {
            columnRSCM(RAW_ITEM, "items.raw_rat_meat")
            columnRSCM(COOKED_ITEM, "items.cooked_meat")
            columnRSCM(BURNT_ITEM, "items.burnt_meat")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_bear_meat") {
            columnRSCM(RAW_ITEM, "items.raw_bear_meat")
            columnRSCM(COOKED_ITEM, "items.cooked_meat")
            columnRSCM(BURNT_ITEM, "items.burnt_meat")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_ugthanki_meat") {
            columnRSCM(RAW_ITEM, "items.raw_ugthanki_meat")
            columnRSCM(COOKED_ITEM, "items.cooked_ugthanki_meat")
            columnRSCM(BURNT_ITEM, "items.burnt_meat")
            column(LEVEL, 1)
            column(XP, 40)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        // Fish recipes
        row("dbrows.cooking_raw_shrimp") {
            columnRSCM(RAW_ITEM, "items.raw_shrimp")
            columnRSCM(COOKED_ITEM, "items.shrimp")
            columnRSCM(BURNT_ITEM, "items.burnt_shrimp")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 3)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_sardine") {
            columnRSCM(RAW_ITEM, "items.raw_sardine")
            columnRSCM(COOKED_ITEM, "items.sardine")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 1)
            column(XP, 40)
            column(HEAL, 4)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_herring") {
            columnRSCM(RAW_ITEM, "items.raw_herring")
            columnRSCM(COOKED_ITEM, "items.herring")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 5)
            column(XP, 50)
            column(HEAL, 5)
            column(BURN_CHANCE, 40)
        }

        row("dbrows.cooking_raw_anchovy") {
            columnRSCM(RAW_ITEM, "items.raw_anchovies") // Note: plural form exists
            columnRSCM(COOKED_ITEM, "items.anchovies") // TODO: Verify cooked anchovies exists
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 1)
            column(XP, 30)
            column(HEAL, 1)
            column(BURN_CHANCE, 50)
        }

        row("dbrows.cooking_raw_mackerel") {
            columnRSCM(RAW_ITEM, "items.raw_mackerel")
            columnRSCM(COOKED_ITEM, "items.mackerel")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 10)
            column(XP, 60)
            column(HEAL, 6)
            column(BURN_CHANCE, 35)
        }

        row("dbrows.cooking_raw_trout") {
            columnRSCM(RAW_ITEM, "items.raw_trout")
            columnRSCM(COOKED_ITEM, "items.trout")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 15)
            column(XP, 70)
            column(HEAL, 7)
            column(BURN_CHANCE, 30)
        }

        row("dbrows.cooking_raw_cod") {
            columnRSCM(RAW_ITEM, "items.raw_cod")
            columnRSCM(COOKED_ITEM, "items.cod")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 18)
            column(XP, 75)
            column(HEAL, 7)
            column(BURN_CHANCE, 28)
        }

        row("dbrows.cooking_raw_pike") {
            columnRSCM(RAW_ITEM, "items.raw_pike")
            columnRSCM(COOKED_ITEM, "items.pike")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 20)
            column(XP, 80)
            column(HEAL, 8)
            column(BURN_CHANCE, 25)
        }

        row("dbrows.cooking_raw_salmon") {
            columnRSCM(RAW_ITEM, "items.raw_salmon")
            columnRSCM(COOKED_ITEM, "items.salmon")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 25)
            column(XP, 90)
            column(HEAL, 9)
            column(BURN_CHANCE, 20)
        }

        row("dbrows.cooking_raw_tuna") {
            columnRSCM(RAW_ITEM, "items.raw_tuna")
            columnRSCM(COOKED_ITEM, "items.tuna")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 30)
            column(XP, 100)
            column(HEAL, 10)
            column(BURN_CHANCE, 15)
        }

        row("dbrows.cooking_raw_lobster") {
            columnRSCM(RAW_ITEM, "items.raw_lobster")
            columnRSCM(COOKED_ITEM, "items.lobster")
            columnRSCM(BURNT_ITEM, "items.burnt_lobster")
            column(LEVEL, 40)
            column(XP, 120)
            column(HEAL, 12)
            column(BURN_CHANCE, 10)
        }

        row("dbrows.cooking_raw_bass") {
            columnRSCM(RAW_ITEM, "items.raw_bass")
            columnRSCM(COOKED_ITEM, "items.bass")
            columnRSCM(BURNT_ITEM, "items.burntfish2")
            column(LEVEL, 43)
            column(XP, 130)
            column(HEAL, 13)
            column(BURN_CHANCE, 8)
        }

        row("dbrows.cooking_raw_swordfish") {
            columnRSCM(RAW_ITEM, "items.raw_swordfish")
            columnRSCM(COOKED_ITEM, "items.swordfish")
            columnRSCM(BURNT_ITEM, "items.burnt_swordfish")
            column(LEVEL, 45)
            column(XP, 140)
            column(HEAL, 14)
            column(BURN_CHANCE, 5)
        }

        row("dbrows.cooking_raw_monkfish") {
            columnRSCM(RAW_ITEM, "items.raw_monkfish")
            columnRSCM(COOKED_ITEM, "items.monkfish")
            columnRSCM(BURNT_ITEM, "items.burnt_monkfish")
            column(LEVEL, 62)
            column(XP, 150)
            column(HEAL, 16)
            column(BURN_CHANCE, 3)
        }

        row("dbrows.cooking_raw_shark") {
            columnRSCM(RAW_ITEM, "items.raw_shark")
            columnRSCM(COOKED_ITEM, "items.shark")
            columnRSCM(BURNT_ITEM, "items.burnt_shark")
            column(LEVEL, 80)
            column(XP, 210)
            column(HEAL, 20)
            column(BURN_CHANCE, 2)
        }

        row("dbrows.cooking_raw_sea_turtle") {
            columnRSCM(RAW_ITEM, "items.raw_seaturtle")
            columnRSCM(COOKED_ITEM, "items.seaturtle")
            columnRSCM(BURNT_ITEM, "items.burnt_seaturtle")
            column(LEVEL, 82)
            column(XP, 211)
            column(HEAL, 21)
            column(BURN_CHANCE, 2)
        }

        row("dbrows.cooking_raw_manta_ray") {
            columnRSCM(RAW_ITEM, "items.raw_mantaray")
            columnRSCM(COOKED_ITEM, "items.mantaray")
            columnRSCM(BURNT_ITEM, "items.burnt_mantaray")
            column(LEVEL, 91)
            column(XP, 216)
            column(HEAL, 22)
            column(BURN_CHANCE, 1)
        }

        row("dbrows.cooking_raw_anglerfish") {
            columnRSCM(RAW_ITEM, "items.raw_anglerfish")
            columnRSCM(COOKED_ITEM, "items.anglerfish")
            columnRSCM(BURNT_ITEM, "items.burnt_anglerfish")
            column(LEVEL, 84)
            column(XP, 230)
            column(HEAL, 22)
            column(BURN_CHANCE, 1)
        }

        row("dbrows.cooking_raw_dark_crab") {
            columnRSCM(RAW_ITEM, "items.raw_dark_crab")
            columnRSCM(COOKED_ITEM, "items.dark_crab")
            columnRSCM(BURNT_ITEM, "items.burnt_dark_crab")
            column(LEVEL, 90)
            column(XP, 215)
            column(HEAL, 22)
            column(BURN_CHANCE, 1)
        }
    }
}

