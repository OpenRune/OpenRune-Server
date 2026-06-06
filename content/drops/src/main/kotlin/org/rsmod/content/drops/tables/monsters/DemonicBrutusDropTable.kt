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
public val demonicBrutusDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Demonic Brutus Drops",
    npcs = npcs("npc.cowboss_hardmode", "npc.cowboss_hardmode_ghost"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.cow_slippers_recol_4" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.cowbosspet" count 1
    },
)
