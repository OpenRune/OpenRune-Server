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
public val doomionDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Doomion Drops",
    npcs = npcs("npc.doomion_vis"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.doomion_amulet" count 1 condition {
            player -> player.isOnQuest("quest_undergroundpass")
        }
    },
)
