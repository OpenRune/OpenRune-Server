package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val verzikViturDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Verzik Vitur Drops",
    npcs = npcs("npc.myq4_verzik_human", "npc.tobquest_verzik", "npc.verzik_death_bat", "npc.verzik_death_bat_hard", "npc.verzik_death_bat_story", "npc.verzik_initial", "npc.verzik_initial_hard", "npc.verzik_initial_story", "npc.verzik_phase1", "npc.verzik_phase1_hard", "npc.verzik_phase1_story", "npc.verzik_phase1_to2_transition", "npc.verzik_phase1_to2_transition_hard", "npc.verzik_phase1_to2_transition_story", "npc.verzik_phase2", "npc.verzik_phase2_hard", "npc.verzik_phase2_story", "npc.verzik_phase2_to3_transition", "npc.verzik_phase2_to3_transition_hard", "npc.verzik_phase2_to3_transition_story", "npc.verzik_phase3", "npc.verzik_phase3_hard", "npc.verzik_phase3_story"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.tob_book_verzik" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
    },
)
