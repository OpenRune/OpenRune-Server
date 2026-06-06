package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ospreyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Osprey Drops",
    npcs = npcs("npc.sailing_osprey"),
    mainTable = rsPlayerWeightedTable(total = 500) {
        name("Osprey Drops")
        20 weight "obj.flax_seed" count 1
        12 weight "obj.hemp_seed" count 1
        8 weight "obj.cotton_seed" count 1
        12 weight "obj.coral_elkhorn_frag" count 1
        6 weight "obj.coral_pillar_frag" count 1
        2 weight "obj.coral_umbral_frag" count 1
        65 outOf 2500 separate "obj.bird_nest_seeds_jan2019" count 1
        32 outOf 2500 separate "obj.bird_nest_ring" count 1
        1 outOf 2500 separate rsPlayerWeightedTable {
            1 weight "obj.bird_nest_egg_red" count 1
            1 weight "obj.bird_nest_egg_green" count 1
            1 weight "obj.bird_nest_egg_blue" count 1
        }
        25 outOf 50 separate "obj.feather" count 200..300
        2 outOf 50 separate rsPlayerWeightedTable {
            2 weight "obj.raw_lobster" count 1
            2 weight "obj.willow_roots" count 1
        }
        5 outOf 50 separate "obj.raw_tuna" count 1
        4 outOf 50 separate rsPlayerWeightedTable {
            4 weight "obj.emerald" count 1
            4 weight "obj.emerald_ring" count 1
        }
        440 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.sailing_osprey_feather" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_osprey_beak" count 1
        1 outOf 190 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
    },
)
