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
public val iceDemonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice demon Drops",
    npcs = npcs("npc.raids_icedemon_combat", "npc.raids_icedemon_noncombat"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raids_stinkhorn_mushroom" count 7
        "obj.raids_endarkened_juice" count 7
        "obj.raids_cicely" count 5
    },
)
