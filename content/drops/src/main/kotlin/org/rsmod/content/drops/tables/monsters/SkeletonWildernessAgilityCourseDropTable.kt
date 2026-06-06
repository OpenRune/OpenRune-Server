package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skeletonWildernessAgilityCourseDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Wilderness Agility Course) Drops",
    npcs = npcs("npc.wildy_agility_skeleton_1", "npc.wildy_agility_skeleton_1_longhunt", "npc.wildy_agility_skeleton_2", "npc.wildy_agility_skeleton_2_longhunt", "npc.wildy_agility_skeleton_3", "npc.wildy_agility_skeleton_4", "npc.wildy_agility_skeleton_5"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
    },
)
