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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val greatWhiteSharkDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Great white shark Drops",
    npcs = npcs("npc.sailing_great_white_shark"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 6138 weight "obj.sailing_boat_large_keel_part_dragon" count 1
        1 outOf 6138 weight "obj.sailing_boat_keel_part_dragon" count 1
        1 outOf 500 weight "obj.broken_dragon_hook" count 1
        1 outOf 120 weight "obj.dragon_sheet" count 1..2
    },
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Great white shark Drops")
        4 weight "obj.rune_cannonball" count 24..36
        2 weight "obj.dragon_cannonball" count 12..18
        2 weight "obj.sailing_fremennik_shipwreck_salvage" count 1
        8 weight "obj.adamant_platebody" count 1
        6 weight "obj.battlestaff" count 1
        5 weight "obj.rune_sword" count 1
        5 weight "obj.mystic_hat" count 1
        3 weight "obj.black_dhide_shield" count 1
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.dragon_longsword" count 1
        17 weight "obj.raw_shark" count 1
        12 weight "obj.strung_diamond_amulet" count 1
        8 weight "obj.coins" count 15000..20000
        8 weight "obj.rune_arrow" count 50..65
        8 weight "obj.xbows_crossbow_bolts_runite" count 30..50
        4 weight "obj.xbows_bolt_tips_onyx" count 4..6
        3 weight "obj.dragon_dart_tip" count 15..25
        2 weight "obj.crystal_key" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_great_white_shark_jaw" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_great_white_shark_liver" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/190 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 200 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
