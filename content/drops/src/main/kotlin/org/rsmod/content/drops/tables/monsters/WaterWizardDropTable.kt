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
public val waterWizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Water wizard Drops",
    npcs = npcs("npc.water_wizard"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Water wizard Drops")
        19 weight "obj.waterrune" count 5..10
        1 weight "obj.water_talisman" count 1
    },
)
