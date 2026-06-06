package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val hermitCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Hermit crab Drops",
    npcs = npcs("npc.great_conch_hermit_crab", "npc.great_conch_hermit_crab_inactive_east", "npc.great_conch_hermit_crab_inactive_north", "npc.great_conch_hermit_crab_inactive_south", "npc.great_conch_hermit_crab_inactive_west"),
    mainTable = rsPlayerWeightedTable(total = 1280) {
        name("Hermit crab Drops")
        6 weight "obj.coral_elkhorn_frag" count 1
        3 weight "obj.coral_pillar_frag" count 1
        1 weight "obj.coral_umbral_frag" count 1

        1 weight SharedDropTables.gem
        1269 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.sailing_paint_sandy" count 1 condition { player ->
            // Drops Need Manual: Sandy paint is only dropped while performing the Crab dance emote
             true
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (easy) [tertiary/Unknown]
