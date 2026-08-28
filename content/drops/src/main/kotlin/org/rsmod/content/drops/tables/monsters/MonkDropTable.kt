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
public val monkDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monk Drops",
    npcs = npcs("npc.entrana_monk", "npc.hosidius_monk", "npc.monk", "npc.monk_ardougne"),
)
