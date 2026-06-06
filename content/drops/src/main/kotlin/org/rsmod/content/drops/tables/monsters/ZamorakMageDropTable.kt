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
public val zamorakMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zamorak mage Drops",
    npcs = npcs("npc.rc_zmi_mage", "npc.rc_zmi_mage2"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Zamorak mage Drops")
        2 weight "obj.firerune" count 7
        2 weight "obj.waterrune" count 6
        2 weight "obj.airrune" count 5
        2 weight "obj.naturerune" count 2
        1 weight "obj.lawrune" count 2
        1 weight "obj.bloodrune" count 1
        40 weight "obj.coins" count 100
    },
)
