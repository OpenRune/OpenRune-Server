package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val knightOfVarlamoreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Knight of Varlamore Drops",
    npcs = npcs("npc.varlamore_knight_f_1", "npc.varlamore_knight_f_2", "npc.varlamore_knight_f_3", "npc.varlamore_knight_m_1", "npc.varlamore_knight_m_2", "npc.varlamore_knight_m_3"),
    mainTable = rsPlayerWeightedTable(total = 1000) {
        name("Knight of Varlamore Drops")
        190 weight "obj.coins" count 1
        190 weight "obj.coins" count 6
        205 weight "obj.coins" count 12
        140 weight "obj.coins" count 25
        145 weight "obj.coins" count 30
        80 weight "obj.coins" count 55

        50 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
