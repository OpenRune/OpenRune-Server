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
public val bloodthirstyHydraDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Hydra Drops",
    npcs = npcs("npc.league_superior_hydra"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 361 weight "obj.hydra_eye" count 1 condition { player ->
            // Drops Need Manual: The order players receive the items in are: Hydra's eye, Hydra's fang, and finally Hydra's heart.
             true
        }
        1 outOf 361 weight "obj.hydra_heart" count 1
        1 outOf 361 weight "obj.hydra_fang" count 1
        1 outOf 1001 weight "obj.hydra_tail" count 1
        1 outOf 2000 weight "obj.dragon_thrownaxe" count 200..400
        1 outOf 2001 weight "obj.dragon_knife" count 200..400
    },
    mainTable = rsPlayerWeightedTable(total = 444) {
        name("Bloodthirsty Hydra Drops")
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
        3 outOf 128 separate rsPlayerWeightedTable {
            3 weight "obj.black_dragonhide_chaps" count 1
            3 weight "obj.rune_kiteshield" count 1
        }
        2 outOf 128 separate "obj.cert_battlestaff" count 2..3
        5 outOf 128 separate rsPlayerWeightedTable {
            5 weight "obj.fire_battlestaff" count 1
            5 weight "obj.water_battlestaff" count 1
        }
        1 outOf 128 separate rsPlayerWeightedTable {
            1 weight "obj.rune_platebody" count 1
            1 weight "obj.mystic_robe_bottom" count 1
            1 weight "obj.dragon_longsword" count 1
        }
        9 outOf 128 separate rsPlayerWeightedTable {
            9 weight "obj.bloodrune" count 15..45
            9 weight "obj.chaosrune" count 20..50
            9 weight "obj.deathrune" count 30..60
            9 weight "obj.firerune" count 70..90
            9 weight "obj.lawrune" count 30..60
            9 weight "obj.waterrune" count 70..90
        }
        16 outOf 128 separate "obj.coins" count 500..3500
        11 outOf 128 separate "obj.monkfish" count 1
        4 outOf 128 separate "obj.1dose2combat" count 1
        3 outOf 128 separate "obj.cert_dragon_bones" count 3..5
        6 outOf 128 separate "obj.1dose2restore" count 1..2

        4 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        4 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Grimy avantoe [main/1/{{#expr:1/(5*{{#var:uht}}) round 2}}]
//   - Grimy ranarr weed [main/1/{{#expr:1/(4*{{#var:uht}}) round 2}}]
//   - Grimy snapdragon [main/1/{{#expr:1/(3*{{#var:uht}}) round 2}}]
//   - Grimy torstol [main/1/{{#expr:1/(3*{{#var:uht}}) round 2}}]
//   - Diamond bolt tips [main/1/{{#expr:1/(10*{{#var:bolttipbase}}) round 1}}]
//   - Ruby bolt tips [main/1/{{#expr:1/(9*{{#var:bolttipbase}}) round 1}}]
//   - Emerald bolt tips [main/1/{{#expr:1/(9*{{#var:bolttipbase}}) round 1}}]
//   - Dragonstone bolt tips [main/1/{{#expr:1/(7*{{#var:bolttipbase}}) round 1}}]
//   - Onyx bolt tips [main/1/{{#expr:1/(3*{{#var:bolttipbase}}) round 1}}]
//   - Sapphire bolt tips [main/1/{{#expr:1/(2*{{#var:bolttipbase}}) round 1}}]
