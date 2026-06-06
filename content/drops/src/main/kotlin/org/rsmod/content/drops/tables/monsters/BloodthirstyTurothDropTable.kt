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
public val bloodthirstyTurothDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Turoth Drops",
    npcs = npcs("npc.league_superior_turoth"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Turoth Drops")
        7 weight "obj.steel_platelegs" count 1
        3 weight "obj.mithril_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.rune_dagger" count 1
        6 weight "obj.lawrune" count 3
        5 weight "obj.naturerune" count 15
        1 weight "obj.naturerune" count 37
        7 weight "obj.limpwurt_root" count 1
        29 weight "obj.coins" count 44
        12 weight "obj.coins" count 132
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
        1 outOf 500 separate "obj.leafbladed_sword" count 1
        1 outOf 512 separate "obj.mystic_robe_bottom_light" count 1

        31 weight rsWeightedTable(total = 31) {
            15 weight herbDropTable
            10 weight doubleRollHerbDropTable
            6 weight tripleRollHerbDropTable
        }
        5 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
