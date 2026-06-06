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
public val animatedSteelArmourTarnsLairDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Animated steel armour (Tarn's Lair) Drops",
    npcs = npcs("npc.lotr_steel_armour"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.iron_platebody" count 1
    },
)
