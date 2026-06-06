package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val iceSpiderDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice spider Drops",
    npcs = npcs("npc.ice_spider", "npc.ice_spider_shortrange", "npc.tapo_ice_spider_shortrange"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 964 weight "obj.varlamore_key_half_1" count 1
        1 outOf 128 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Frozen tear [tertiary/<ref name=row group=d>Only dropped in Ruins of Tapoyauik.</ref>]
