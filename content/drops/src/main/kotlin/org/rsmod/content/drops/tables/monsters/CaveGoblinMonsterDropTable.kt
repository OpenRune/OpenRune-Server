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
public val caveGoblinMonsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave goblin (monster) Drops",
    npcs = npcs("npc.cave_goblin", "npc.cave_goblin2", "npc.cave_goblin3", "npc.cave_goblin4"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Cave goblin (monster) Drops")
        5 weight "obj.bodyrune" count 7
        5 weight "obj.waterrune" count 6
        5 weight "obj.earthrune" count 4
        4 weight "obj.coins" count 5
        4 weight "obj.coins" count 9
        6 weight "obj.coins" count 15
        5 weight "obj.coins" count 20
        1 weight "obj.coins" count 1
        6 weight ringNothing()
        4 weight "obj.hammer" count 1
        1 weight "obj.brass_necklace" count 1
        4 weight "obj.tinderbox" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_cave_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_goblin" count 1
        1 outOf 64 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
