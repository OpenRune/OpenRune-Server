package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val paladinDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Paladin Drops",
    npcs = npcs("npc.paladin", "npc.paladin_f", "npc.paladin_f_variant01", "npc.paladin_variant01", "npc.paladin_variant02", "npc.paladin_west_f_vis", "npc.paladin_west_vis"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Paladin Drops")
        2 weight "obj.steel_sword" count 1
        1 weight "obj.steel_longsword" count 1
        1 weight "obj.steel_full_helm" count 1
        13 weight "obj.waterrune" count 30
        1 weight "obj.bloodrune" count 1
        9 weight "obj.iron_bar" count 1
        1 weight "obj.mithril_bar" count 1
        1 weight "obj.steel_bar" count 1
        40 weight "obj.coins" count 48
        19 weight "obj.coins" count 15
        16 weight "obj.coins" count 2
        10 weight "obj.coins" count 8
        2 weight "obj.coins" count 120
        2 weight ringNothing()

        8 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
