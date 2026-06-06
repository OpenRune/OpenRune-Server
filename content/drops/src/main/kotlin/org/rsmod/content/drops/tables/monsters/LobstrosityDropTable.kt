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
public val lobstrosityDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lobstrosity Drops",
    npcs = npcs("npc.fossil_lobster"),
    mainTable = rsPlayerWeightedTable(total = 118) {
        name("Lobstrosity Drops")
        8 weight "obj.waterrune" count 100
        8 weight "obj.chaosrune" count 20
        5 weight "obj.deathrune" count 10
        6 weight "obj.naturerune" count 10
        9 weight "obj.coins" count 1000
        8 weight "obj.fossil_puffer_fish" count 1
        8 weight "obj.cert_giant_seaweed" count 6
        8 weight "obj.cert_seaweed" count 6
        6 weight "obj.cert_toads_legs" count 2
        5 weight "obj.cert_brut_caviar" count 2
        4 weight "obj.bigoysterpearls" count 1
        15 outOf 944 separate "obj.unidentified_kwuarm" count 1
        9 outOf 944 separate "obj.unidentified_lantadyme" count 1
        3 outOf 236 separate "obj.unidentified_cadantine" count 1
        3 outOf 326 separate "obj.unidentified_dwarf_weed" count 1

        14 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        5 weight SharedDropTables.rareSeed
        21 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
