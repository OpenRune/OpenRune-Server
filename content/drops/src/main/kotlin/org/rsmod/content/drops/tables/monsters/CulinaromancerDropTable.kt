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
public val culinaromancerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Culinaromancer Drops",
    npcs = npcs("npc.culinaromancer_froze", "npc.hundred_culinaromancer_base", "npc.hundred_culinaromancer_book1", "npc.hundred_culinaromancer_book2", "npc.hundred_culinaromancer_book3", "npc.hundred_culinaromancer_book4", "npc.hundred_culinaromancer_book5", "npc.hundred_culinaromancer_dead", "npc.hundred_culinaromancer_end", "npc.hundred_culinaromancer_final"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.cake" count 1
    },
)
