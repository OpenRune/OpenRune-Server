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
public val cruorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cruor Drops",
    npcs = npcs("npc.nex_bloodmage"),
    mainTable = rsPlayerWeightedTable(total = 1000) {
        name("Cruor Drops")
        1 weight "obj.ancient_ceremonial_mask" count 1
        1 weight "obj.ancient_ceremonial_top" count 1
        1 weight "obj.ancient_ceremonial_legs" count 1
        1 weight "obj.ancient_ceremonial_gloves" count 1
        1 weight "obj.ancient_ceremonial_boots" count 1
        995 weight nothing()
    },
)
