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
public val zombieRatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie rat Drops",
    npcs = npcs("npc.dragonslayer_giantrat_1_key", "npc.dragonslayer_giantrat_2", "npc.dragonslayer_giantrat_3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.redkey" count 1 condition { player ->
            // Drops Need Manual: Only dropped by the small rat with a long tail.
             true
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.rag_giant_rat_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
    },
)
