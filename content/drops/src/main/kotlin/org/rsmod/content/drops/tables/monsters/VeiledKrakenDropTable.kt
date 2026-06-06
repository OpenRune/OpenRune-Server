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
public val veiledKrakenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Veiled kraken Drops",
    npcs = npcs("npc.sailing_veiled_kraken"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 90 weight "obj.dragon_sheet" count 1..2
        1 outOf 500 weight "obj.dragon_cannon_barrel" count 1
        1 outOf 1850 weight "obj.sailing_boat_keel_part_dragon" count 1
        1 outOf 1900 weight "obj.sailing_paint_inky" count 1
        1 outOf 2100 weight "obj.sailing_boat_large_keel_part_dragon" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 272) {
        name("Veiled kraken Drops")
        10 weight "obj.rune_cannonball" count 80..100
        10 weight "obj.earthrune" count 500..750
        10 weight "obj.waterrune" count 500..750
        8 weight "obj.deathrune" count 50..75
        5 weight "obj.bloodrune" count 30..40
        3 weight "obj.dragon_cannonball" count 25..35
        2 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx" count 10..20
        30 weight "obj.earth_battlestaff" count 1
        30 weight "obj.mystic_earth_staff" count 1
        30 weight "obj.water_battlestaff" count 1
        30 weight "obj.mystic_water_staff" count 1
        20 weight "obj.rune_full_helm" count 1
        20 weight "obj.rune_platelegs" count 1
        3 weight "obj.mystic_hat" count 1
        3 weight "obj.mystic_gloves" count 1
        3 weight "obj.mystic_boots" count 1
        10 weight "obj.bigoysterpearls" count 1
        5 weight "obj.boat_repair_kit_ironwood" count 1
        5 weight "obj.sailing_merchant_shipwreck_salvage" count 1
        5 weight "obj.giant_seaweed" count 1
        5 weight "obj.village_snake_hide" count 1
        5 weight "obj.raw_bluefin" count 1
        5 weight "obj.raw_halibut" count 1
        3 weight "obj.plank_camphor" count 1
        1 weight "obj.boat_repair_kit_rosewood" count 1

        9 weight SharedDropTables.herb
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_veiled_kraken_ink_sac" count 1 condition { player ->
            // Drops Need Manual: Tentacles and ink sacks are only dropped while on an applicable bounty task.
             true
        }
        1 outOf 5 weight "obj.sailing_veiled_kraken_tentacle" count 1
        1 outOf 133 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Camphor seed [main/1/{{#expr:1/(15/20 * 2/272) round 1}}]
//   - Ironwood seed [main/1/{{#expr:1/(4/20 * 2/272) round 1}}]
//   - Rosewood seed [main/1/{{#expr:1/(1/20 * 2/272) round 1}}]
