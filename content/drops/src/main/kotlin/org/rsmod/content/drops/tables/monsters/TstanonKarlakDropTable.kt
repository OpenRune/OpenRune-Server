package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tstanonKarlakDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tstanon Karlak Drops",
    npcs = npcs("npc.godwars_ancient_greater_demon"),
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Tstanon Karlak Drops")
        7 weight "obj.steel_arrow" count 95..100
        8 weight "obj.steel_dart" count 95..100
        8 weight "obj.deathrune" count 5..10
        8 weight "obj.bloodrune" count 5..10
        66 weight "obj.coins" count 1300..1400
        8 weight "obj.shark" count 3
        8 weight "obj.potato_tuna+sweetcorn" count 2
        8 weight "obj.cert_wine_of_zamorak" count 5..10
        2 weight "obj.3dose2attack" count 1
        2 weight "obj.3dose2strength" count 1
        3 outOf 16129 separate "obj.zamorak_spear" count 1
        124 outOf 16129 separate "obj.coins" count 1300..1400
        1 outOf 1524 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        9 outOf 1524 separate "obj.coins" count 1300..1400
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.nex_frozen_key_zamorak" count 1 condition { player ->
            // Drops Need Manual: [[Frozen key (The Frozen Door)
             true
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
