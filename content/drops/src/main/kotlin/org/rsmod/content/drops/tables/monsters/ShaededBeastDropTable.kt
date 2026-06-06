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
public val shaededBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Shaeded Beast Drops",
    npcs = npcs("npc.hosdun_shaeded_beast_hidden"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.hosdun_temple_coin" count 1
    },
)
