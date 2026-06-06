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
public val rockLobsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rock lobster Drops",
    npcs = npcs("npc.dagannoth_rock_lobster_2x2", "npc.dagannoth_rock_lobster_2x2_loc"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Rock lobster Drops")
        10 weight "obj.earthrune" count 1..20
        10 weight "obj.waterrune" count 1..20
        10 weight "obj.steel_arrow" count 1..10
        10 weight "obj.iron_arrow" count 1..20
        10 weight "obj.blankrune_high" count 4
        10 weight "obj.seaweed" count 1..3
        10 weight "obj.soda_ash" count 1
        10 weight "obj.snape_grass" count 1
        10 weight "obj.edible_seaweed" count 1..3
        10 weight "obj.lobster_pot" count 1
        10 weight "obj.tinderbox" count 1

        3 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        14 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
