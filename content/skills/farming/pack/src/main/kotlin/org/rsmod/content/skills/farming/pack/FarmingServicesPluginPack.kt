package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack
import dev.openrune.rscm.RSCM.asRSCM

class FarmingServicesPluginPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(
        dbTable("dbtable.farming_services", serverOnly = true) {
            column("tools", 0, VarType.OBJ, VarType.STRING, VarType.STRING)
            column("gardeners", 1, VarType.NPC, VarType.LOC, VarType.INT)
            column("noting_add", 2, VarType.OBJ)
            column("noting_exclude", 3, VarType.OBJ)
            column("advice", 4, VarType.STRING)
            column("removal_prices", 5, VarType.STRING, VarType.INT)
            column("cabbage_refusal_coord", 6, VarType.COORDGRID)
            column("iknami_gardener", 7, VarType.NPC)

            val tools = mutableListOf<Any>()
            fun tool(obj: String, component: String, varbits: String) {
                tools += obj.asRSCM()
                tools += component
                tools += varbits.split(',').joinToString(",") { "varbit.farming_tools_$it" }
            }
            tool("obj.rake", "rake", "rake,extrarakes")
            tool("obj.dibber", "dibber", "dibber,extradibbers")
            tool("obj.spade", "spade", "spade,extraspades")
            tool("obj.secateurs", "secateurs", "secateurs,extrasecateurs")
            tool("obj.watering_can_dummy", "wateringcan", "wateringcan")
            tool("obj.gardening_trowel", "trowel", "trowel,extratrowels")
            tool("obj.plant_cure", "plantcure", "plantcure")
            tool("obj.bottomless_compost_bucket", "bottomless_bucket", "bottomless_bucket_type")
            tool("obj.bucket_empty", "bucket", "buckets,extrabuckets,extra2buckets")
            tool("obj.bucket_compost", "compost", "compost,extracompost")
            tool("obj.bucket_supercompost", "supercompost", "supercompost,extrasupercompost")
            tool("obj.bucket_ultracompost", "ultracompost", "ultracompost")

            val gardeners = mutableListOf<Int>()
            fun gardener(npc: String, loc: String, op: Int) {
                gardeners += listOf(npc.asRSCM(), loc.asRSCM(), op)
            }
            gardener("npc.elstan", "loc.farming_veg_patch_1", 3)
            gardener("npc.elstan", "loc.farming_veg_patch_2", 4)
            gardener("npc.lyra", "loc.farming_veg_patch_7", 3)
            gardener("npc.lyra", "loc.farming_veg_patch_8", 4)
            gardener("npc.dantaera", "loc.farming_veg_patch_3", 3)
            gardener("npc.dantaera", "loc.farming_veg_patch_4", 4)
            gardener("npc.kragen", "loc.farming_veg_patch_5", 3)
            gardener("npc.kragen", "loc.farming_veg_patch_6", 4)
            gardener("npc.hosidius_allotment_gardener", "loc.farming_veg_patch_10", 3)
            gardener("npc.hosidius_allotment_gardener", "loc.farming_veg_patch_11", 4)
            gardener("npc.farming_gardener_farmguild_t1", "loc.farming_veg_patch_12", 3)
            gardener("npc.farming_gardener_farmguild_t1", "loc.farming_veg_patch_13", 3)
            gardener("npc.prif_gardener", "loc.farming_veg_patch_14", 3)
            gardener("npc.prif_gardener", "loc.farming_veg_patch_15", 4)
            gardener("npc.fortis_gardener", "loc.farming_veg_patch_16", 3)
            gardener("npc.fortis_gardener", "loc.farming_veg_patch_17", 4)
            gardener("npc.farming_gardener_hops_3", "loc.farming_hops_patch_3", 3)
            gardener("npc.farming_gardener_hops_4", "loc.farming_hops_patch_4", 3)
            gardener("npc.farming_gardener_hops_1", "loc.farming_hops_patch_1", 3)
            gardener("npc.francis", "loc.farming_hops_patch_2", 3)
            gardener("npc.farming_gardener_hops_5", "loc.farming_hops_patch_5", 3)
            gardener("npc.farming_gardener_bush_1", "loc.farming_bush_patch_1", 3)
            gardener("npc.farming_gardener_bush_2", "loc.farming_bush_patch_2", 3)
            gardener("npc.farming_gardener_bush_4", "loc.farming_bush_patch_4", 3)
            gardener("npc.farming_gardener_bush_3", "loc.farming_bush_patch_3", 3)
            gardener("npc.farming_gardener_farmguild_t1", "loc.farming_bush_patch_5", 3)
            gardener("npc.farming_gardener_tree_4", "loc.farming_tree_patch_4", 3)
            gardener("npc.farming_gardener_tree_3", "loc.farming_tree_patch_3", 3)
            gardener("npc.farming_gardener_tree_3_02", "loc.farming_tree_patch_3", 3)
            gardener("npc.farming_gardener_tree_2", "loc.farming_tree_patch_2", 3)
            gardener("npc.farming_gardener_tree_1", "loc.farming_tree_patch_1", 3)
            gardener("npc.farming_gardener_tree_gnome", "loc.farming_tree_patch_5", 3)
            gardener("npc.farming_gardener_farmguild_t2", "loc.farming_tree_patch_6", 3)
            gardener("npc.farming_gardener_tree_7", "loc.farming_tree_patch_7", 3)
            gardener("npc.farming_gardener_fruit_1", "loc.farming_fruit_tree_patch_1", 3)
            gardener("npc.farming_gardener_fruit_4", "loc.farming_fruit_tree_patch_4", 3)
            gardener("npc.farming_gardener_fruit_2", "loc.farming_fruit_tree_patch_2", 3)
            gardener("npc.garth", "loc.farming_fruit_tree_patch_3", 3)
            gardener("npc.farming_gardener_fruit_tree_5", "loc.farming_fruit_tree_patch_5", 3)
            gardener("npc.farming_gardener_farmguild_t3", "loc.farming_fruit_tree_patch_6", 3)
            gardener("npc.farming_gardener_fruit_7", "loc.farming_fruit_tree_patch_7", 3)
            gardener("npc.farming_gardener_spirit_tree_2", "loc.farming_spirit_tree_patch_2", 3)
            gardener("npc.farming_gardener_spirit_tree_1", "loc.farming_spirit_tree_patch_1", 3)
            gardener("npc.farming_gardener_spirit_tree_3", "loc.farming_spirit_tree_patch_3", 3)
            gardener("npc.farming_gardener_spirit_tree_4", "loc.farming_spirit_tree_patch_4", 3)
            gardener("npc.farming_gardener_spirit_tree_5", "loc.farming_spirit_tree_patch_5", 3)
            gardener("npc.fossil_gardener_underwater", "loc.farming_seaweed_patch_1", 3)
            gardener("npc.fossil_gardener_underwater", "loc.farming_seaweed_patch_2", 4)
            gardener("npc.frog_quest_marcellus_farmer", "loc.farming_hardwood_tree_patch_4", 3)
            gardener("npc.farming_gardener_hardwood_tree_5", "loc.farming_hardwood_tree_patch_5", 3)
            gardener("npc.farming_gardener_calquat", "loc.farming_calquat_tree_patch", 3)
            gardener("npc.farming_gardener_calquat_3", "loc.farming_calquat_tree_patch_3", 3)
            gardener("npc.farming_gardener_calquat_2", "loc.farming_calquat_tree_patch_2", 3)
            gardener("npc.farming_gardener_farmguild_celastrus", "loc.farming_celastrus_patch_1", 3)
            gardener("npc.farming_gardener_farmguild_redwood", "loc.farming_redwood_tree_patch_0_1", 3)
            gardener("npc.farming_gardener_cactus", "loc.farming_cactus_patch", 3)
            gardener("npc.farming_gardener_farmguild_t1", "loc.farming_cactus_patch_2", 3)

            row("dbrow.farming_services") {
                column(0, *tools.toTypedArray())
                column(1, *gardeners.toTypedArray())
                column(2, "obj.weeds".asRSCM(), "obj.willow_branch".asRSCM(), "obj.huasca".asRSCM())
                column(3, "obj.stackable_nightshade".asRSCM(), "obj.plantpot_crystal_tree_sapling".asRSCM(), "obj.plantpot_spirit_tree_sapling".asRSCM())
                column(4,
                    "Tree seeds must be grown in a plantpot of soil into a sapling, and then transferred to a tree patch to continue growing to adulthood.",
                    "You can put up to ten potatoes, cabbages or onions in vegetable sacks, although you can't have a mix in the same sack.",
                    "You can put up to five tomatoes, strawberries, apples, bananas or oranges into a fruit basket, although you can't have a mix in the same basket.",
                    "You can fill plantpots with soil from any empty patch, if you have a gardening trowel.",
                )
                column(5, "TREE", 200, "FRUIT_TREE", 200, "HARDWOOD_TREE", 200, "REDWOOD", 2000)
                column(6, org.rsmod.map.CoordGrid(0, 47, 51, 45, 41).packed)
                column(7, "npc.farming_gardener_tree_7".asRSCM())
            }
        },
    )
}
