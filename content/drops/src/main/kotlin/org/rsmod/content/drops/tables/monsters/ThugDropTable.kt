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
public val thugDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Thug Drops",
    npcs = npcs("npc.thug"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Thug Drops")
        4 weight "obj.iron_med_helm" count 1
        2 weight "obj.iron_battleaxe" count 1
        1 weight "obj.steel_axe" count 1
        13 weight "obj.naturerune" count 2
        4 weight "obj.chaosrune" count 2
        1 weight "obj.cosmicrune" count 2
        1 weight "obj.lawrune" count 2
        1 weight "obj.deathrune" count 2
        4 weight "obj.iron_ore" count 1
        3 weight "obj.iron_bar" count 1
        2 weight "obj.coal" count 1
        23 weight "obj.coins" count 8
        12 weight "obj.coins" count 15
        2 weight "obj.coins" count 30
        1 weight "obj.coins" count 20
        30 weight ringNothing()

        24 weight SharedDropTables.herb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 30 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
