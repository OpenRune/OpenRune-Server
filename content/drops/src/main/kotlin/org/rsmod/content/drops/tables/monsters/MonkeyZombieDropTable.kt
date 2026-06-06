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
public val monkeyZombieDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monkey Zombie Drops",
    npcs = npcs("npc.mm_zombie_monkey_large", "npc.mm_zombie_monkey_large_guard", "npc.mm_zombie_monkey_small"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.rag_monkey_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
