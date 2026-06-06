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
public val forgottenSoulSoulWarsDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Forgotten Soul (Soul Wars) Drops",
    npcs = npcs("npc.soul_wars_soul_strong_1", "npc.soul_wars_soul_strong_2", "npc.soul_wars_soul_weak_1", "npc.soul_wars_soul_weak_2", "npc.soul_wars_tut_soul_strong_1", "npc.soul_wars_tut_soul_strong_2", "npc.soul_wars_tut_soul_weak_1", "npc.soul_wars_tut_soul_weak_2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.soul_wars_tut_soul_fragment" count 2
    },
)
