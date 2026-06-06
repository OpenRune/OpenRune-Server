package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val shadeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Shade Drops",
    npcs = npcs("npc.kourend_shade", "npc.macro_shade"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Shade Drops")
        32 weight "obj.blackrobetop" count 1
        32 weight "obj.blackrobebottom" count 1
        64 weight ringNothing()
    },
)
