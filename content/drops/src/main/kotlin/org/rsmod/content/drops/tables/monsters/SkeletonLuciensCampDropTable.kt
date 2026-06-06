package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skeletonLuciensCampDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (Lucien's camp) Drops",
    npcs = npcs("npc.wgs_skeleton_1", "npc.wgs_skeleton_2", "npc.wgs_skeleton_3", "npc.wgs_skeleton_4"),
    mainTable = rsPlayerWeightedTable(total = 120) {
        name("Skeleton (Lucien's camp) Drops")
        2 weight "obj.adamant_axe" count 1
        2 weight "obj.adamant_sword" count 1
        2 weight "obj.rune_scimitar" count 1
        2 weight "obj.rune_med_helm" count 1
        4 weight "obj.waterrune" count 20
        6 weight "obj.cosmicrune" count 12
        4 weight "obj.chaosrune" count 10
        4 weight "obj.lawrune" count 8
        4 weight "obj.mithril_arrow" count 15
        2 weight "obj.bloodrune" count 10
        6 weight "obj.mithril_bar" count 1
        20 weight "obj.coins" count 5
        10 weight "obj.coins" count 25
        19 weight "obj.coins" count 40
        20 weight "obj.coins" count 80
        2 weight "obj.coins" count 45
        5 weight "obj.coins" count 100..200
        9 outOf 6720 separate rsPlayerWeightedTable {
            9 weight "obj.opal_bolttips" count 4..10
            9 weight "obj.pearl_bolttips" count 4..10
            9 weight "obj.xbows_bolt_tips_diamond" count 4..10
        }
        3 outOf 6720 separate rsPlayerWeightedTable {
            3 weight "obj.xbows_bolt_tips_jade" count 4..10
            3 weight "obj.xbows_bolt_tips_redtopaz" count 4..10
            3 weight "obj.xbows_bolt_tips_sapphire" count 4..10
            3 weight "obj.xbows_bolt_tips_onyx" count 4..10
        }
        5 outOf 6720 separate "obj.xbows_bolt_tips_emerald" count 4..10
        6 outOf 6720 separate rsPlayerWeightedTable {
            6 weight "obj.xbows_bolt_tips_ruby" count 4..10
            6 weight "obj.xbows_bolt_tips_dragonstone" count 4..10
        }

        1 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.seed
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/95 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
