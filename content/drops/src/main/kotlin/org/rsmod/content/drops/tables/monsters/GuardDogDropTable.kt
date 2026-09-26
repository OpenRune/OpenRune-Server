package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val guardDogDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard dog Drops",
    npcs = npcs("npc.guarddog", "npc.hosidius_guarddog"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 25 weight "obj.arceuus_corpse_dog" count 1
    },
)
