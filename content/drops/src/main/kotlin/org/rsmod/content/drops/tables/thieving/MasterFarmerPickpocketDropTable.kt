package org.rsmod.content.drops.tables.thieving

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks
import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import kotlin.math.min
import kotlin.random.Random
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.stat.statBase
import org.rsmod.game.entity.Player

/**
 * Herb seed sub-table from the wiki's master farmer seed calculator: ranarr, snapdragon and torstol
 * share `6 + min(85, Farming)` of every 1000 slots in a 69:10:2 split, taken from guam's share.
 * Weights are kept in 81000ths so the split stays integral.
 */
private object MasterFarmerHerbSeeds :
    Rollable<Player, DropRollItem>,
    RollableHooks<Player, DropRollItem> by RollableHooks.Default() {
    private const val SPECIAL_SPLIT = 81
    private const val SPECIAL_BASE = 6
    private const val FARMING_CAP = 85
    private const val GUAM_BASE = 320

    private val fixed =
        listOf(
            "obj.marrentill_seed" to 218,
            "obj.tarromin_seed" to 149,
            "obj.harralander_seed" to 101,
            "obj.toadflax_seed" to 47,
            "obj.irit_seed" to 32,
            "obj.avantoe_seed" to 22,
            "obj.kwuarm_seed" to 15,
            "obj.cadantine_seed" to 7,
            "obj.lantadyme_seed" to 5,
            "obj.dwarf_weed_seed" to 3,
        )

    override fun selectResult(target: Player, otherArgs: ArgMap): RollResult<DropRollItem> {
        val special = SPECIAL_BASE + min(FARMING_CAP, target.statBase("stat.farming"))
        val weights =
            fixed.map { (obj, weight) -> obj to weight * SPECIAL_SPLIT } +
                listOf(
                    "obj.guam_seed" to (GUAM_BASE + SPECIAL_SPLIT - special) * SPECIAL_SPLIT,
                    "obj.ranarr_seed" to 69 * special,
                    "obj.snapdragon_seed" to 10 * special,
                    "obj.torstol_seed" to 2 * special,
                )
        var pick = Random.nextInt(weights.sumOf { it.second })
        for ((obj, weight) in weights) {
            if (pick < weight) return RollResult.Single(DropRollItem(obj, 1))
            pick -= weight
        }
        return RollResult.Nothing()
    }
}

@field:RegisterDropTable
@JvmField
public val masterFarmerPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Master Farmer (Pickpocket)",
    pickpockets = pickpockets("master_farmer"),
    mainTable = rsPlayerWeightedTable(total = 100032) {
        name("Master Farmer (Pickpocket)")
        17700 weight "obj.potato_seed" count 1..4
        13280 weight "obj.onion_seed" count 1..3
        6944 weight "obj.cabbage_seed" count 1..3
        6369 weight "obj.tomato_seed" count 1..2
        2212 weight "obj.sweetcorn_seed" count 1..2
        1106 weight "obj.strawberry_seed" count 1
        529 weight "obj.watermelon_seed" count 1
        385 weight "obj.snape_grass_seed" count 1
        5556 weight "obj.barley_seed" count 1..12
        5556 weight "obj.hammerstone_hop_seed" count 1..9
        4184 weight "obj.asgarnian_hop_seed" count 1..6
        4149 weight "obj.jute_seed" count 1..9
        2770 weight "obj.yanillian_hop_seed" count 1..6
        1385 weight "obj.krandorian_hop_seed" count 1..6
        704 weight "obj.wildblood_hop_seed" count 1..3
        4587 weight "obj.marigold_seed" count 1
        3040 weight "obj.nasturtium_seed" count 1
        1965 weight "obj.rosemary_seed" count 1
        1451 weight "obj.woad_seed" count 1
        1159 weight "obj.limpwurt_seed" count 1
        3876 weight "obj.redberry_bush_seed" count 1
        2717 weight "obj.cadavaberry_bush_seed" count 1
        1942 weight "obj.dwellberry_bush_seed" count 1
        775 weight "obj.jangerberry_bush_seed" count 1
        282 weight "obj.whiteberry_bush_seed" count 1
        107 weight "obj.poisonivy_bush_seed" count 1
        203 weight "obj.mushroom_seed" count 1
        122 weight "obj.belladonna_seed" count 1
        81 weight "obj.cactus_seed" count 1
        53 weight "obj.seaweed_seed" count 1
        41 weight "obj.potato_cactus_seed" count 1
        4802 weight MasterFarmerHerbSeeds
    },
)
