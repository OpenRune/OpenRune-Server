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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val mogreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mogre Drops",
    npcs = npcs("npc.mudskipper_ogre"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Mogre Drops")
        4 weight "obj.waterrune" count 5
        4 weight "obj.waterrune" count 7
        4 weight "obj.waterrune" count 14
        20 weight "obj.raw_swordfish" count 1
        9 weight "obj.raw_tuna" count 1
        7 weight "obj.raw_pike" count 1
        4 weight "obj.raw_salmon" count 1
        3 weight "obj.raw_herring" count 1
        3 weight "obj.raw_sardine" count 1
        3 weight "obj.raw_shark" count 1
        30 weight "obj.fishing_bait" count 5
        10 weight "obj.fishing_bait" count 15
        5 weight "obj.mudskipper_hat" count 1
        3 weight "obj.oystershell" count 1
        2 weight "obj.mudskipper_flippers" count 1
        2 weight "obj.seaweed" count 1
        1 weight "obj.staff_of_water" count 1
        1 weight "obj.fishbowl_water" count 1
        13 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_mogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
