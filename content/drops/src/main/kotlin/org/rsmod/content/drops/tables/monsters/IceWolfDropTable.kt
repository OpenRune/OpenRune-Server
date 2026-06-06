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
public val iceWolfDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice wolf Drops",
    npcs = npcs("npc.ice_wolf_1", "npc.ice_wolf_2", "npc.ice_wolf_3", "npc.ice_wolf_4", "npc.ice_wolf_5", "npc.ice_wolf_6", "npc.trollromance_ice_wolf_1", "npc.trollromance_ice_wolf_1b", "npc.trollromance_ice_wolf_2"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_wolf_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
