package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skeletonMelzarsMazeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Melzar's Maze) Drops",
    npcs = npcs("npc.dragonslayer_skeleton_1_key"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.yellowkey" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
    },
)

@field:RegisterDropTable
@JvmField
public val skeletonMelzarsMazeDropTableNoKey: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Melzar's Maze) Drops (no key)",
    npcs = npcs("npc.dragonslayer_skeleton_2", "npc.dragonslayer_skeleton_3"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Coins [main/Common]
