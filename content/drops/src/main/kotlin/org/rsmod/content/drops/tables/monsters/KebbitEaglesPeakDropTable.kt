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
public val kebbitEaglesPeakDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kebbit (Eagles' Peak) Drops",
    npcs = npcs("npc.eaglepeak_uber_kebbit"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.eaglepeak_crystal_feather2" count 1 condition {
            player -> player.isOnQuest("quest_eaglespeak")
        }
    },
)
