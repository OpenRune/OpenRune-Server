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
public val vampyreKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vampyre kraken Drops",
    npcs = npcs("npc.sailing_vampyre_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 1024 weight "obj.sailing_boat_keel_part_dragon" count 1
        1 outOf 1024 weight "obj.sailing_boat_large_keel_part_dragon" count 1
        1 outOf 350 weight "obj.bottled_storm" count 1
        1 outOf 80 weight "obj.dragon_sheet" count 1..2
        1 outOf 1500 weight "obj.sailing_paint_inky" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 120) {
        name("Vampyre kraken Drops")
        10 weight "obj.deathrune" count 50..75
        10 weight "obj.waterrune" count 500..750
        8 weight "obj.bloodrune" count 30..40
        4 weight "obj.rune_cannonball" count 48..60
        2 weight "obj.dragon_cannonball" count 30..42
        6 weight "obj.battlestaff" count 1
        6 weight "obj.water_battlestaff" count 1
        6 weight "obj.fire_battlestaff" count 1
        5 weight "obj.mystic_water_staff" count 1
        5 weight "obj.mystic_fire_staff" count 1
        3 weight "obj.rune_chainbody" count 1
        1 weight "obj.mystic_robe_top" count 1
        12 weight "obj.raw_seaturtle" count 1
        10 weight "obj.coins" count 17500..20000
        8 weight "obj.plank_mahogany" count 3..5
        4 weight "obj.boat_repair_kit_ironwood" count 1
        2 weight "obj.sailing_fremennik_shipwreck_salvage" count 1

        8 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        8 weight SharedDropTables.rareSeed
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_vampyre_kraken_ink_sac" count 1 condition { player ->
            // Drops Need Manual: Tentacles and ink sacks are only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_vampyre_kraken_tentacle" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/142 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 150 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
