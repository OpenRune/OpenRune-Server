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
public val sotetsegDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sotetseg Drops",
    npcs = npcs("npc.tob_sotetseg_combat", "npc.tob_sotetseg_combat_hard", "npc.tob_sotetseg_combat_story", "npc.tob_sotetseg_noncombat", "npc.tob_sotetseg_noncombat_hard", "npc.tob_sotetseg_noncombat_story"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.tob_book_sotetseg" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 400) {
        name("Sotetseg Drops")
        1 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 separate "obj.dorgesh_construction_bone_curved" count 1
        399 weight nothing()
    },
)
