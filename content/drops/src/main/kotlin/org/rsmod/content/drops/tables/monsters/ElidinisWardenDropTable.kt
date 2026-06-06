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
public val elidinisWardenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elidinis' Warden Drops",
    npcs = npcs("npc.toa_warden_elidinis_phase1", "npc.toa_warden_elidinis_phase1_inactive", "npc.toa_warden_elidinis_phase2_exposed", "npc.toa_warden_elidinis_phase2_mage", "npc.toa_warden_elidinis_phase2_range", "npc.toa_warden_elidinis_phase3", "npc.toa_warden_elidinis_phase3_charging", "npc.toa_warden_elidinis_phase3_inactive"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.toa_book_wardens" count 1 condition { player ->
            // Drops Need Manual: Only received if players have not read it or have it stored.
             true
        }
    },
)
