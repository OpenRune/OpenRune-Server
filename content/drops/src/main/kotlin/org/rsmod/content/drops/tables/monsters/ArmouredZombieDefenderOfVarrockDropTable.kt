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
public val armouredZombieDefenderOfVarrockDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Armoured zombie (Defender of Varrock) Drops",
    npcs = npcs("npc.dov_armoured_zombie_varrock_melee_1", "npc.dov_armoured_zombie_varrock_melee_1_huntplayer", "npc.dov_armoured_zombie_varrock_melee_2", "npc.dov_armoured_zombie_varrock_melee_3", "npc.dov_armoured_zombie_varrock_melee_3_huntplayer", "npc.dov_armoured_zombie_varrock_melee_4", "npc.dov_armoured_zombie_varrock_melee_4_huntplayer", "npc.dov_armoured_zombie_varrock_melee_5", "npc.dov_armoured_zombie_varrock_ranged_1", "npc.dov_armoured_zombie_varrock_ranged_2", "npc.dov_armoured_zombie_varrock_ranged_2_huntplayer", "npc.dov_armoured_zombie_varrock_ranged_4", "npc.dov_armoured_zombie_varrock_ranged_4_huntplayer", "npc.dov_armoured_zombie_varrock_ranged_5", "npc.dov_armoured_zombie_varrock_ranged_5_huntplayer"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
