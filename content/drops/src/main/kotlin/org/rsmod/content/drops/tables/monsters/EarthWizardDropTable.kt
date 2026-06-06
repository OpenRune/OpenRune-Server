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
public val earthWizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Earth wizard Drops",
    npcs = npcs("npc.earth_wizard"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Earth wizard Drops")
        19 weight "obj.earthrune" count 5..10
        1 weight "obj.earth_talisman" count 1
    },
)
