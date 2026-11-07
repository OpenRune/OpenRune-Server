package org.rsmod.content.areas.misc.lobbies.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack

class PestControlRewardPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(training(), packContents())

    private fun training() =
        dbTable("dbtable.pest_control_training", serverOnly = true) {
            column("selection", 0, VarType.INT)
            column("skill", 1, VarType.STAT)
            column("multiplier", 2, VarType.INT)

            fun skill(name: String, selection: Int, multiplier: Int) {
                row("dbrow.pest_control_training_$name") {
                    column(0, selection)
                    columnRSCM(1, "stat.$name")
                    column(2, multiplier)
                }
            }

            skill("attack", 0, 35)
            skill("strength", 3, 35)
            skill("defence", 6, 35)
            skill("ranged", 9, 32)
            skill("magic", 12, 32)
            skill("hitpoints", 15, 35)
            skill("prayer", 18, 18)
        }

    private fun packContents() =
        dbTable("dbtable.pest_control_pack_contents", serverOnly = true) {
            column("selection", 0, VarType.INT)
            column("item", 1, VarType.OBJ)
            column("minimum", 2, VarType.INT)
            column("maximum", 3, VarType.INT)
            column("weight", 4, VarType.INT)

            fun content(
                name: String,
                selection: Int,
                item: String,
                minimum: Int,
                maximum: Int = minimum,
                weight: Int = 1,
            ) {
                row("dbrow.pest_control_pack_$name") {
                    column(0, selection)
                    columnRSCM(1, "obj.$item")
                    column(2, minimum)
                    column(3, maximum)
                    column(4, weight)
                }
            }

            content("harralander", 21, "cert_unidentified_harralander", 2, 4)
            content("ranarr", 21, "cert_unidentified_ranarr", 2, 3)
            content("toadflax", 21, "cert_unidentified_toadflax", 1, 4)
            content("irit", 21, "cert_unidentified_irit", 2, 4)
            content("avantoe", 21, "cert_unidentified_avantoe", 1, 4)
            content("kwuarm", 21, "cert_unidentified_kwuarm", 1, 5)
            content("coal", 22, "cert_coal", 25)
            content("iron", 22, "cert_iron_ore", 18)
            content("spirit", 23, "spirit_tree_seed", 1, weight = 2)
            content("magic", 23, "magic_tree_seed", 1, weight = 2)
            content("yew", 23, "yew_seed", 1, weight = 16)
            content("maple", 23, "maple_seed", 1, weight = 20)
            content("willow", 23, "willow_seed", 1, weight = 40)
            content("acorn", 23, "acorn", 1, weight = 60)
            content("ranarr_seed", 23, "ranarr_seed", 1, weight = 40)
            content("sweetcorn", 23, "sweetcorn_seed", 3, weight = 240)
            content("strawberry", 23, "strawberry_seed", 2, weight = 180)
            content("limpwurt", 23, "limpwurt_seed", 2, weight = 200)
            content("watermelon", 23, "watermelon_seed", 2, weight = 180)
            content("tomato", 23, "tomato_seed", 6, weight = 220)
            content("cabbage", 23, "cabbage_seed", 9, weight = 200)
            content("onion", 23, "onion_seed", 11, weight = 200)
            content("potato", 23, "potato_seed", 14, weight = 400)
            content("snape_grass", 23, "snape_grass_seed", 2, weight = 120)
            content("celastrus", 23, "celastrus_tree_seed", 1, weight = 1)
            content("redwood", 23, "redwood_tree_seed", 1, weight = 1)
        }
}
