package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val screamingBansheeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Screaming banshee Drops",
    npcs = npcs("npc.superior_banshee"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Screaming banshee Drops")
        2 weight "obj.iron_mace" count 1
        2 weight "obj.iron_dagger" count 1
        1 weight "obj.iron_kiteshield" count 1
        3 weight "obj.airrune" count 3
        3 weight "obj.cosmicrune" count 2
        2 weight "obj.chaosrune" count 3
        1 weight "obj.firerune" count 7
        1 weight "obj.chaosrune" count 7
        10 weight "obj.coins" count 13
        8 weight "obj.coins" count 26
        8 weight "obj.coins" count 35
        22 weight "obj.cert_blankrune_high" count 13
        22 weight "obj.fishing_bait" count 15
        5 weight "obj.fishing_bait" count 7
        1 weight "obj.eye_of_newt" count 1
        1 weight "obj.iron_ore" count 1
        1 outOf 512 separate "obj.mystic_gloves_dark" count 1

        34 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/12 after unlocking the easy Combat Achievements rewards tier.
        10 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
