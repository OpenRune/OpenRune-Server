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
public val bloodthirstyGargoyleDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty gargoyle Drops",
    npcs = npcs("npc.league_superior_gargoyle", "npc.league_superior_gargoyle_dead"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.granite_maul" count 1
        1 outOf 512 weight "obj.mystic_robe_top_dark" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty gargoyle Drops")
        4 weight "obj.adamant_platelegs" count 1
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.rune_2h_sword" count 1
        1 weight "obj.adamant_armoured_boots" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.rune_platelegs" count 1
        10 weight "obj.firerune" count 75
        8 weight "obj.chaosrune" count 30
        6 weight "obj.firerune" count 150
        5 weight "obj.deathrune" count 15
        10 weight "obj.cert_gold_ore" count 10..20
        6 weight "obj.cert_blankrune_high" count 150
        6 weight "obj.cert_steel_bar" count 15
        3 weight "obj.cert_gold_bar" count 10..15
        2 weight "obj.cert_mithril_bar" count 15
        2 weight "obj.runite_ore" count 1
        28 weight "obj.coins" count 400..800
        20 weight "obj.coins" count 500..1000
        5 weight "obj.coins" count 10000
        5 outOf 315 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 315 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 315 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 315 separate rsPlayerWeightedTable {
            20 weight "obj.cert_white_berries" count 60..150
            20 weight "obj.cert_raw_shark" count 60..80
        }
        30 outOf 315 separate rsPlayerWeightedTable {
            30 weight "obj.kwuarm_seed" count 8..15
            30 weight "obj.ranarr_seed" count 8..15
            30 weight "obj.cert_wine_of_zamorak" count 30..50
            30 weight "obj.dwarf_weed_seed" count 5..8
        }
        25 outOf 315 separate rsPlayerWeightedTable {
            25 weight "obj.cert_blue_dragon_scale" count 20..40
            25 weight "obj.cert_red_spiders_eggs" count 40..60
        }
        35 outOf 315 separate "obj.cert_magic_logs" count 30..50

        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 150 weight "obj.slayer_roof_key" count 1 condition { player ->
            // Drops Need Manual: Brittle keys are only dropped if the player has not unlocked the roof access, does not have one in their bank or inventory, and has gargoyles as their current Slayer task.
             true
        }
    },
)
