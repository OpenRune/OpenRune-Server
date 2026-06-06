package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val torturedSoulDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tortured soul Drops",
    npcs = npcs("npc.ahoy_tortured_soul"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Tortured soul Drops")
        7 weight "obj.bodyrune" count 7
        6 weight "obj.waterrune" count 6
        3 weight "obj.lawrune" count 2
        5 weight "obj.unidentified_harralander" count 1
        3 weight "obj.unidentified_tarromin" count 1
        2 weight "obj.unidentified_marentill" count 1
        1 weight "obj.unidentified_guam" count 1
        1 weight "obj.unidentified_ranarr" count 1
        1 weight "obj.coins" count 12
        2 weight "obj.coins" count 2
        3 weight "obj.coins" count 6
        3 weight "obj.coins" count 5
        30 weight "obj.coins" count 3
        21 weight "obj.coins" count 2
        16 weight ringNothing()
        3 weight "obj.limpwurt_root" count 1
        1 weight "obj.vial_water" count 1
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        20 weight nothing()
    },
)
