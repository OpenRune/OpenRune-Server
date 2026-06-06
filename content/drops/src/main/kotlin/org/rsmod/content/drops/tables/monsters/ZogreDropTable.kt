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
public val zogreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zogre Drops",
    npcs = npcs("npc.zogre_1", "npc.zogre_2", "npc.zogre_3", "npc.zogre_4", "npc.zogre_5", "npc.zogre_6", "npc.zogre_dance_1", "npc.zogre_dance_2", "npc.zogre_dance_3", "npc.zogre_dance_4", "npc.zogre_drummer1"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.zogre_coffinkey" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
