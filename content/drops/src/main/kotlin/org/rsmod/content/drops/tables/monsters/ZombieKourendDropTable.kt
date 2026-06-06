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
public val zombieKourendDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie (Kourend) Drops",
    npcs = npcs("npc.kourend_zombie1", "npc.kourend_zombie2", "npc.kourend_zombie3", "npc.kourend_zombie4"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Zombie (Kourend) Drops")
        3 weight "obj.iron_mace" count 1
        2 weight "obj.iron_dagger" count 1
        1 weight "obj.bronze_kiteshield" count 1
        3 weight "obj.mithril_arrow" count 1
        2 weight "obj.bodyrune" count 3
        3 weight "obj.airrune" count 3
        1 weight "obj.chaosrune" count 4
        1 weight "obj.cosmicrune" count 2
        1 weight "obj.firerune" count 7
        10 weight "obj.coins" count 10
        21 weight "obj.coins" count 18
        8 weight "obj.coins" count 26
        6 weight "obj.coins" count 35
        2 weight "obj.coins" count 1
        26 weight "obj.fishing_bait" count 7
        3 weight ringNothing()
        2 weight "obj.tinderbox" count 1
        1 weight "obj.eye_of_newt" count 1
        1 weight "obj.tin_ore" count 1

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
