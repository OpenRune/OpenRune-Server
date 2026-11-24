package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ApothecaryPotionsTable {
    const val KEY = 0
    const val TITLE = 1
    const val ARTICLE = 2
    const val PRODUCT = 3
    const val COINS = 4
    const val INGREDIENTS = 5
    const val AMOUNTS = 6
    const val LABELS = 7

    fun potions() =
        dbTable("dbtable.apothecary_potions", serverOnly = true) {
            column("key", KEY, VarType.STRING)
            column("title", TITLE, VarType.STRING)
            column("article", ARTICLE, VarType.STRING)
            column("product", PRODUCT, VarType.OBJ)
            column("coins", COINS, VarType.INT)
            column("ingredients", INGREDIENTS, VarType.OBJ)
            column("amounts", AMOUNTS, VarType.INT)
            column("labels", LABELS, VarType.STRING)

            row("dbrow.apothecary_strength") {
                column(KEY, "strength")
                column(TITLE, "Strength potion")
                column(ARTICLE, "a")
                columnRSCM(PRODUCT, "obj.strength4")
                column(COINS, 5)
                columnRSCM(INGREDIENTS, "obj.red_spiders_eggs", "obj.limpwurt_root")
                column(AMOUNTS, 1, 1)
                column(LABELS, "Red spiders' eggs", "Limpwurt root")
            }
            row("dbrow.apothecary_energy") {
                column(KEY, "energy")
                column(TITLE, "Energy potion")
                column(ARTICLE, "an")
                columnRSCM(PRODUCT, "obj.4dose1energy")
                column(COINS, 0)
                columnRSCM(INGREDIENTS, "obj.chocolate_dust", "obj.limpwurt_root")
                column(AMOUNTS, 1, 2)
                column(LABELS, "Chocolate dust", "2 limpwurt roots")
            }
            row("dbrow.apothecary_antipoison") {
                column(KEY, "antipoison")
                column(TITLE, "Antipoison potion")
                column(ARTICLE, "an")
                columnRSCM(PRODUCT, "obj.4doseantipoison")
                column(COINS, 5)
                columnRSCM(INGREDIENTS, "obj.cadavaberries", "obj.limpwurt_root")
                column(AMOUNTS, 1, 1)
                column(LABELS, "Cadava berry", "Limpwurt root")
            }
        }
}
