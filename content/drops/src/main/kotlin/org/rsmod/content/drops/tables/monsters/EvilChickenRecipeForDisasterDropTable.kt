package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val evilChickenRecipeForDisasterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Evil Chicken (Recipe for Disaster) Drops",
    npcs = npcs("npc.chickenquest_evil_chicken"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chicken" count 1
        "obj.feather" count (50..250) condition { player ->
            // Drops Need Manual: The quantity of feathers scales with your combat level. A ring of wealth may also increase the quantity.
             true
        }
        "obj.chickenquest_evil_chicken_egg" count 1 condition {
            player -> player.isOnQuest("quest_recipefordisaster")
        }
    },
)
