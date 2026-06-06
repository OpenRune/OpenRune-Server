package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mammothDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mammoth Drops",
    npcs = npcs("npc.wilderness_mammoth"),
    mainTable = rsPlayerWeightedTable(total = 640) {
        name("Mammoth Drops")
        30 weight "obj.potato_seed" count 3
        20 weight "obj.onion_seed" count 3
        16 weight "obj.cabbage_seed" count 3
        14 weight "obj.tomato_seed" count 3
        10 weight "obj.sweetcorn_seed" count 3
        8 weight "obj.strawberry_seed" count 3
        2 weight "obj.watermelon_seed" count 3
        30 weight "obj.redberry_bush_seed" count 2
        20 weight "obj.cadavaberry_bush_seed" count 2
        16 weight "obj.dwellberry_bush_seed" count 2
        14 weight "obj.jangerberry_bush_seed" count 2
        10 weight "obj.whiteberry_bush_seed" count 2
        10 weight "obj.poisonivy_bush_seed" count 2
        30 weight "obj.barley_seed" count 4
        20 weight "obj.hammerstone_hop_seed" count 4
        16 weight "obj.asgarnian_hop_seed" count 4
        14 weight "obj.jute_seed" count 4
        10 weight "obj.yanillian_hop_seed" count 4
        8 weight "obj.krandorian_hop_seed" count 4
        2 weight "obj.wildblood_hop_seed" count 4
        18 weight "obj.apple_tree_seed" count 1
        12 weight "obj.banana_tree_seed" count 1
        10 weight "obj.orange_tree_seed" count 1
        6 weight "obj.curry_tree_seed" count 1
        3 weight "obj.pineapple_tree_seed" count 1
        1 weight "obj.papaya_tree_seed" count 1
        13 outOf 128 separate "obj.coins" count 30
        7 outOf 128 separate "obj.coins" count 180
        5 outOf 128 separate rsPlayerWeightedTable {
            5 weight "obj.acorn" count 1
            5 weight "obj.limpwurt_seed" count 2
            5 weight "obj.1doseprayerrestore" count 1
            5 weight "obj.steel_arrow" count 15
        }
        3 outOf 128 separate "obj.lobster" count 2
        2 outOf 128 separate "obj.wilderness_fishing_bait" count 12

        8 weight SharedDropTables.herb
        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
        277 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        // Drops Need Manual (rate): Medium clue scrolls have a drop rate of 1/64 if a ring of wealth (i) is worn.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
