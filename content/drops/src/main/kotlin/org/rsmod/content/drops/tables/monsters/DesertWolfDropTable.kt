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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val desertWolfDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Desert Wolf Drops",
    npcs = npcs("npc.desert_wolf", "npc.desert_wolf2", "npc.desert_wolf3"),
    mainTable = rsPlayerWeightedTable(total = 4) {
        name("Desert Wolf Drops")
        1 weight "obj.rag_wolf_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        3 weight nothing()
    },
)
