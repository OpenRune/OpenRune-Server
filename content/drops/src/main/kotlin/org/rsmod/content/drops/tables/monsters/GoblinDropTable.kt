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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val goblinDropTable1: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin Drop table 1",
    npcs = npcs("npc.goblin", "npc.goblin_armed_melee_1", "npc.goblin_armed_melee_2", "npc.goblin_armed_melee_3", "npc.goblin_armed_melee_4", "npc.goblin_unarmed_mcannon_1", "npc.goblin_unarmed_mcannon_2", "npc.goblin_unarmed_mcannon_3", "npc.goblin_unarmed_mcannon_4", "npc.goblin_unarmed_mcannon_5", "npc.goblin_unarmed_mcannon_6", "npc.goblin_unarmed_mcannon_7", "npc.goblin_unarmed_mcannon_8", "npc.goblin_unarmed_mcannon_9", "npc.goblin_unarmed_melee_1", "npc.goblin_unarmed_melee_2", "npc.goblin_unarmed_melee_3", "npc.goblin_unarmed_melee_4", "npc.goblin_unarmed_melee_5", "npc.goblin_unarmed_melee_6", "npc.goblin_unarmed_melee_7", "npc.goblin_unarmed_melee_8", "npc.goblin_unarmed_melee_in_1", "npc.goblin_unarmed_melee_in_2", "npc.goblin_unarmed_melee_in_3", "npc.goblin_unarmed_melee_in_4", "npc.goblin_unarmed_melee_in_5", "npc.goblin_unarmed_melee_in_6", "npc.goblin_unarmed_melee_in_7", "npc.goblin_unarmed_melee_in_8"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Goblin Drop table 1")
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
    },
)

@field:RegisterDropTable
@JvmField
public val goblinDropTable2: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin Drop table 2",
    npcs = npcs("npc.fai_barbarian_goblin_armed_1", "npc.fai_barbarian_goblin_armed_2", "npc.fai_barbarian_goblin_armed_3", "npc.fai_barbarian_goblin_armed_4", "npc.goblin_armed", "npc.goblin_armed_mcannon_1", "npc.goblin_armed_mcannon_2", "npc.goblin_armed_mcannon_3", "npc.goblin_armed_mcannon_4", "npc.goblin_armed_mcannon_5", "npc.goblin_helmet", "npc.mcannon_goblin1", "npc.mcannon_goblin_guard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Goblin Drop table 2")
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
        8 weight ringNothing()
        9 weight "obj.hammer" count 1
        2 weight "obj.slice_goblin_history_book" count 1
        10 weight "obj.goblin_armour" count 1 condition { player ->
            // Drops Need Manual: Colour received depends on the goblin mail worn.
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
