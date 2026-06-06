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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val blackHeatherDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Black Heather Drops",
    npcs = npcs("npc.black_heather"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Black Heather Drops")
        1 weight "obj.steel_longsword" count 1
        1 weight "obj.steel_full_helm" count 1
        4 weight "obj.lawrune" count 2
        4 weight "obj.naturerune" count 4
        3 weight "obj.bodyrune" count 12
        3 weight "obj.chaosrune" count 3
        3 weight "obj.waterrune" count 30
        1 weight "obj.mindrune" count 5
        30 weight "obj.coins" count 48
        18 weight "obj.coins" count 15
        11 weight "obj.coins" count 8
        10 weight "obj.coins" count 70
        5 weight "obj.coins" count 5
        2 weight "obj.coins" count 150
        11 weight "obj.silver_ore" count 1
        2 weight ringNothing()
        2 weight "obj.cert_swordfish" count 5

        15 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
