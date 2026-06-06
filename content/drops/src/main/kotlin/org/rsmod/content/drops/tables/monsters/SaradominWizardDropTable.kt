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
public val saradominWizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Saradomin wizard Drops",
    npcs = npcs("npc.trail_hard2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Saradomin wizard Drops")
        2 weight "obj.air_battlestaff" count 1
        3 weight "obj.water_battlestaff" count 1
        3 weight "obj.earth_battlestaff" count 1
        1 weight "obj.fire_battlestaff" count 1
        3 weight "obj.airrune" count 60
        2 weight "obj.airrune" count 189
        3 weight "obj.waterrune" count 42
        2 weight "obj.waterrune" count 126
        3 weight "obj.earthrune" count 79
        4 weight "obj.earthrune" count 86
        2 weight "obj.earthrune" count 107
        3 weight "obj.firerune" count 51
        2 weight "obj.firerune" count 180
        1 weight "obj.cosmicrune" count 25
        3 weight "obj.naturerune" count 28
        7 weight "obj.naturerune" count 30
        1 weight "obj.lawrune" count 9
        4 weight "obj.lawrune" count 10
        6 weight "obj.deathrune" count 45
        12 weight "obj.bloodrune" count 8
        17 weight "obj.coins" count 77
        25 weight "obj.coins" count 89
        3 weight "obj.coins" count 240
        16 weight nothing()
    },
)
