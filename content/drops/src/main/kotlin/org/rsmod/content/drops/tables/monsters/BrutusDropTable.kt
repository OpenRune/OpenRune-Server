package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brutusDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brutus Drops",
    npcs = npcs("npc.cowboss", "npc.cowboss_routefind"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_tbone_steak" count 1
    },
    preRoll = rsPlayerPrerollTable {
        5 outOf 150 weight "obj.mooleta" count 1
        4 outOf 150 weight "obj.bottomless_milk_bucket" count 1
        1 outOf 150 weight "obj.cow_slippers" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 81) {
        name("Brutus Drops")
        2 weight "obj.iron_full_helm" count 1
        2 weight "obj.iron_platebody" count 1
        1 weight "obj.iron_platelegs" count 1
        1 weight "obj.iron_plateskirt" count 1
        10 weight "obj.iron_arrow" count 14
        10 weight "obj.airrune" count 29
        8 weight "obj.mindrune" count 18
        2 weight "obj.chaosrune" count 12
        10 weight "obj.potato_seed" count 3
        5 weight "obj.acorn" count 2
        10 weight "obj.cert_raw_tbone_steak" count 3
        10 weight "obj.cow_hide" count 1
        5 weight "obj.oak_logs" count 2
        5 weight "obj.logs" count 2
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1000 weight "obj.cowbosspet" count 1
        1 outOf 15 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
