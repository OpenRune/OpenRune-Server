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
public val chaosDwarfDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos dwarf Drops",
    npcs = npcs("npc.dwarf_chaos"),
    mainTable = rsPlayerWeightedTable(total = 133) {
        name("Chaos dwarf Drops")
        2 weight "obj.steel_full_helm" count 1
        1 weight "obj.mithril_longsword" count 1
        1 weight "obj.mithril_sq_shield" count 1
        4 weight "obj.lawrune" count 3
        3 weight "obj.airrune" count 24
        3 weight "obj.chaosrune" count 10
        3 weight "obj.mindrune" count 37
        3 weight "obj.naturerune" count 9
        2 weight "obj.cosmicrune" count 3
        1 weight "obj.deathrune" count 3
        1 weight "obj.waterrune" count 10
        40 weight "obj.coins" count 92
        18 weight "obj.coins" count 47
        11 weight "obj.coins" count 25
        10 weight "obj.coins" count 150
        2 weight "obj.coins" count 350
        2 weight "obj.coins" count 15
        1 weight "obj.coal" count 1
        7 weight "obj.muddy_key" count 1
        5 weight ringNothing()
        1 weight "obj.cheese" count 1
        6 weight "obj.mithril_bar" count 1
        1 weight "obj.tomato" count 1

        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
