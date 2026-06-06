package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val undeadLumberjackDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Undead Lumberjack Drops",
    npcs = npcs("npc.ttrek2_zombie_diff_1_ver_1_1", "npc.ttrek2_zombie_diff_1_ver_1_2", "npc.ttrek2_zombie_diff_1_ver_1_3", "npc.ttrek2_zombie_diff_1_ver_1_4", "npc.ttrek2_zombie_diff_1_ver_2_1", "npc.ttrek2_zombie_diff_1_ver_2_2", "npc.ttrek2_zombie_diff_1_ver_2_3", "npc.ttrek2_zombie_diff_1_ver_2_4", "npc.ttrek2_zombie_diff_2_ver_1_1", "npc.ttrek2_zombie_diff_2_ver_1_2", "npc.ttrek2_zombie_diff_2_ver_1_3", "npc.ttrek2_zombie_diff_2_ver_1_4", "npc.ttrek2_zombie_diff_2_ver_2_1", "npc.ttrek2_zombie_diff_2_ver_2_2", "npc.ttrek2_zombie_diff_2_ver_2_3", "npc.ttrek2_zombie_diff_2_ver_2_4", "npc.ttrek2_zombie_diff_3_ver_1_1", "npc.ttrek2_zombie_diff_3_ver_1_2", "npc.ttrek2_zombie_diff_3_ver_1_3", "npc.ttrek2_zombie_diff_3_ver_1_4", "npc.ttrek2_zombie_diff_3_ver_2_1", "npc.ttrek2_zombie_diff_3_ver_2_2", "npc.ttrek2_zombie_diff_3_ver_2_3", "npc.ttrek2_zombie_diff_3_ver_2_4", "npc.ttrek2_zombie_diff_4_ver_1_1", "npc.ttrek2_zombie_diff_4_ver_1_2", "npc.ttrek2_zombie_diff_4_ver_1_3", "npc.ttrek2_zombie_diff_4_ver_1_4", "npc.ttrek2_zombie_diff_4_ver_2_1", "npc.ttrek2_zombie_diff_4_ver_2_2", "npc.ttrek2_zombie_diff_4_ver_2_3", "npc.ttrek2_zombie_diff_4_ver_2_4", "npc.ttrek2_zombie_diff_5_ver_1_1", "npc.ttrek2_zombie_diff_5_ver_1_2", "npc.ttrek2_zombie_diff_5_ver_1_3", "npc.ttrek2_zombie_diff_5_ver_1_4", "npc.ttrek2_zombie_diff_5_ver_2_1", "npc.ttrek2_zombie_diff_5_ver_2_2", "npc.ttrek2_zombie_diff_5_ver_2_3", "npc.ttrek2_zombie_diff_5_ver_2_4", "npc.ttrek2_zombie_diff_6_ver_1_1", "npc.ttrek2_zombie_diff_6_ver_1_2", "npc.ttrek2_zombie_diff_6_ver_1_3", "npc.ttrek2_zombie_diff_6_ver_1_4", "npc.ttrek2_zombie_diff_6_ver_2_1", "npc.ttrek2_zombie_diff_6_ver_2_2", "npc.ttrek2_zombie_diff_6_ver_2_3", "npc.ttrek2_zombie_diff_6_ver_2_4", "npc.ttrek2_zombie_diff_7_ver_1_1", "npc.ttrek2_zombie_diff_7_ver_1_2", "npc.ttrek2_zombie_diff_7_ver_1_3", "npc.ttrek2_zombie_diff_7_ver_1_4", "npc.ttrek2_zombie_diff_7_ver_2_1", "npc.ttrek2_zombie_diff_7_ver_2_2", "npc.ttrek2_zombie_diff_7_ver_2_3", "npc.ttrek2_zombie_diff_7_ver_2_4", "npc.ttrek2_zombie_diff_8_ver_1_1", "npc.ttrek2_zombie_diff_8_ver_1_2", "npc.ttrek2_zombie_diff_8_ver_1_3", "npc.ttrek2_zombie_diff_8_ver_1_4", "npc.ttrek2_zombie_diff_8_ver_2_1", "npc.ttrek2_zombie_diff_8_ver_2_2", "npc.ttrek2_zombie_diff_8_ver_2_3", "npc.ttrek2_zombie_diff_8_ver_2_4", "npc.ttrek2_zombie_diff_9_ver_1_1", "npc.ttrek2_zombie_diff_9_ver_1_2", "npc.ttrek2_zombie_diff_9_ver_1_3", "npc.ttrek2_zombie_diff_9_ver_1_4", "npc.ttrek2_zombie_diff_9_ver_2_1", "npc.ttrek2_zombie_diff_9_ver_2_2", "npc.ttrek2_zombie_diff_9_ver_2_3", "npc.ttrek2_zombie_diff_9_ver_2_4"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.woodplank" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 4) {
        name("Undead Lumberjack Drops")
        1 weight "obj.ramble_lumberjack_top" count 1
        1 weight "obj.ramble_lumberjack_legs" count 1
        1 weight "obj.ramble_lumberjack_hat" count 1
        1 weight "obj.ramble_lumberjack_boots" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
