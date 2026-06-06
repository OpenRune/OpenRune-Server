package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val guardDogDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard dog Drops",
    npcs = npcs("npc.guarddog", "npc.hosidius_guarddog"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: Keys are only dropped by the guard dogs in Handelmort Mansion when completing a medium clue scroll asking you to kill one.
             true
        }
        1 outOf 25 weight "obj.arceuus_corpse_dog" count 1
    },
)
