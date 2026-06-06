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
public val animatedSteelArmourDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Animated Steel Armour Drops",
    npcs = npcs("npc.warguild_steel_armour"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.steel_platebody" count 1
        "obj.steel_platelegs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 10) {
        name("Animated Steel Armour Drops")
        9 weight "obj.steel_full_helm" count 1
        1 weight nothing()
    },
)
