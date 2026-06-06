package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val chickenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chicken Drops",
    npcs = npcs("npc.chicken", "npc.chicken_brown", "npc.farm_chicken_brown", "npc.farm_chicken_brown_indoors", "npc.farm_chicken_brown_outdoors", "npc.farm_chicken_dark_brown", "npc.farm_chicken_dark_brown_outdoors", "npc.farm_chicken_tan", "npc.farm_chicken_tan_outdoors", "npc.misc_chicken", "npc.misc_chicken_brown", "npc.tut2_chicken"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chicken" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Chicken Drops")
        64 weight "obj.feather" count 5
        32 weight "obj.feather" count 15
        32 weight ringNothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: Keys are only dropped when completing a medium clue scroll asking you to kill a Chicken.
             true
        }
        1 outOf 300 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_beginner")
        }
    },
)
