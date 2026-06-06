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
public val bloodthirstyRepugnantSpectreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Repugnant spectre Drops",
    npcs = npcs("npc.league_superior_kourend_spectre"),
    mainTable = rsPlayerWeightedTable(total = 499) {
        name("Bloodthirsty Repugnant spectre Drops")
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
        10 weight "obj.toadflax_seed" count 5..8
        5 outOf 128 separate rsPlayerWeightedTable {
            5 weight "obj.battlestaff" count 1
            5 weight "obj.black_platelegs" count 1
            5 weight "obj.mithril_battleaxe" count 1
        }
        2 outOf 128 separate "obj.rune_full_helm" count 1
        1 outOf 128 separate rsPlayerWeightedTable {
            1 weight "obj.lava_battlestaff" count 1
            1 weight "obj.rune_chainbody" count 1
        }
        5 outOf 128 separate "obj.adamantite_ore" count 1

        46 weight rsWeightedTable(total = 46) {
            29 weight doubleRollHerbDropTable
            17 weight tripleRollHerbDropTable
        }
        32 weight SharedDropTables.gem
        16 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 512 weight "obj.mystic_robe_bottom_dark" count 1
    },
)
