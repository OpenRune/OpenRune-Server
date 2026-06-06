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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val caveGoblinMinerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave goblin miner Drops",
    npcs = npcs("npc.cave_goblin_miner", "npc.cave_goblin_miner2", "npc.cave_goblin_miner2_mining", "npc.cave_goblin_miner3", "npc.cave_goblin_miner3_mining", "npc.cave_goblin_miner4", "npc.cave_goblin_miner4_mining", "npc.cave_goblin_miner_mining"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Cave goblin miner Drops")
        28 weight ringNothing()
        20 weight "obj.cave_goblin_mining_helmet_unlit" count 1
        20 weight "obj.coins" count 6
        20 weight "obj.iron_ore" count 7
        20 weight "obj.tinderbox" count 1
        15 weight "obj.iron_ore" count 2
        5 weight "obj.silver_ore" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_cave_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
    },
)
