package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val goblinGoblinVillageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin (Goblin Village) Drops",
    npcs = npcs("npc.goblin_green_soldier_2", "npc.goblin_green_soldier_3", "npc.goblin_green_soldier_4", "npc.goblin_green_soldier_5", "npc.goblin_green_soldier_6", "npc.goblin_green_soldier_7", "npc.goblin_green_soldier_8", "npc.goblin_red_soldier_2", "npc.goblin_red_soldier_3", "npc.goblin_red_soldier_4", "npc.goblin_red_soldier_5", "npc.goblin_red_soldier_6", "npc.goblin_red_soldier_7", "npc.goblin_red_soldier_8"),
    mainTable = rsPlayerWeightedTable(total = 130) {
        name("Goblin (Goblin Village) Drops")
        3 weight "obj.bronze_axe" count 1
        1 weight "obj.bronze_scimitar" count 1
        9 weight "obj.bronze_spear" count 1
        3 weight "obj.bronze_arrow" count 7
        3 weight "obj.mindrune" count 2
        3 weight "obj.earthrune" count 4
        3 weight "obj.bodyrune" count 2
        2 weight "obj.bronze_javelin" count 5
        1 weight "obj.chaosrune" count 1
        1 weight "obj.naturerune" count 1
        34 weight "obj.coins" count 1
        13 weight "obj.coins" count 3
        8 weight "obj.coins" count 5
        7 weight "obj.coins" count 16
        3 weight "obj.coins" count 24
        9 weight "obj.hammer" count 1
        2 weight "obj.slice_goblin_history_book" count 1
        10 weight "obj.goblin_armour_red" count 1 condition { player ->
            // Drops Need Manual: Only dropped by goblins wearing red goblin mail.
             true
        }
        10 weight "obj.goblin_armour_green" count 1 condition { player ->
            // Drops Need Manual: Only dropped by goblins wearing green goblin mail.
             true
        }
        1 weight "obj.grapes" count 1
        1 weight "obj.red_cape" count 1
        1 weight "obj.tin_ore" count 1

        2 weight SharedDropTables.herb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 30 weight "obj.arceuus_corpse_goblin" count 1
        1 outOf 5000 weight "obj.champions_challenge_goblin" count 1
        1 outOf 80 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
