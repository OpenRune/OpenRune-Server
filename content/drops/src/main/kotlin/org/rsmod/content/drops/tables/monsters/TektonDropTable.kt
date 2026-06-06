package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tektonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tekton Drops",
    npcs = npcs("npc.raids_tekton_fighting_enraged", "npc.raids_tekton_fighting_standard", "npc.raids_tekton_hammering", "npc.raids_tekton_waiting", "npc.raids_tekton_walking_enraged", "npc.raids_tekton_walking_standard"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raids_tekton_book" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
        "obj.raids_stinkhorn_mushroom" count 5
        "obj.raids_vial_overload_strong_4" count 2
        "obj.raids_vial_prayer_strong_4" count 1
        "obj.raids_vial_revitalisation_strong_4" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 450) {
        name("Tekton Drops")
        1 weight "obj.onyx" count 1
        449 weight nothing()
    },
)
