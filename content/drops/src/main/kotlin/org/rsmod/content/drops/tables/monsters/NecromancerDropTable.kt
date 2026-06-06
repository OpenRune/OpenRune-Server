package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val necromancerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Necromancer Drops",
    npcs = npcs("npc.necromancer"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Necromancer Drops")
        8 weight "obj.plainstaff" count 1
        4 weight "obj.zamrobebottom" count 1
        3 weight "obj.zamrobetop" count 1
        1 weight "obj.staff_of_fire" count 1
        4 weight "obj.earthrune" count 36
        3 weight "obj.airrune" count 10
        3 weight "obj.waterrune" count 10
        3 weight "obj.earthrune" count 10
        3 weight "obj.firerune" count 10
        2 weight "obj.airrune" count 18
        2 weight "obj.waterrune" count 18
        2 weight "obj.earthrune" count 18
        2 weight "obj.firerune" count 18
        7 weight "obj.naturerune" count 5
        6 weight "obj.chaosrune" count 4
        3 weight "obj.mindrune" count 10
        3 weight "obj.bodyrune" count 10
        2 weight "obj.mindrune" count 18
        2 weight "obj.bodyrune" count 18
        2 weight "obj.bloodrune" count 2
        1 weight "obj.cosmicrune" count 2
        1 weight "obj.lawrune" count 3
        17 weight "obj.coins" count 1
        16 weight "obj.coins" count 2
        9 weight "obj.coins" count 4
        3 weight "obj.coins" count 29
        16 weight ringNothing()
    },
)
