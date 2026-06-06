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
public val undeadOneDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Undead one Drops",
    npcs = npcs("npc.zqskeleton_armed", "npc.zqskeleton_armed2", "npc.zqskeleton_unarmed", "npc.zqskeleton_unarmed2", "npc.zqzombie_armed", "npc.zqzombie_armed2", "npc.zqzombie_armed3", "npc.zqzombie_unarmed", "npc.zqzombie_unarmed2", "npc.zqzombie_unarmed3"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Skeleton champion scrolls are only dropped by the skeletal variant.
             true
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1 condition { player ->
            // Drops Need Manual: Zombie champion scrolls are only dropped by the zombie variant.
             true
        }
    },
)
