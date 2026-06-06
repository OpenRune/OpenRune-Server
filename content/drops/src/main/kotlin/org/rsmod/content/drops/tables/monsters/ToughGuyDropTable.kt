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
public val toughGuyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tough Guy Drops",
    npcs = npcs("npc.feud_menap_toughguy"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.blackjack_willow" count 1
    },
)
