package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyAbyssalDemonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty abyssal demon Drops",
    npcs = npcs("npc.league_superior_abyssal_demon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_abyssal" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty abyssal demon Drops")
        4 weight "obj.black_sword" count 1
        3 weight "obj.steel_battleaxe" count 1
        2 weight "obj.black_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        35 weight "obj.coins" count 132
        9 weight "obj.coins" count 220
        7 weight "obj.coins" count 30
        6 weight "obj.coins" count 44
        1 weight "obj.coins" count 460
        8 weight "obj.airrune" count 50
        6 weight "obj.chaosrune" count 10
        5 weight "obj.cert_blankrune_high" count 60
        4 weight "obj.bloodrune" count 7
        2 weight "obj.lobster" count 1
        2 weight "obj.adamantite_bar" count 1
        1 weight "obj.lawrune" count 3
        1 weight "obj.cosmic_talisman" count 1
        1 weight "obj.chaos_talisman" count 1
        1 weight "obj.3dose1defense" count 1
        1 weight "obj.cert_raw_monkfish" count 75
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
        1 outOf 512 separate "obj.abyssal_whip" count 1
        1 outOf 32000 separate "obj.abyssal_dagger" count 1

        19 weight SharedDropTables.herb
        2 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 6000 weight "obj.poh_trophydrop_abyssaldemon" count 1
    },
)
