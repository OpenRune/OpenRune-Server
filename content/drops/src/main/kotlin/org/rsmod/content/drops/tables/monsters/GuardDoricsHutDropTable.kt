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
public val guardDoricsHutDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard (Doric's hut) Drops",
    npcs = npcs("npc.falador_doric_area_guard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Guard (Doric's hut) Drops")
        10 weight "obj.xbows_crossbow_bolts_iron" count 2..12
        4 weight "obj.steel_arrow" count 1
        3 weight "obj.bronze_arrow" count 1
        2 weight "obj.airrune" count 6
        2 weight "obj.earthrune" count 3
        2 weight "obj.firerune" count 2
        1 weight "obj.bloodrune" count 1
        1 weight "obj.chaosrune" count 1
        1 weight "obj.naturerune" count 1
        1 weight "obj.steel_arrow" count 5
        2 weight "obj.bronze_arrow" count 2
        19 weight "obj.coins" count 1
        16 weight "obj.coins" count 7
        9 weight "obj.coins" count 12
        8 weight "obj.coins" count 4
        4 weight "obj.coins" count 25
        4 weight "obj.coins" count 17
        2 weight "obj.coins" count 30
        8 weight ringNothing()
        6 weight "obj.iron_dagger" count 1
        3 weight "obj.body_talisman" count 1
        1 weight "obj.grain" count 1
        1 weight "obj.iron_ore" count 1

        18 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing a medium clue scroll asking the player to kill an Ardougne knight.
             true
        }
    },
)
