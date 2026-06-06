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
public val zamorakCrafterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zamorak crafter Drops",
    npcs = npcs("npc.rc_zmi_runerunner", "npc.rc_zmi_runerunner2"),
    mainTable = rsPlayerWeightedTable(total = 70) {
        name("Zamorak crafter Drops")
        10 weight "obj.airrune" count 5
        5 weight "obj.bodyrune" count 3
        2 weight "obj.bloodrune" count 4
        5 weight "obj.chaosrune" count 4
        2 weight "obj.deathrune" count 3
        5 weight "obj.earthrune" count 6
        10 weight "obj.waterrune" count 5
        2 weight "obj.soulrune" count 3
        10 weight "obj.firerune" count 2
        1 weight "obj.lawrune" count 3
        2 weight "obj.naturerune" count 2
        5 weight "obj.blankrune_high" count 3
        1 weight "obj.blankrune_high" count 10
        10 weight "obj.coins" count 50
    },
)
