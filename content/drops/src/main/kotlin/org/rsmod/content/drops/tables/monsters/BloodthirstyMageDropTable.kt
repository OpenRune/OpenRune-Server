package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Mage Drops",
    npcs = npcs("npc.league_superior_infernal_mage"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Mage Drops")
        8 weight "obj.plainstaff" count 1
        1 weight "obj.staff_of_fire" count 1
        6 weight "obj.earthrune" count 10
        6 weight "obj.firerune" count 10
        4 weight "obj.earthrune" count 36
        3 weight "obj.airrune" count 10
        3 weight "obj.waterrune" count 10
        2 weight "obj.airrune" count 18
        2 weight "obj.waterrune" count 18
        2 weight "obj.earthrune" count 18
        2 weight "obj.firerune" count 18
        2 weight "obj.mindrune" count 18
        2 weight "obj.bodyrune" count 18
        2 weight "obj.bloodrune" count 4
        18 weight "obj.deathrune" count 7
        19 weight "obj.coins" count 1
        14 weight "obj.coins" count 2
        8 weight "obj.coins" count 4
        3 weight "obj.coins" count 29
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
        1 outOf 512 separate rsPlayerWeightedTable {
            1 weight "obj.mystic_boots_dark" count 1
            1 weight "obj.mystic_hat_dark" count 1
        }
        1 outOf 1000 separate "obj.lava_battlestaff" count 1
        21 weight nothing()
    },
)
