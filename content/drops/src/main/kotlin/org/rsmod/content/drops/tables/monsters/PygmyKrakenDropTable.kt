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
public val pygmyKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pygmy kraken Drops",
    npcs = npcs("npc.sailing_pygmy_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 3000 weight "obj.sailing_paint_inky" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 108) {
        name("Pygmy kraken Drops")
        10 weight "obj.waterrune" count 100..150
        10 weight "obj.deathrune" count 20..25
        6 weight "obj.bloodrune" count 10..15
        2 weight "obj.adamant_cannonball" count 18..24
        3 weight "obj.water_battlestaff" count 1
        3 weight "obj.battlestaff" count 1
        2 weight "obj.mithril_chainbody" count 1
        14 weight "obj.raw_swordfish" count 1
        14 weight "obj.raw_bass" count 1
        8 weight "obj.coins" count 3000..5000
        8 weight "obj.plank_teak" count 2..4
        6 weight "obj.swamp_tar" count 10..25
        5 weight "obj.boat_repair_kit_teak" count 1
        1 weight "obj.sailing_large_shipwreck_salvage" count 1
        6 outOf 540 separate "obj.coral_elkhorn_frag" count 1
        3 outOf 540 separate "obj.coral_pillar_frag" count 1
        1 outOf 540 separate "obj.coral_umbral_frag" count 1

        8 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        7 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_pygmy_kraken_tentacle" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_pygmy_kraken_ink_sac" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (medium) [tertiary/Rare]
