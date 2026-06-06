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
public val bloodthirstyBasiliskDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty basilisk Drops",
    npcs = npcs("npc.league_superior_basilisk"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty basilisk Drops")
        3 weight "obj.mithril_axe" count 1
        2 weight "obj.mithril_spear" count 1
        3 weight "obj.steel_battleaxe" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_dagger" count 1
        8 weight "obj.waterrune" count 75
        5 weight "obj.naturerune" count 15
        3 weight "obj.lawrune" count 3
        1 weight "obj.naturerune" count 37
        3 weight "obj.adamantite_ore" count 1
        29 weight "obj.coins" count 44
        17 weight "obj.coins" count 200
        5 weight "obj.coins" count 132
        5 weight "obj.coins" count 11
        1 weight "obj.coins" count 440
        5 outOf 76 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 76 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 76 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        6 outOf 76 separate "obj.cert_white_berries" count 60..150
        1 outOf 512 separate "obj.mystic_hat_light" count 1

        35 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_baby_basilisk_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 2000 weight "obj.poh_trophydrop_basilisk" count 1
    },
)
