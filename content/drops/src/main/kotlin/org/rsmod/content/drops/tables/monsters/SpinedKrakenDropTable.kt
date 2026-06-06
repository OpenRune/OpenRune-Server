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
public val spinedKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spined kraken Drops",
    npcs = npcs("npc.sailing_spined_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 2500 weight "obj.sailing_paint_inky" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 109) {
        name("Spined kraken Drops")
        10 weight "obj.waterrune" count 250..400
        10 weight "obj.deathrune" count 35..50
        8 weight "obj.bloodrune" count 20..30
        4 weight "obj.adamant_cannonball" count 24..36
        2 weight "obj.rune_cannonball" count 18..24
        5 weight "obj.water_battlestaff" count 1
        3 weight "obj.mystic_water_staff" count 1
        3 weight "obj.air_battlestaff" count 1
        2 weight "obj.battlestaff" count 1
        2 weight "obj.adamant_chainbody" count 1
        14 weight "obj.raw_shark" count 1
        8 weight "obj.coins" count 8000..12000
        8 weight "obj.plank_teak" count 2..4
        5 weight "obj.sailing_pirate_shipwreck_salvage" count 1
        1 weight "obj.boat_repair_kit_mahogany" count 1

        8 weight SharedDropTables.herb
        7 weight SharedDropTables.gem
        8 weight SharedDropTables.rareSeed
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_spined_kraken_ink_sac" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_spined_kraken_tentacle" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
