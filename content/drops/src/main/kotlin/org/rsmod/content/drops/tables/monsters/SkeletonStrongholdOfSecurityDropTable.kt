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
public val skeletonStrongholdOfSecurityDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Stronghold of Security) Drops",
    npcs = npcs("npc.sos_death_skeleton_armed", "npc.sos_death_skeleton_armed2", "npc.sos_death_skeleton_armed3", "npc.sos_death_skeleton_unarmed", "npc.sos_death_skeleton_unarmed2", "npc.sos_death_skeleton_unarmed3", "npc.sos_death_skeleton_unarmed4"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
    },
)
