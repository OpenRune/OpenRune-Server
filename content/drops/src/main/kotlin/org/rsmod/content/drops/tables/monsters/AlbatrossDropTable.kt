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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val albatrossDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Albatross Drops",
    npcs = npcs("npc.sailing_albatross"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 35 weight "obj.swift_albatross_feather" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Albatross Drops")
        40 weight "obj.flax_seed" count 1
        24 weight "obj.hemp_seed" count 1
        16 weight "obj.cotton_seed" count 1
        24 weight "obj.coral_elkhorn_frag" count 1
        12 weight "obj.coral_pillar_frag" count 1
        4 weight "obj.coral_umbral_frag" count 1
        65 outOf 1000 separate "obj.bird_nest_seeds_jan2019" count 1
        32 outOf 1000 separate "obj.bird_nest_ring" count 1
        1 outOf 1000 separate rsPlayerWeightedTable {
            1 weight "obj.bird_nest_egg_red" count 1
            1 weight "obj.bird_nest_egg_green" count 1
            1 weight "obj.bird_nest_egg_blue" count 1
        }
        14 outOf 50 separate "obj.feather" count 500..1000
        5 outOf 50 separate rsPlayerWeightedTable {
            5 weight "obj.raw_swordfish" count 1
            5 weight "obj.diamond" count 1
            5 weight "obj.diamond_ring" count 1
        }
        2 outOf 50 separate rsPlayerWeightedTable {
            2 weight "obj.raw_shark" count 1
            2 weight "obj.yew_roots" count 1
        }
        380 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_albatross_feather" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_albatross_beak" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/114 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 120 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
