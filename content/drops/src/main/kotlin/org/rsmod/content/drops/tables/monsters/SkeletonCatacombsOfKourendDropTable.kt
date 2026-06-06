package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skeletonCatacombsOfKourendDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Catacombs of Kourend) Drops",
    npcs = npcs("npc.kourend_skeleton"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeleton (Catacombs of Kourend) Drops")
        6 weight "obj.iron_med_helm" count 1
        4 weight "obj.iron_sword" count 1
        2 weight "obj.iron_axe" count 1
        1 weight "obj.iron_scimitar" count 1
        3 weight "obj.airrune" count 15
        3 weight "obj.waterrune" count 9
        3 weight "obj.chaosrune" count 5
        2 weight "obj.iron_arrow" count 12
        2 weight "obj.lawrune" count 2
        1 weight "obj.cosmicrune" count 2
        24 weight "obj.coins" count 10
        25 weight "obj.coins" count 5
        8 weight "obj.coins" count 25
        4 weight "obj.coins" count 45
        3 weight "obj.coins" count 65
        2 weight "obj.coins" count 1
        8 weight ringNothing()
        5 weight "obj.bronze_bar" count 1

        20 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
        1 outOf 100 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_beginner")
        }
    },
)
