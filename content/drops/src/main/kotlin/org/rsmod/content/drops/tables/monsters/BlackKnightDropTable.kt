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
public val blackKnightDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Black Knight Drops",
    npcs = npcs("npc.aggressive_black_knight", "npc.aggressive_black_knight_f", "npc.black_knight", "npc.black_knight_f", "npc.kr_aggressive_black_knight"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Black Knight Drops")
        4 weight "obj.iron_sword" count 1
        2 weight "obj.iron_full_helm" count 1
        1 weight "obj.steel_mace" count 1
        4 weight "obj.mithril_arrow" count 3
        3 weight "obj.bodyrune" count 9
        3 weight "obj.chaosrune" count 6
        3 weight "obj.earthrune" count 10
        2 weight "obj.deathrune" count 2
        2 weight "obj.lawrune" count 3
        1 weight "obj.cosmicrune" count 7
        1 weight "obj.mindrune" count 2
        6 weight "obj.steel_bar" count 1
        1 weight "obj.tin_ore" count 1
        1 weight "obj.pot_flour" count 1
        21 weight "obj.coins" count 35
        11 weight "obj.coins" count 6
        10 weight "obj.coins" count 58
        9 weight "obj.coins" count 12
        2 weight "obj.coins" count 80
        14 weight "obj.coins" count 1
        2 weight ringNothing()
        1 weight "obj.bread" count 1

        3 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        18 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 9 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)
