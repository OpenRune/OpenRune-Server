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
public val reanimatedGiantDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Reanimated giant Drops",
    npcs = npcs("npc.arceuus_reanimated_giant"),
    mainTable = rsPlayerWeightedTable(total = 5000) {
        name("Reanimated giant Drops")
        1 weight "obj.champions_challenge_giant" count 1
        4999 weight nothing()
    },
)
