package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val dolphinDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dolphin Drops",
    npcs = npcs("npc.sailing_dolphin"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 1200 weight "obj.echo_pearl" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Dolphin Drops")
        15 weight "obj.waterrune" count 50..80
        8 weight "obj.iron_cannonball" count 30..42
        6 weight "obj.bronze_cannonball" count 36..54
        10 weight "obj.cert_seaweed" count 3..5
        4 weight "obj.flax_seed" count 3..5
        4 weight "obj.swamppaste" count 40..60
        1 weight "obj.coral_elkhorn_frag" count 1
        1 weight "obj.casket" count 1
        1 weight "obj.ball_gnomeball_game" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/52 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 55 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
