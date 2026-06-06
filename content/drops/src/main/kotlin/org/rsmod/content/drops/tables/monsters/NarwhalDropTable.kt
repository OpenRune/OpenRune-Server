package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val narwhalDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Narwhal Drops",
    npcs = npcs("npc.sailing_narwhal"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 20 weight "obj.narwhal_horn" count 1
        1 outOf 75 weight "obj.nails_dragon" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 60) {
        name("Narwhal Drops")
        1 weight "obj.rune_mace" count 1
        1 weight "obj.rune_scimitar" count 1
        1 weight "obj.rune_sword" count 1
        15 weight "obj.waterrune" count 300..800
        4 weight "obj.adamant_arrow" count 1
        4 weight "obj.rune_arrow" count 1
        4 weight "obj.adamant_cannonball" count 36..54
        2 weight "obj.rune_cannonball" count 30..42
        8 weight "obj.swamppaste" count 40..60
        6 weight "obj.cert_giant_seaweed" count 1
        1 weight "obj.casket" count 1
        1 weight "obj.old_boot" count 1
        5 outOf 75 separate "obj.flax_seed" count 2..3
        3 outOf 75 separate "obj.hemp_seed" count 2..3
        2 outOf 75 separate "obj.cotton_seed" count 2..3
        6 outOf 200 separate "obj.coral_elkhorn_frag" count 1..2
        3 outOf 200 separate "obj.coral_pillar_frag" count 1..2
        1 outOf 200 separate "obj.coral_umbral_frag" count 1..2

        1 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_narwhal_blubber" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_narwhal_tusk" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/142 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 150 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
