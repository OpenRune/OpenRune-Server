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
public val zombiePirateBraindeathIslandDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie pirate (Braindeath Island) Drops",
    npcs = npcs("npc.deal_zombie_pirates_1", "npc.deal_zombie_pirates_2", "npc.deal_zombie_pirates_3", "npc.deal_zombie_pirates_4", "npc.deal_zombie_pirates_5", "npc.deal_zombie_pirates_6"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Zombie pirate (Braindeath Island) Drops")
        3 weight "obj.steel_scimitar" count 1
        2 weight "obj.iron_dagger" count 1
        3 weight "obj.bolt" count 2..12
        3 weight "obj.airrune" count 3
        3 weight "obj.cosmicrune" count 2
        2 weight "obj.bodyrune" count 3
        1 weight "obj.waterrune" count 17
        1 weight "obj.chaosrune" count 4
        21 weight "obj.coins" count 18
        10 weight "obj.coins" count 10
        8 weight "obj.coins" count 26
        6 weight "obj.coins" count 35
        2 weight "obj.coins" count 40
        21 weight "obj.fishing_bait" count 7
        5 weight "obj.fishing_bait" count 10
        3 weight "obj.deal_karamthulhu" count 1
        1 weight "obj.iron_ore" count 1
        1 weight "obj.eye_patch" count 1
        1 weight "obj.deal_rusty_scimitar" count 1

        30 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
