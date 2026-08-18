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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val armouredKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armoured kraken Drops",
    npcs = npcs("npc.sailing_armoured_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 1280 weight "obj.sailing_boat_large_keel_part_dragon" count 1
        1 outOf 1280 weight "obj.sailing_boat_keel_part_dragon" count 1
        1 outOf 90 weight "obj.dragon_sheet" count 1..2
        1 outOf 500 weight "obj.bottled_storm" count 1
        1 outOf 2000 weight "obj.sailing_paint_inky" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 119) {
        name("Armoured kraken Drops")
        10 weight "obj.deathrune" count 40..65
        10 weight "obj.waterrune" count 400..650
        8 weight "obj.bloodrune" count 25..35
        4 weight "obj.rune_cannonball" count 24..36
        6 weight "obj.battlestaff" count 1
        6 weight "obj.water_battlestaff" count 1
        6 weight "obj.earth_battlestaff" count 1
        5 weight "obj.mystic_water_staff" count 1
        5 weight "obj.mystic_earth_staff" count 1
        2 weight "obj.adamant_platebody" count 1
        1 weight "obj.mystic_robe_bottom" count 1
        12 weight "obj.raw_seaturtle" count 1
        10 weight "obj.coins" count 16000..18500
        8 weight "obj.plank_teak" count 2..4
        4 weight "obj.boat_repair_kit_camphor" count 1
        2 weight "obj.sailing_pirate_shipwreck_salvage" count 1

        8 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        11 weight nothing()
        10 outOf 727 separate "obj.toadflax_seed" count 1
        5 outOf 531 separate "obj.irit_seed" count 1
        5 outOf 549 separate "obj.belladonna_seed" count 1
        5 outOf 764 separate "obj.avantoe_seed" count 1
        10 outOf 1553 separate "obj.poisonivy_bush_seed" count 1
        10 outOf 1587 separate "obj.camphor_seed" count 1
        5 outOf 818 separate "obj.cactus_seed" count 1
        10 outOf 2249 separate "obj.potato_cactus_seed" count 1
        10 outOf 2273 separate "obj.kwuarm_seed" count 1
        5 outOf 1714 separate "obj.snapdragon_seed" count 1
        10 outOf 4543 separate "obj.limpwurt_seed" count 1
        10 outOf 4751 separate "obj.strawberry_seed" count 1
        10 outOf 4907 separate "obj.cadantine_seed" count 1
        10 outOf 4979 separate "obj.marrentill_seed" count 1
        1 outOf 595 separate "obj.ironwood_seed" count 1
        2 outOf 1353 separate "obj.jangerberry_bush_seed" count 1
        5 outOf 3409 separate "obj.lantadyme_seed" count 1
        5 outOf 3661 separate "obj.tarromin_seed" count 1
        5 outOf 3749 separate "obj.wildblood_hop_seed" count 1
        10 outOf 8107 separate "obj.snape_grass_seed" count 3
        10 outOf 9879 separate "obj.watermelon_seed" count 1
        1 outOf 1111 separate "obj.harralander_seed" count 1
        1 outOf 1117 separate "obj.dwarf_weed_seed" count 1
        1 outOf 1556 separate "obj.snape_grass_seed" count 1
        1 outOf 1596 separate "obj.ranarr_seed" count 1
        1 outOf 1751 separate "obj.torstol_seed" count 1
        1 outOf 1831 separate "obj.whiteberry_bush_seed" count 1
        1 outOf 2146 separate "obj.mushroom_seed" count 1
        1 outOf 2380 separate "obj.rosewood_seed" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 2 weight "obj.sailing_armoured_kraken_tentacle" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 10 weight "obj.sailing_armoured_kraken_ink_sac" count 1
        1 outOf 152 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
