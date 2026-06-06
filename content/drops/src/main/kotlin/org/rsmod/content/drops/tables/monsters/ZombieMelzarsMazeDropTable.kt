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
public val zombieMelzarsMazeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie (Melzar's Maze) Drops",
    npcs = npcs("npc.dragonslayer_zombie_1_key", "npc.dragonslayer_zombie_2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.bluekey" count 1 condition { player ->
            // Drops Need Manual: Only dropped by one of the spawns.
             true
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
