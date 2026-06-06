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
public val skogreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skogre Drops",
    npcs = npcs("npc.zogre_drummer2", "npc.zogre_skele", "npc.zogre_skele_dance"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.zogre_coffinkey" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: }}
             true
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1 condition { player ->
            // Drops Need Manual: Zombie champion scrolls are dropped by the skogre playing the drums in western Jiggig, and by skogres with the examine text
            // Drops Need Manual: }}
             true
        }
        1 outOf 4 weight "obj.rag_zogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
