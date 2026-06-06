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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyBasiliskSentinelDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Basilisk Sentinel Drops",
    npcs = npcs("npc.league_superior_basilisk_knight"),
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Bloodthirsty Basilisk Sentinel Drops")
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.adamant_kiteshield" count 1
        2 weight "obj.rune_axe" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_scimitar" count 1
        1 weight "obj.rune_spear" count 1
        1 weight "obj.rune_med_helm" count 1
        6 weight "obj.astralrune" count 15..35
        6 weight "obj.naturerune" count 15..30
        6 weight "obj.lawrune" count 20..30
        3 weight "obj.deathrune" count 10..25
        3 weight "obj.bloodrune" count 8..20
        7 weight "obj.coins" count 500..2498
        1 weight "obj.cert_adamantite_ore" count 1..2
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

        8 weight SharedDropTables.gem
        3 weight SharedDropTables.usefulHerb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_baby_basilisk_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 256 weight "obj.mystic_hat_light" count 1
        1 outOf 1000 weight "obj.poh_trophydrop_basilisk" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 1000 weight "obj.basilisk_jaw" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
