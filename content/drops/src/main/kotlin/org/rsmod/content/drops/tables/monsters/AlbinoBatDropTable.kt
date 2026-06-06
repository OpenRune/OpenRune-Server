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
public val albinoBatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Albino bat Drops",
    npcs = npcs("npc.harmless_island_albino_bat"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.rag_giant_bat_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
    },
)
