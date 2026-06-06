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
public val animatedBronzeArmourDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Animated Bronze Armour Drops",
    npcs = npcs("npc.warguild_bronze_armour"),
    mainTable = rsPlayerWeightedTable(total = 27) {
        name("Animated Bronze Armour Drops")
        9 weight "obj.bronze_full_helm" count 1
        9 weight "obj.bronze_platebody" count 1
        9 weight "obj.bronze_platelegs" count 1
    },
)
