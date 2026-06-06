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
public val chompyBirdDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chompy bird Drops",
    npcs = npcs("npc.chompybird"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chompy" count 1
        "obj.feather" count 10..30
    },
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Chompy bird Drops")
        1 weight "obj.chompybird_pet" count 1 condition { player ->
            // Drops Need Manual: Only players who have completed the elite Western Provinces Diary are eligible to receive this pet.
             true
        }
        499 weight nothing()
    },
)
