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
public val unicornDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Unicorn Drops",
    npcs = npcs("npc.unicorn", "npc.unicorn_lowwander"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.unicorn_horn" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.rag_unicorn_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 35 weight "obj.arceuus_corpse_unicorn" count 1
    },
)
