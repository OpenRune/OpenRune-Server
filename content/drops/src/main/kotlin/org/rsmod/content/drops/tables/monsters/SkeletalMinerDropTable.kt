package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skeletalMinerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeletal miner Drops",
    npcs = npcs("npc.skeletal_miner"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeletal miner Drops")
        6 weight "obj.iron_med_helm" count 1
        4 weight "obj.iron_sword" count 1
        2 weight "obj.iron_axe" count 1
        1 weight "obj.iron_scimitar" count 1
        3 weight "obj.airrune" count 15
        3 weight "obj.waterrune" count 9
        3 weight "obj.chaosrune" count 5
        2 weight "obj.lawrune" count 2
        2 weight "obj.iron_arrow" count 12
        1 weight "obj.cosmicrune" count 2
        25 weight "obj.coins" count 5
        24 weight "obj.coins" count 10
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
        1 outOf 100 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
