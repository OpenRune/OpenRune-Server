package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.map.CoordGrid

class FarmingGuildPack : PluginPack() {
    override fun dbTables(): List<DBTable> = listOf(guild())

    private fun guild() = dbTable("dbtable.farming_guild", serverOnly = true) {
        column("contracts", 0, VarType.INT, VarType.OBJ, VarType.INT, VarType.INT)
        column("rewards", 1, VarType.INT, VarType.OBJ, VarType.INT, VarType.INT, VarType.INT)
        column("rolls", 2, VarType.INT, VarType.INT, VarType.INT, VarType.INT, VarType.INT, VarType.INT, VarType.INT)
        column("gates", 3, VarType.INT, VarType.COORDGRID, VarType.COORDGRID, VarType.STRING)
        column("entrance", 4, VarType.LOC, VarType.LOC, VarType.COORDGRID, VarType.INT)
        val contracts = mutableListOf<Int>()
        fun pool(tier: Int, unlock: Int, reward: Int, vararg seeds: String) {
            for (seed in seeds) contracts += listOf(tier, "obj.$seed".asRSCM(), unlock, reward)
        }
        pool(1, 45, 1, "potato_seed", "onion_seed", "cabbage_seed", "tomato_seed", "sweetcorn_seed", "strawberry_seed", "marigold_seed", "rosemary_seed", "nasturtium_seed", "woad_seed", "limpwurt_seed")
        pool(1, 45, 2, "redberry_bush_seed", "cadavaberry_bush_seed", "dwellberry_bush_seed")
        pool(1, 48, 2, "jangerberry_bush_seed")
        pool(1, 55, 3, "cactus_seed")
        pool(1, 65, 2, "guam_seed", "marrentill_seed", "tarromin_seed", "harralander_seed", "ranarr_seed", "toadflax_seed", "irit_seed", "avantoe_seed")
        pool(1, 65, 3, "acorn", "willow_seed", "maple_seed")
        pool(1, 85, 3, "apple_tree_seed", "banana_tree_seed", "orange_tree_seed", "curry_tree_seed")
        pool(2, 65, 2, "strawberry_seed", "white_lily_seed", "snape_grass_seed", "potato_cactus_seed")
        pool(2, 65, 3, "irit_seed", "avantoe_seed", "kwuarm_seed", "snapdragon_seed", "watermelon_seed", "jangerberry_bush_seed", "whiteberry_bush_seed")
        pool(2, 67, 3, "cadantine_seed")
        pool(2, 70, 3, "poisonivy_bush_seed")
        pool(2, 73, 3, "lantadyme_seed")
        pool(2, 65, 4, "maple_seed", "cactus_seed", "yew_seed")
        pool(2, 75, 4, "magic_tree_seed")
        pool(2, 85, 4, "curry_tree_seed", "pineapple_tree_seed", "papaya_tree_seed", "palm_tree_seed")
        pool(3, 85, 3, "white_lily_seed", "snape_grass_seed", "potato_cactus_seed")
        pool(3, 85, 4, "watermelon_seed", "whiteberry_bush_seed", "snapdragon_seed", "cadantine_seed", "poisonivy_bush_seed", "lantadyme_seed", "dwarf_weed_seed", "torstol_seed")
        pool(3, 85, 5, "maple_seed", "yew_seed", "palm_tree_seed", "magic_tree_seed", "dragonfruit_tree_seed", "celastrus_tree_seed")
        pool(3, 90, 5, "redwood_tree_seed")

        val rewards = mutableListOf<Int>()
        fun reward(group: Int, min: Int, max: Int, weight: Int, vararg seeds: String) {
            for (seed in seeds) rewards += listOf(group, "obj.$seed".asRSCM(), min, max, weight)
        }
        reward(0, 8, 12, 2, "potato_seed", "onion_seed", "cabbage_seed", "tomato_seed", "sweetcorn_seed", "strawberry_seed", "jute_seed", "marigold_seed", "rosemary_seed", "nasturtium_seed", "woad_seed")
        reward(0, 8, 14, 2, "barley_seed")
        reward(0, 6, 8, 2, "hammerstone_hop_seed", "asgarnian_hop_seed", "yanillian_hop_seed", "krandorian_hop_seed", "redberry_bush_seed", "cadavaberry_bush_seed", "dwellberry_bush_seed", "jangerberry_bush_seed")
        reward(0, 3, 5, 2, "acorn", "apple_tree_seed", "banana_tree_seed", "orange_tree_seed", "curry_tree_seed", "guam_seed", "marrentill_seed", "tarromin_seed", "harralander_seed")
        reward(0, 4, 6, 1, "mushroom_seed", "belladonna_seed")
        reward(1, 2, 6, 3, "irit_seed")
        reward(1, 4, 8, 3, "limpwurt_seed")
        reward(1, 8, 12, 2, "watermelon_seed", "wildblood_hop_seed")
        reward(1, 6, 8, 2, "snape_grass_seed", "whiteberry_bush_seed", "poisonivy_bush_seed")
        reward(1, 2, 6, 2, "cactus_seed", "potato_cactus_seed")
        reward(1, 2, 4, 1, "willow_seed")
        reward(1, 3, 5, 1, "pineapple_tree_seed")
        reward(1, 1, 3, 1, "toadflax_seed", "avantoe_seed", "kwuarm_seed", "cadantine_seed", "lantadyme_seed", "dwarf_weed_seed", "teak_seed")
        reward(1, 3, 6, 1, "calquat_tree_seed")
        reward(2, 1, 3, 5, "papaya_tree_seed")
        reward(2, 1, 2, 5, "palm_tree_seed")
        reward(2, 1, 1, 5, "hespori_seed")
        reward(2, 1, 2, 4, "ranarr_seed", "maple_seed", "mahogany_seed")
        reward(2, 1, 1, 4, "snapdragon_seed")
        reward(2, 1, 1, 3, "yew_seed", "dragonfruit_tree_seed")
        reward(2, 1, 1, 2, "celastrus_tree_seed", "torstol_seed")
        reward(2, 1, 1, 1, "magic_tree_seed", "spirit_tree_seed", "redwood_tree_seed")
        row("dbrow.farming_guild") {
            column(0, *contracts.toTypedArray())
            column(1, *rewards.toTypedArray())
            column(2,
                1, 6, 1, 3, 0, 0, 1,
                2, 7, 2, 3, 0, 1, 11,
                3, 8, 2, 4, 0, 1, 1,
                4, 9, 3, 5, 1, 2, 1,
                5, 10, 4, 6, 1, 3, 1,
            )
            column(3,
                1, CoordGrid(1248, 3723, 0).packed, CoordGrid(1249, 3723, 0).packed, "NORTH",
                2, CoordGrid(1243, 3729, 0).packed, CoordGrid(1243, 3730, 0).packed, "WEST",
                3, CoordGrid(1248, 3745, 0).packed, CoordGrid(1249, 3745, 0).packed, "NORTH",
            )
            column(4,
                "loc.kebos_farming_guild_door_left_closed".asRSCM(),
                "loc.kebos_farming_guild_door_left_open".asRSCM(), CoordGrid(1248, 3723, 0).packed, 3,
                "loc.kebos_farming_guild_door_right_closed".asRSCM(),
                "loc.kebos_farming_guild_door_right_open".asRSCM(), CoordGrid(1249, 3723, 0).packed, 1,
            )
        }
    }
}
