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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyDrakeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Drake Drops",
    npcs = npcs("npc.league_superior_drake"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 512 weight "obj.drake_tooth" count 1
        1 outOf 512 weight "obj.drake_claw" count 1
        1 outOf 2000 rolls rsPlayerWeightedTable {
            1 weight "obj.dragon_thrownaxe" count 100..200
            1 weight "obj.dragon_knife" count 100..200
        }
    },
    mainTable = rsPlayerWeightedTable(total = 442) {
        name("Bloodthirsty Drake Drops")
        5 weight "obj.cert_raw_lobster" count 60..80
        10 weight "obj.cert_snape_grass" count 60..150
        10 weight "obj.irit_seed" count 10..15
        5 weight "obj.cert_unicorn_horn" count 60..150
        10 weight "obj.cert_limpwurt_root" count 60..150
        15 weight "obj.cert_yew_logs" count 70..90
        15 weight "obj.cert_raw_monkfish" count 60..80
        20 weight "obj.cert_white_berries" count 60..150
        30 weight "obj.kwuarm_seed" count 8..15
        30 weight "obj.ranarr_seed" count 8..15
        25 weight "obj.cert_blue_dragon_scale" count 20..40
        20 weight "obj.cert_raw_shark" count 60..80
        25 weight "obj.cert_red_spiders_eggs" count 40..60
        35 weight "obj.cert_magic_logs" count 30..50
        30 weight "obj.cert_wine_of_zamorak" count 30..50
        40 weight "obj.dwarf_weed_seed" count 5..8
        40 weight "obj.snapdragon_seed" count 5..8
        30 weight "obj.cert_raw_mantaray" count 40..60
        40 weight "obj.toadflax_seed" count 5..8
        3 outOf 85 separate "obj.rune_full_helm" count 1
        2 outOf 85 separate "obj.red_dragonhide_body" count 1
        1 outOf 85 separate rsPlayerWeightedTable {
            1 weight "obj.black_dragon_vambraces" count 1
            1 weight "obj.mystic_earth_staff" count 1
            1 weight "obj.dragon_mace" count 1
        }
        10 outOf 85 separate rsPlayerWeightedTable {
            10 weight "obj.firerune" count 100..200
            10 weight "obj.naturerune" count 30..60
            10 weight "obj.lawrune" count 25..50
            10 weight "obj.deathrune" count 20..40
            10 weight "obj.rune_arrow" count 35..65
        }
        4 outOf 85 separate rsPlayerWeightedTable {
            4 weight "obj.coins" count 1000..2000
            4 weight "obj.cert_diamond" count 3..6
            4 weight "obj.swordfish" count 1..2
        }
        1 outOf 85 separate "obj.coins" count 5000..7000

        5 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy avantoe [main/1/{{#expr:1/(5*{{#var:herbbase}}) round 1}}]
//   - Grimy kwuarm [main/1/{{#expr:1/(5*{{#var:herbbase}}) round 1}}]
//   - Grimy ranarr weed [main/1/{{#expr:1/(4*{{#var:herbbase}}) round 1}}]
//   - Grimy snapdragon [main/1/{{#expr:1/(4*{{#var:herbbase}}) round 1}}]
//   - Grimy cadantine [main/1/{{#expr:1/(4*{{#var:herbbase}}) round 1}}]
//   - Grimy dwarf weed [main/1/{{#expr:1/(4*{{#var:herbbase}}) round 1}}]
//   - Grimy lantadyme [main/1/{{#expr:1/(3*{{#var:herbbase}}) round 1}}]
//   - Grimy torstol [main/1/{{#expr:1/(3*{{#var:herbbase}}) round 1}}]
