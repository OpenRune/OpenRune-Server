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
public val nazastaroolDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Nazastarool Drops",
    npcs = npcs("npc.zq_mainzombie1", "npc.zq_mainzombie2", "npc.zq_mainzombie3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.zqcorpse" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Zombie Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
    },
)
