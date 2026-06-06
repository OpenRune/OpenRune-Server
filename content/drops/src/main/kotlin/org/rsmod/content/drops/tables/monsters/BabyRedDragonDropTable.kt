package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val babyRedDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Baby red dragon Drops",
    npcs = npcs("npc.babyreddragon", "npc.babyreddragon2", "npc.babyreddragon3"),
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate to receive any tattered page is 1/25. When rolled, the player will receive the page they have the fewest of.
        // Drops Need Manual (rate): Tattered pages and grubby keys are only dropped by those found in the Forthos Dungeon. The drop rate to receive any tattered page is 1/25. When rolled, the player will receive the page they have the fewest of.
        1 outOf 25 weight "obj.hosdun_moon_page" count 1 condition { player ->
            // Drops Need Manual: Tattered pages and grubby keys are only dropped by those found in the Forthos Dungeon.
             true
        }
        1 outOf 25 weight "obj.hosdun_sun_page" count 1
        1 outOf 25 weight "obj.hosdun_temple_page" count 1
        1 outOf 80 weight "obj.hosdun_grubby_key" count 1
        onBuilder { brimstoneKeyRoll() }
    },
)
