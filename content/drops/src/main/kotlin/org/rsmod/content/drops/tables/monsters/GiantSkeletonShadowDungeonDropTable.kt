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
public val giantSkeletonShadowDungeonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant skeleton (Shadow Dungeon) Drops",
    npcs = npcs("npc.sword_skeleton_3", "npc.sword_skeleton_3b"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Giant skeleton (Shadow Dungeon) Drops")
        6 weight "obj.steel_med_helm" count 1
        4 weight "obj.steel_sword" count 1
        1 weight "obj.steel_scimitar" count 1
        2 weight "obj.steel_axe" count 1
        2 weight "obj.iron_arrow" count 24
        3 weight "obj.airrune" count 30
        3 weight "obj.waterrune" count 18
        3 weight "obj.chaosrune" count 8
        1 weight "obj.cosmicrune" count 4
        2 weight "obj.lawrune" count 4
        8 weight "obj.coins" count 2
        25 weight "obj.coins" count 10
        24 weight "obj.coins" count 20
        8 weight "obj.coins" count 50
        4 weight "obj.coins" count 90
        1 weight "obj.coins" count 120
        5 weight "obj.iron_bar" count 1

        13 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Skeleton Champion hasn't been previously killed in the Champions' Challenge.
             true
        }
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
