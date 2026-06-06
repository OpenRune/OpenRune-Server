package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val frigatebirdDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Frigatebird Drops",
    npcs = npcs("npc.sailing_frigatebird"),
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Frigatebird Drops")
        30 weight "obj.flax_seed" count 1
        18 weight "obj.hemp_seed" count 1
        12 weight "obj.cotton_seed" count 1
        18 weight "obj.coral_elkhorn_frag" count 1
        9 weight "obj.coral_pillar_frag" count 1
        3 weight "obj.coral_umbral_frag" count 1
        18 outOf 50 separate "obj.feather" count 300..500
        5 outOf 50 separate rsPlayerWeightedTable {
            5 weight "obj.raw_swordfish" count 1
            5 weight "obj.ruby" count 1
            5 weight "obj.ruby_ring" count 1
        }
        2 outOf 50 separate rsPlayerWeightedTable {
            2 weight "obj.raw_lobster" count 1
            2 weight "obj.maple_roots" count 1
        }
        410 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.sailing_frigatebird_feather" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_frigatebird_beak" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/161 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 170 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
