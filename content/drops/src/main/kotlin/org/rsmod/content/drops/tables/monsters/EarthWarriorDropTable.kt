package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val earthWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Earth warrior Drops",
    npcs = npcs("npc.earthwarrior"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Earth warrior Drops")
        3 weight "obj.steel_spear" count 1
        2 weight "obj.staff_of_earth" count 1
        13 weight "obj.earthrune" count 12
        9 weight "obj.naturerune" count 3
        7 weight "obj.chaosrune" count 3
        6 weight "obj.lawrune" count 2
        4 weight "obj.deathrune" count 2
        3 weight "obj.earthrune" count 60
        1 weight "obj.bloodrune" count 2
        18 weight "obj.coins" count 12
        28 weight ringNothing()

        14 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        18 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 5000 weight "obj.champions_challenge_earthwarrior" count 1
    },
)
