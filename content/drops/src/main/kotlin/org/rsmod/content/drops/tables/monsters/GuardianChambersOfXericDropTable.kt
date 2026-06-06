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
public val guardianChambersOfXericDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guardian (Chambers of Xeric) Drops",
    npcs = npcs("npc.raids_stoneguardians_left", "npc.raids_stoneguardians_left_dead", "npc.raids_stoneguardians_right", "npc.raids_stoneguardians_right_dead"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raids_seed_buchuleaf" count 3..5
        "obj.raids_seed_golpar" count 3..5
        "obj.raids_seed_noxifer" count 3..5
    },
)
