package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyTwistedBansheeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty twisted banshee Drops",
    npcs = npcs("npc.league_superior_kourend_banshee"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty twisted banshee Drops")
        5 weight "obj.battlestaff" count 1
        1 weight "obj.adamant_kiteshield" count 1
        2 weight "obj.air_battlestaff" count 1
        2 weight "obj.adamant_mace" count 1
        2 weight "obj.rune_dagger" count 1
        2 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_full_helm" count 1
        3 weight "obj.cosmicrune" count 20
        2 weight "obj.chaosrune" count 30
        1 weight "obj.chaosrune" count 17
        1 weight "obj.firerune" count 35
        21 weight "obj.cert_blankrune_high" count 65
        1 weight "obj.eye_of_newt" count 1
        1 weight "obj.mithril_ore" count 1
        9 weight "obj.coins" count 130
        7 weight "obj.coins" count 35
        7 weight "obj.coins" count 260
        5 weight "obj.swordfish" count 1
        5 outOf 40 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 40 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        1 outOf 256 separate "obj.mystic_gloves_dark" count 1
        5 outOf 256 separate "obj.airrune" count 30

        34 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        13 weight SharedDropTables.rareSeed
        6 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
    },
)
