package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val minotaurDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Minotaur Drops",
    npcs = npcs("npc.sos_war_minotaur", "npc.sos_war_minotaur2", "npc.sos_war_minotaur3"),
    mainTable = rsPlayerWeightedTable(total = 101) {
        name("Minotaur Drops")
        10 weight "obj.iron_arrow" count 5..14
        10 weight "obj.bronze_spear" count 1
        10 weight "obj.bronze_full_helm" count 1
        3 weight "obj.bronze_arrow" count 3
        4 weight "obj.bronze_dagger" count 1
        1 weight "obj.mindrune" count 1
        11 weight "obj.coins" count 2
        20 weight "obj.coins" count 7
        7 weight "obj.coins" count 5..84
        6 weight "obj.cert_copper_ore" count 1
        6 weight "obj.cert_tin_ore" count 1
        5 weight "obj.cert_blankrune_high" count 15
        3 weight "obj.cooked_meat" count 1
        3 weight "obj.sos_half_skull1" count 1

        1 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 50 weight "obj.arceuus_corpse_minotaur" count 1
        1 outOf 60 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
