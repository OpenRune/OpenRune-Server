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
public val orcaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Orca Drops",
    npcs = npcs("npc.sailing_orca"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 70 weight "obj.echo_pearl" count 1
        1 outOf 100 weight "obj.dragon_sheet" count 1..2
        1 outOf 75 weight "obj.nails_dragon" count 5..10
    },
    mainTable = rsPlayerWeightedTable(total = 92) {
        name("Orca Drops")
        10 weight "obj.rune_arrow" count 50..75
        5 weight "obj.rune_cannonball" count 24..42
        3 weight "obj.bloodrune" count 45..70
        2 weight "obj.dragon_cannonball" count 18..24
        8 weight "obj.rune_longsword" count 1
        6 weight "obj.rune_platelegs" count 1
        6 weight "obj.rune_warhammer" count 1
        3 weight "obj.coral_pillar_frag" count 1
        2 weight "obj.coral_umbral_frag" count 1
        12 weight "obj.coins" count 15000..20000
        10 weight "obj.raw_seaturtle" count 1
        8 weight "obj.bigoysterpearls" count 1
        6 weight "obj.boat_repair_kit_camphor" count 1..2
        6 weight "obj.sailing_fremennik_shipwreck_salvage" count 1
        5 outOf 460 separate "obj.flax_seed" count 1
        3 outOf 460 separate "obj.hemp_seed" count 1
        2 outOf 460 separate "obj.cotton_seed" count 1
        15 outOf 1840 separate "obj.camphor_seed" count 1
        4 outOf 1840 separate "obj.ironwood_seed" count 1
        1 outOf 1840 separate "obj.rosewood_seed" count 1

        2 weight SharedDropTables.gem
        3 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_orca_blubber" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_orca_teeth" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/123 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 130 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
