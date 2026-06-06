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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val flamingPyrelordDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Flaming pyrelord Drops",
    npcs = npcs("npc.superior_pyrefiend"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Flaming pyrelord Drops")
        4 weight "obj.steel_axe" count 1
        4 weight "obj.steel_full_helm" count 1
        3 weight "obj.staff_of_fire" count 1
        2 weight "obj.mithril_chainbody" count 1
        1 weight "obj.adamant_med_helm" count 1
        1 weight "obj.steel_armoured_boots" count 1
        21 weight "obj.firerune" count 30
        8 weight "obj.firerune" count 60
        5 weight "obj.chaosrune" count 12
        3 weight "obj.deathrune" count 3
        7 weight "obj.coins" count 10
        24 weight "obj.coins" count 40
        20 weight "obj.coins" count 120
        10 weight "obj.coins" count 200
        2 weight "obj.coins" count 450
        8 weight "obj.gold_ore" count 1
        2 weight "obj.jug_wine" count 1

        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition { player ->
            // Drops Need Manual: Only dropped by those found in the Wilderness.
             true
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/12 after unlocking the medium Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
