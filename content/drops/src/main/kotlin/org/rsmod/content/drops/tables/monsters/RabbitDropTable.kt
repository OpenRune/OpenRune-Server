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
public val rabbitDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rabbit Drops",
    npcs = npcs("npc.fairy2_rabbit1", "npc.fairy2_rabbit2", "npc.misc_rabbit", "npc.misc_rabbit2", "npc.rabbit_1", "npc.rabbit_2", "npc.rabbit_3", "npc.regicide_bunny1", "npc.regicide_bunny2", "npc.regicide_rabbit"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_rabbit" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_rabbit_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
