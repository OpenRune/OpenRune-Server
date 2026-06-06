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
public val guardVarlamoreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard (Varlamore) Drops",
    npcs = npcs("npc.varlamore_guard_f_1", "npc.varlamore_guard_f_2", "npc.varlamore_guard_f_3", "npc.varlamore_guard_f_4", "npc.varlamore_guard_f_5", "npc.varlamore_guard_m_1", "npc.varlamore_guard_m_2", "npc.varlamore_guard_m_3", "npc.varlamore_guard_m_4", "npc.varlamore_guard_m_5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Guard (Varlamore) Drops")
        10 weight "obj.xbows_crossbow_bolts_iron" count 2..12
        4 weight "obj.steel_arrow" count 1
        3 weight "obj.bronze_arrow" count 1
        2 weight "obj.airrune" count 6
        2 weight "obj.earthrune" count 3
        2 weight "obj.firerune" count 2
        2 weight "obj.bronze_arrow" count 2
        1 weight "obj.chaosrune" count 1
        1 weight "obj.naturerune" count 1
        1 weight "obj.bloodrune" count 1
        1 weight "obj.steel_arrow" count 5
        19 weight "obj.coins" count 1
        16 weight "obj.coins" count 7
        9 weight "obj.coins" count 12
        8 weight "obj.coins" count 4
        4 weight "obj.coins" count 25
        4 weight "obj.coins" count 17
        2 weight "obj.coins" count 30
        10 weight ringNothing()
        6 weight "obj.iron_dagger" count 1
        1 weight "obj.body_talisman" count 1
        1 weight "obj.grain" count 1
        1 weight "obj.iron_ore" count 1

        18 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
