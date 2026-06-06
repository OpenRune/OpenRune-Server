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
public val ghostDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ghost Drops",
    npcs = npcs("npc.ghost", "npc.ghost2", "npc.ghost3", "npc.ghost3_unaggressive", "npc.ghost4", "npc.ghost5", "npc.ghost5_unaggressive", "npc.ghost6", "npc.ghost7", "npc.ghost7_unaggressive", "npc.ghost8", "npc.ghost8_unaggressive", "npc.ghost_unaggressive", "npc.house_ghost", "npc.house_ghost2", "npc.house_ghost3", "npc.house_ghost4", "npc.house_ghost5", "npc.kourend_ghost1", "npc.kourend_ghost2", "npc.sos_death_ghost", "npc.sos_death_ghost2", "npc.sos_death_ghost2_b", "npc.sos_death_ghost2_c", "npc.sos_death_ghost2_d", "npc.sos_death_ghost_b", "npc.sos_death_ghost_c", "npc.sos_death_ghost_d"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 7 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 90 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
