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
public val grizzlyBearCubDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Grizzly bear cub Drops",
    npcs = npcs("npc.regicide_darkbear_cub1", "npc.regicide_darkbear_cub2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.fur" count 1
        "obj.raw_bear_meat" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.rag_bear_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 25 weight "obj.arceuus_corpse_bear" count 1
        1 outOf 90 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
