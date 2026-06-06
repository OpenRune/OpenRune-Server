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
public val animatedMithrilArmourDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Animated Mithril Armour Drops",
    npcs = npcs("npc.warguild_mithril_armour"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mithril_full_helm" count 1
        "obj.mithril_platebody" count 1
        "obj.mithril_platelegs" count 1
    },
)
