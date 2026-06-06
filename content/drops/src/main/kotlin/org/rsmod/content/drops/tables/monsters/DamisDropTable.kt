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
public val damisDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Damis Drops",
    npcs = npcs("npc.fd_damis_normal", "npc.fd_damis_tougher"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.fd_dark_diamond" count 1 condition {
            player -> player.isOnQuest("quest_deserttreasure1")
        }
    },
)
