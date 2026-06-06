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
public val snakeDesertDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Snake (desert) Drops",
    npcs = npcs("npc.feud_desert_snake", "npc.feud_desert_snake_outside"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_snake_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
