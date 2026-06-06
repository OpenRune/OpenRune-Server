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
public val guardCaveGoblinDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard (Cave goblin) Drops",
    npcs = npcs("npc.dorgesh_guard1", "npc.dorgesh_guard2"),
    mainTable = rsPlayerWeightedTable(total = 148) {
        name("Guard (Cave goblin) Drops")
        20 weight "obj.cave_goblin_bone_club" count 1 condition { player ->
            // Drops Need Manual: Bone clubs only drop from the bone club wielding guards.
             true
        }
        20 weight "obj.cave_goblin_bone_spear" count 1 condition { player ->
            // Drops Need Manual: Bone spears only drop from the bone spear wielding guards.
             true
        }
        20 weight "obj.iron_chainbody" count 1
        28 weight ringNothing()
        20 weight "obj.coins" count 12
        20 weight "obj.oil_lantern_unlit" count 1
        20 weight "obj.tinderbox" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_cave_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_goblin" count 1
    },
)
