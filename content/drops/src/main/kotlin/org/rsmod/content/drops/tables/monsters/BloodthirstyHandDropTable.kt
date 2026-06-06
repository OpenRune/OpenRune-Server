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
public val bloodthirstyHandDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty hand Drops",
    npcs = npcs("npc.league_superior_crawling_hand"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty hand Drops")
        21 weight "obj.leather_gloves" count 1
        2 weight "obj.wolfengloves_purple" count 1
        2 weight "obj.wolfengloves_tangerine" count 1
        2 weight "obj.wolfengloves_crimson" count 1
        2 weight "obj.wolfengloves_ocean" count 1
        3 weight "obj.gold_ring" count 1
        2 weight "obj.sapphire_ring" count 1
        2 weight "obj.emerald_ring" count 1
        21 weight "obj.coins" count 5
        23 weight "obj.coins" count 8
        5 outOf 40 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 40 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }

        2 weight SharedDropTables.gem
        46 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 500 weight "obj.poh_trophydrop_crawlinghand" count 1
    },
)
