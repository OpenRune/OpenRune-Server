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
public val goblinGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin guard Drops",
    npcs = npcs("npc.goblin_guard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Goblin guard Drops")
        3 weight "obj.bronze_sq_shield" count 1
        4 weight "obj.bronze_spear" count 1
        5 weight "obj.bodyrune" count 7
        6 weight "obj.waterrune" count 6
        3 weight "obj.earthrune" count 4
        3 weight "obj.bolt" count 8
        28 weight "obj.coins" count 5
        3 weight "obj.coins" count 9
        3 weight "obj.coins" count 15
        2 weight "obj.coins" count 20
        1 weight "obj.coins" count 1
        38 weight ringNothing()
        15 weight "obj.hammer" count 1
        2 weight "obj.slice_goblin_history_book" count 1
        5 weight "obj.goblin_armour" count 1
        3 weight "obj.chefs_hat" count 1
        2 weight "obj.beer" count 1
        1 weight "obj.brass_necklace" count 1
        1 weight "obj.air_talisman" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 35 weight "obj.arceuus_corpse_goblin" count 1
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
