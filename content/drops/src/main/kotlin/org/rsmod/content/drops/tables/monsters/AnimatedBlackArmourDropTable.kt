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
public val animatedBlackArmourDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Animated Black Armour Drops",
    npcs = npcs("npc.warguild_black_armour"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.black_full_helm" count 1
        "obj.black_platebody" count 1
        "obj.black_platelegs" count 1
    },
)
