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
public val bloodthirstyBloodveldDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Bloodveld Drops",
    npcs = npcs("npc.league_superior_bloodveld"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_bloodveld" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 138) {
        name("Bloodthirsty Bloodveld Drops")
        4 weight "obj.steel_axe" count 1
        4 weight "obj.steel_full_helm" count 1
        2 weight "obj.steel_scimitar" count 1
        1 weight "obj.black_armoured_boots" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.mithril_chainbody" count 1
        1 weight "obj.rune_med_helm" count 1
        8 weight "obj.firerune" count 60
        3 weight "obj.bloodrune" count 3
        5 weight "obj.bloodrune" count 10
        1 weight "obj.bloodrune" count 30
        7 weight "obj.coins" count 10
        29 weight "obj.coins" count 40
        30 weight "obj.coins" count 120
        10 weight "obj.coins" count 200
        1 weight "obj.coins" count 450
        10 weight "obj.bones" count 1
        7 weight "obj.big_bones" count 1
        3 weight "obj.big_bones" count 3
        2 weight "obj.gold_ore" count 1
        3 weight "obj.meat_pizza" count 1
        5 outOf 136 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 136 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 136 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 136 separate "obj.cert_white_berries" count 60..150
        30 outOf 136 separate "obj.kwuarm_seed" count 8..15
        16 outOf 136 separate "obj.ranarr_seed" count 8..15

        1 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
