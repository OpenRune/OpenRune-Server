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
public val bloodthirstyNightBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Night beast Drops",
    npcs = npcs("npc.league_superior_dark_beast"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count 1 condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Night beast Drops")
        3 weight "obj.black_battleaxe" count 1
        1 weight "obj.adamant_sq_shield" count 1
        1 weight "obj.rune_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.rune_2h_sword" count 1
        1 weight "obj.rune_battleaxe" count 1
        8 weight "obj.deathrune" count 20
        7 weight "obj.chaosrune" count 30
        4 weight "obj.bloodrune" count 15
        2 weight "obj.cert_adamantite_bar" count 3
        1 weight "obj.cert_adamantite_ore" count 5
        1 weight "obj.cert_runite_ore" count 1
        40 weight "obj.coins" count 152
        6 weight "obj.coins" count 64
        6 weight "obj.coins" count 95
        5 weight "obj.coins" count 220
        3 weight "obj.shark" count 1
        1 weight "obj.shark" count 2
        1 weight "obj.death_talisman" count 1
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
        1 outOf 512 separate "obj.darkbow" count 1

        24 weight rsWeightedTable(total = 5) {
            4 weight herbDropTable
            1 weight doubleRollHerbDropTable
        }
        3 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
        4 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/114 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 120 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
