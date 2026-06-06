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
public val crawlingHandDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crawling Hand Drops",
    npcs = npcs("npc.slayer_crawling_hand_1", "npc.slayer_crawling_hand_2", "npc.slayer_crawling_hand_3", "npc.slayer_crawling_hand_4", "npc.slayer_crawling_hand_5", "npc.slayer_crawling_hand_big_1", "npc.slayer_crawling_hand_big_2", "npc.slayer_crawling_hand_big_3", "npc.slayer_crawling_hand_big_4", "npc.slayer_crawling_hand_big_5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Crawling Hand Drops")
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

        2 weight SharedDropTables.gem
        46 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 500 weight "obj.poh_trophydrop_crawlinghand" count 1
    },
)
