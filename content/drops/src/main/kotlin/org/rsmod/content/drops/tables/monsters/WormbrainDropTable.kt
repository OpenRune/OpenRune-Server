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
public val wormbrainDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wormbrain Drops",
    npcs = npcs("npc.wormbrain"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mappart2" count 1 condition {
            player -> player.isOnQuest("quest_dragonslayer1")
        }
    },
)
