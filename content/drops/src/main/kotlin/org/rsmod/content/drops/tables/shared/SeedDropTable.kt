package org.rsmod.content.drops.tables.shared

import dtx.core.singleRollable
import dtx.rs.RSWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.KillRollContext
import org.rsmod.game.entity.Player
import kotlin.random.Random

public val generalSeedDropTable: RSWeightedTable<Player, DropRollItem> = rsWeightedTable {
    name("General seed drop table")
    1 weight singleRollable<Player, DropRollItem> {
        selectResult { player, args ->
            val npc = requireNotNull(args[KillRollContext.npc]) { "General seeds need a killed NPC" }
            val roll = Random.nextInt(Math.multiplyExact(npc.type.combatLevel, 10))
            GENERAL_SEED_TABLES[generalSeedTier(roll)].roll(player, args)
        }
    }
}

internal fun generalSeedTier(roll: Int): Int {
    require(roll >= 0)
    return when {
        roll < 485 -> 0
        roll < 728 -> 1
        roll < 850 -> 2
        roll < 947 -> 3
        roll < 995 -> 4
        else -> 5
    }
}

private val GENERAL_SEED_TABLES = listOf(
    rsWeightedTable<Player, DropRollItem>(total = 1008) {
        368 weight DropRollItem("obj.potato_seed", 4)
        276 weight DropRollItem("obj.onion_seed", 4)
        184 weight DropRollItem("obj.cabbage_seed", 4)
        92 weight DropRollItem("obj.tomato_seed", 3)
        46 weight DropRollItem("obj.sweetcorn_seed", 3)
        23 weight DropRollItem("obj.strawberry_seed", 2)
        11 weight DropRollItem("obj.watermelon_seed", 2)
        8 weight DropRollItem("obj.snape_grass_seed", 2)
    },
    rsWeightedTable<Player, DropRollItem>(total = 1000) {
        229 weight DropRollItem("obj.barley_seed", 4)
        228 weight DropRollItem("obj.hammerstone_hop_seed", 3)
        172 weight DropRollItem("obj.asgarnian_hop_seed", 3)
        171 weight DropRollItem("obj.jute_seed", 2)
        114 weight DropRollItem("obj.yanillian_hop_seed", 2)
        57 weight DropRollItem("obj.krandorian_hop_seed", 2)
        29 weight DropRollItem("obj.wildblood_hop_seed", 1)
    },
    rsWeightedTable<Player, DropRollItem>(total = 1000) {
        376 weight DropRollItem("obj.marigold_seed", 1)
        249 weight DropRollItem("obj.nasturtium_seed", 1)
        161 weight DropRollItem("obj.rosemary_seed", 1)
        119 weight DropRollItem("obj.woad_seed", 1)
        95 weight DropRollItem("obj.limpwurt_seed", 1)
    },
    rsWeightedTable<Player, DropRollItem>(total = 1000) {
        400 weight DropRollItem("obj.redberry_bush_seed", 1)
        280 weight DropRollItem("obj.cadavaberry_bush_seed", 1)
        200 weight DropRollItem("obj.dwellberry_bush_seed", 1)
        80 weight DropRollItem("obj.jangerberry_bush_seed", 1)
        29 weight DropRollItem("obj.whiteberry_bush_seed", 1)
        11 weight DropRollItem("obj.poisonivy_bush_seed", 1)
    },
    rsWeightedTable<Player, DropRollItem>(total = 1000) {
        320 weight DropRollItem("obj.guam_seed", 1)
        218 weight DropRollItem("obj.marrentill_seed", 1)
        149 weight DropRollItem("obj.tarromin_seed", 1)
        101 weight DropRollItem("obj.harralander_seed", 1)
        69 weight DropRollItem("obj.ranarr_seed", 1)
        47 weight DropRollItem("obj.toadflax_seed", 1)
        32 weight DropRollItem("obj.irit_seed", 1)
        22 weight DropRollItem("obj.avantoe_seed", 1)
        15 weight DropRollItem("obj.kwuarm_seed", 1)
        10 weight DropRollItem("obj.snapdragon_seed", 1)
        7 weight DropRollItem("obj.cadantine_seed", 1)
        5 weight DropRollItem("obj.lantadyme_seed", 1)
        3 weight DropRollItem("obj.dwarf_weed_seed", 1)
        2 weight DropRollItem("obj.torstol_seed", 1)
    },
    rsWeightedTable<Player, DropRollItem>(total = 1100) {
        500 weight DropRollItem("obj.mushroom_seed", 1)
        300 weight DropRollItem("obj.belladonna_seed", 1)
        200 weight DropRollItem("obj.cactus_seed", 1)
        100 weight DropRollItem("obj.potato_cactus_seed", 1)
    },
)
