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
public val khazardWarlordDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Khazard warlord Drops",
    npcs = npcs("npc.khazard_warlord_chat", "npc.khazard_warlord_combat"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.orbs_of_protection" count 1 condition {
            player -> player.isOnQuest("quest_treegnomevillage")
        }
    },
)
