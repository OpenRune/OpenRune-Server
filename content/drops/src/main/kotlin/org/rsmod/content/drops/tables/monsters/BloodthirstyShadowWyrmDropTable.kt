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
public val bloodthirstyShadowWyrmDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Shadow Wyrm Drops",
    npcs = npcs("npc.league_superior_wyrm_dark", "npc.league_superior_wyrm_light"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 2000 weight "obj.dragon_shortsword" count 1
        1 outOf 2000 weight "obj.dragon_harpoon" count 1
        1 outOf 2000 weight "obj.dragon_knife" count 75..150
        1 outOf 2000 weight "obj.dragon_thrownaxe" count 75..150
    },
    mainTable = rsPlayerWeightedTable(total = 76) {
        name("Bloodthirsty Shadow Wyrm Drops")
        2 weight "obj.adamant_axe" count 1
        3 weight "obj.red_dragonhide_chaps" count 1
        2 weight "obj.adamant_sq_shield" count 1
        2 weight "obj.adamant_battleaxe" count 1
        2 weight "obj.adamant_2h_sword" count 1
        1 weight "obj.earth_battlestaff" count 1
        2 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.dragon_dagger" count 1
        10 weight "obj.firerune" count 200
        10 weight "obj.earthrune" count 75..150
        5 weight "obj.soulrune" count 15..20
        5 weight "obj.bloodrune" count 25..30
        8 weight "obj.coins" count 950..1450
        7 weight "obj.bass" count 1
        3 weight "obj.cert_blankrune_high" count 200..300
        2 weight "obj.rune_arrowheads" count 8..12
        2 weight "obj.adamant_arrowheads" count 8..12
        5 outOf 255 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 255 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 255 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 255 separate rsPlayerWeightedTable {
            20 weight "obj.cert_white_berries" count 60..150
            20 weight "obj.cert_raw_shark" count 60..80
        }
        30 outOf 255 separate rsPlayerWeightedTable {
            30 weight "obj.kwuarm_seed" count 8..15
            30 weight "obj.ranarr_seed" count 8..15
        }
        25 outOf 255 separate rsPlayerWeightedTable {
            25 weight "obj.cert_blue_dragon_scale" count 20..40
            25 weight "obj.cert_red_spiders_eggs" count 40..60
        }
        35 outOf 255 separate "obj.cert_magic_logs" count 30..50

        4 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        3 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
    },
)
