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
public val goradDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gorad Drops",
    npcs = npcs("npc.gorad"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.ogretooth" count 1 condition {
            player -> player.isOnQuest("quest_watchtowerquest")
        }
        1 outOf 4 weight "obj.rag_ogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 30 weight "obj.arceuus_corpse_ogre" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
