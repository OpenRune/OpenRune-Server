package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val riverTrollDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "River troll Drops",
    npcs = npcs("npc.fairy2_rivertrollguardian_1", "npc.fairy2_rivertrollguardian_2", "npc.fairy2_rivertrollguardian_3", "npc.fairy2_rivertrollguardian_4", "npc.fairy2_rivertrollguardian_5", "npc.fairy2_rivertrollguardian_6"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("River troll Drops")
        1 weight "obj.raw_tuna" count 1
        4 weight "obj.raw_salmon" count 1
        3 weight "obj.raw_herring" count 1
        6 weight "obj.raw_pike" count 1
        4 weight "obj.raw_sardine" count 1
        19 weight "obj.raw_swordfish" count 1
        12 weight "obj.raw_shark" count 1
        27 weight "obj.fishing_bait" count 5
        9 weight "obj.fishing_bait" count 15
        13 weight "obj.fishing_bait" count 30
        2 weight "obj.fishing_bait" count 50
        4 weight "obj.feather" count 20
        4 weight "obj.feather" count 40
        12 weight "obj.oystershell" count 1
        3 weight "obj.3dosefisherspotion" count 1
        5 weight "obj.casket" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_troll_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
