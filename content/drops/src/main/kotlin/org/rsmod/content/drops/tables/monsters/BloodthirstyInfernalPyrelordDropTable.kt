package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyInfernalPyrelordDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Infernal pyrelord Drops",
    npcs = npcs("npc.league_superior_pyrelord"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Infernal pyrelord Drops")
        4 weight "obj.mithril_axe" count 1
        4 weight "obj.mithril_full_helm" count 1
        3 weight "obj.staff_of_fire" count 1
        2 weight "obj.mithril_chainbody" count 1
        1 weight "obj.adamant_med_helm" count 1
        1 weight "obj.steel_armoured_boots" count 1
        21 weight "obj.firerune" count 50
        8 weight "obj.firerune" count 100
        5 weight "obj.chaosrune" count 20
        3 weight "obj.deathrune" count 5
        7 weight "obj.coins" count 10
        24 weight "obj.coins" count 60
        20 weight "obj.coins" count 200
        10 weight "obj.coins" count 400
        2 weight "obj.coins" count 600
        8 weight "obj.gold_ore" count 2
        2 weight "obj.jug_wine" count 1
        5 outOf 40 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 40 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }

        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_medium_emote_exp1" count 1
    },
)
