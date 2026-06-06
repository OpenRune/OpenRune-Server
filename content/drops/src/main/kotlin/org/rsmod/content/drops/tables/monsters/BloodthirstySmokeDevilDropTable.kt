package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstySmokeDevilDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty smoke devil Drops",
    npcs = npcs("npc.league_superior_smoke_devil"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty smoke devil Drops")
        3 weight "obj.adamant_battleaxe" count 1
        3 weight "obj.rune_dagger" count 1
        3 weight "obj.air_battlestaff" count 1
        3 weight "obj.black_dragon_vambraces" count 1
        3 weight "obj.fire_battlestaff" count 1
        2 weight "obj.mithril_plateskirt" count 1
        2 weight "obj.rune_full_helm" count 1
        2 weight "obj.rune_chainbody" count 1
        1 weight "obj.red_dragonhide_body" count 1
        11 weight "obj.smokerune" count 15
        5 weight "obj.smokerune" count 40
        5 weight "obj.xbows_crossbow_bolts_runite" count 15
        4 weight "obj.firerune" count 37
        4 weight "obj.airrune" count 37
        4 weight "obj.soulrune" count 10
        2 weight "obj.firerune" count 150
        2 weight "obj.rune_arrow" count 24
        12 weight "obj.coins" count 750
        7 weight "obj.coins" count 80
        3 weight "obj.coins" count 300
        6 weight "obj.shark" count 1
        3 weight "obj.steel_bar" count 2
        3 weight "obj.cert_magic_logs" count 5
        3 weight "obj.cert_coal" count 15
        2 weight "obj.adamantite_bar" count 1
        2 weight "obj.xbows_crossbow_string" count 1
        2 weight "obj.ugthanki_kebab" count 3
        5 outOf 435 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 435 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 435 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 435 separate rsPlayerWeightedTable {
            20 weight "obj.cert_white_berries" count 60..150
            20 weight "obj.cert_raw_shark" count 60..80
        }
        30 outOf 435 separate rsPlayerWeightedTable {
            30 weight "obj.kwuarm_seed" count 8..15
            30 weight "obj.ranarr_seed" count 8..15
            30 weight "obj.cert_wine_of_zamorak" count 30..50
            30 weight "obj.cert_raw_mantaray" count 40..60
        }
        25 outOf 435 separate rsPlayerWeightedTable {
            25 weight "obj.cert_blue_dragon_scale" count 20..40
            25 weight "obj.cert_red_spiders_eggs" count 40..60
        }
        35 outOf 435 separate "obj.cert_magic_logs" count 30..50
        40 outOf 435 separate rsPlayerWeightedTable {
            40 weight "obj.dwarf_weed_seed" count 5..8
            40 weight "obj.snapdragon_seed" count 5..8
            40 weight "obj.toadflax_seed" count 5..8
        }
        1 outOf 512 separate "obj.occult_necklace" count 1
        1 outOf 32768 separate "obj.dragon_chainbody" count 1

        18 weight rsWeightedTable(total = 3) {
            2 weight herbDropTable
            1 weight doubleRollHerbDropTable
        }
        4 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
