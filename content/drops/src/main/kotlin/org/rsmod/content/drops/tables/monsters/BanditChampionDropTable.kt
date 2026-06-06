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
public val banditChampionDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bandit champion Drops",
    npcs = npcs("npc.feud_bandit_toughguy"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.blackjack_willow" count 1 condition { player ->
            // Drops Need Manual: Only if the player does not have a willow blackjack in their inventory.
             true
        }
        "obj.adamant_scimitar" count 1 condition { player ->
            // Drops Need Manual: Only if the player already has a willow blackjack in their inventory.
             true
        }
    },
)
