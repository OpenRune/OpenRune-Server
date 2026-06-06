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
public val entranaFirebirdDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Entrana firebird Drops",
    npcs = npcs("npc.fire_bird"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.hot_feather" count 1
    },
)
