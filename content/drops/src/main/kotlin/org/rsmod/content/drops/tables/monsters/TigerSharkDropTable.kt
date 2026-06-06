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
public val tigerSharkDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tiger shark Drops",
    npcs = npcs("npc.sailing_tiger_shark"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Tiger shark Drops")
        10 weight "obj.xbows_crossbow_adamantite" count 1
        8 weight "obj.adamant_platelegs" count 1
        6 weight "obj.adamant_mace" count 1
        5 weight "obj.adamant_halberd" count 1
        5 weight "obj.snakeskin_chaps" count 1
        4 weight "obj.battlestaff" count 1
        3 weight "obj.water_battlestaff" count 1
        3 weight "obj.rune_dagger" count 1
        24 weight "obj.emerald_ring" count 1
        10 weight "obj.mithril_arrowheads" count 20..40
        8 weight "obj.raw_shark" count 1
        5 weight "obj.deathrune" count 20..30
        4 weight "obj.mcannonball" count 48..60
        2 weight "obj.sailing_fisherman_shipwreck_salvage" count 1
        12 outOf 1000 separate "obj.coral_elkhorn_frag" count 1
        6 outOf 1000 separate "obj.coral_pillar_frag" count 1
        2 outOf 1000 separate "obj.coral_umbral_frag" count 1

        1 weight SharedDropTables.gem
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.sailing_tiger_shark_liver" count 1 condition { player ->
            // Drops Need Manual: Only dropped while on an applicable bounty task.
             true
        }
        1 outOf 2 weight "obj.sailing_tiger_shark_jaw" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (hard) [tertiary/Rare]
