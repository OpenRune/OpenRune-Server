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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mogreSailingDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mogre (Sailing) Drops",
    npcs = npcs("npc.sailing_sea_mogre", "npc.sailing_sea_mogre_f"),
    preRoll = rsPlayerPrerollTable {
        2 outOf 60 weight "obj.mudskipper_hat" count 1
        1 outOf 60 weight "obj.mudskipper_flippers" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Mogre (Sailing) Drops")
        20 weight "obj.raw_swordfish" count 1
        9 weight "obj.raw_tuna" count 1
        7 weight "obj.raw_pike" count 1
        4 weight "obj.raw_shark" count 1
        4 weight "obj.raw_salmon" count 1
        3 weight "obj.raw_herring" count 1
        3 weight "obj.raw_sardine" count 1
        23 weight "obj.fishing_bait" count 10..40
        16 weight "obj.seaweed" count 1
        7 weight "obj.oystershell" count 1
        6 weight "obj.waterrune" count 20..40
        4 weight "obj.giant_seaweed" count 1
        1 weight "obj.staff_of_water" count 1
        1 weight "obj.fishbowl_water" count 1
        20 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.sailing_sea_mogre_mace" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_sea_mogre_head" count 1
    },
)
