package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val hesporiDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Hespori Drops",
    npcs = npcs("npc.hespori"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 35 weight "obj.bottomless_compost_bucket" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 80) {
        name("Hespori Drops")
        2 weight "obj.watermelon_seed" count 10..20
        2 weight "obj.snape_grass_seed" count 6..16
        5 weight "obj.white_lily_seed" count 8..18
        2 weight "obj.limpwurt_seed" count 6..14
        2 weight "obj.wildblood_hop_seed" count 10..20
        2 weight "obj.whiteberry_bush_seed" count 10..16
        2 weight "obj.poisonivy_bush_seed" count 8..16
        3 weight "obj.irit_seed" count 2..8
        3 weight "obj.avantoe_seed" count 2..5
        3 weight "obj.kwuarm_seed" count 2..5
        3 weight "obj.toadflax_seed" count 2..5
        3 weight "obj.cadantine_seed" count 2..5
        3 weight "obj.lantadyme_seed" count 2..5
        3 weight "obj.dwarf_weed_seed" count 2..5
        2 weight "obj.ranarr_seed" count 1..2
        2 weight "obj.snapdragon_seed" count 1
        2 weight "obj.torstol_seed" count 1
        4 weight "obj.maple_seed" count 2..4
        3 weight "obj.willow_seed" count 2..5
        2 weight "obj.yew_seed" count 1
        1 weight "obj.magic_tree_seed" count 1
        3 weight "obj.pineapple_tree_seed" count 3..6
        3 weight "obj.papaya_tree_seed" count 1..3
        3 weight "obj.palm_tree_seed" count 1..3
        2 weight "obj.dragonfruit_tree_seed" count 1
        4 weight "obj.teak_seed" count 2..5
        3 weight "obj.mahogany_seed" count 1..3
        2 weight "obj.cactus_seed" count 4..14
        2 weight "obj.potato_cactus_seed" count 4..14
        2 weight "obj.celastrus_tree_seed" count 1
        1 weight "obj.spirit_tree_seed" count 1
        1 weight "obj.redwood_tree_seed" count 1
        1 outOf 3 separate rsPlayerWeightedTable {
            1 weight "obj.attas_seed" count 1..2
            1 weight "obj.iasor_seed" count 1..2
            1 weight "obj.kronos_seed" count 1..2
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5375 weight "obj.skillpetfarming" count 1 condition { player ->
            // Drops Need Manual: Range is the chance from 65 to 99. The chance increases with the player's Farming level, and is calculated with the formula: \frac{1}{B - (Lvl * 25)} , where ''B'' is the base chance (7000 for Hespori) and ''Lvl'' is the player's Farming level. For more information, see Tangleroot.
             true
        }
    },
)
