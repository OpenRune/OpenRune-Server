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
public val banditVarlamoreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bandit (Varlamore) Drops",
    npcs = npcs("npc.varlamore_bandit_f_1", "npc.varlamore_bandit_f_2", "npc.varlamore_bandit_f_3", "npc.varlamore_bandit_f_4", "npc.varlamore_bandit_m_1", "npc.varlamore_bandit_m_2", "npc.varlamore_bandit_m_3", "npc.varlamore_bandit_m_4"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bandit (Varlamore) Drops")
        9 weight "obj.steel_scimitar" count 1
        2 weight "obj.steel_longsword" count 1
        1 weight "obj.steel_pickaxe" count 1
        5 weight "obj.airrune" count 8
        5 weight "obj.waterrune" count 8
        5 weight "obj.chaosrune" count 6
        1 weight "obj.mindrune" count 2
        27 weight "obj.coins" count 10
        26 weight "obj.coins" count 35
        13 weight "obj.coins" count 1
        11 weight "obj.coins" count 50
        3 weight "obj.coins" count 100
        2 weight ringNothing()

        15 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
