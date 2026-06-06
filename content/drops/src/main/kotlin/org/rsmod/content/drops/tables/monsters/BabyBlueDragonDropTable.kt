package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val babyBlueDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Baby blue dragon Drops",
    npcs = npcs("npc.baby_blue_dragon_tapoyauik_1", "npc.baby_blue_dragon_tapoyauik_2", "npc.babybluedragon", "npc.babybluedragon2", "npc.babybluedragon3"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_baby_blue_dragon_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 100 weight "obj.scaly_bluehide" count 1
        1 outOf 100 weight "obj.frozen_tear" count 6 condition { player ->
            // Drops Need Manual: Only dropped in Ruins of Tapoyauik
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1066 weight "obj.varlamore_key_half_1" count 1
    },
)
