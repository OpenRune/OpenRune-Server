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
public val ternDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tern Drops",
    npcs = npcs("npc.sailing_tern"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Tern Drops")
        25 weight "obj.feather" count 100..200
        6 weight "obj.raw_salmon" count 1
        4 weight "obj.raw_tuna" count 1
        4 weight "obj.sapphire" count 1
        4 weight "obj.sapphire_ring" count 1
        2 weight "obj.oak_roots" count 1
        15 outOf 500 separate "obj.flax_seed" count 1
        9 outOf 500 separate "obj.hemp_seed" count 1
        6 outOf 500 separate rsPlayerWeightedTable {
            6 weight "obj.cotton_seed" count 1
            6 weight "obj.coral_elkhorn_frag" count 1
        }
        3 outOf 500 separate "obj.coral_pillar_frag" count 1
        1 outOf 500 separate "obj.coral_umbral_frag" count 1
        65 outOf 5000 separate "obj.bird_nest_seeds_jan2019" count 1
        32 outOf 5000 separate "obj.bird_nest_ring" count 1
        1 outOf 5000 separate rsPlayerWeightedTable {
            1 weight "obj.bird_nest_egg_red" count 1
            1 weight "obj.bird_nest_egg_green" count 1
            1 weight "obj.bird_nest_egg_blue" count 1
        }
        5 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.sailing_tern_feather" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_tern_beak" count 1
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/118 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 125 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
