package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val wizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wizard Drops",
    npcs = npcs("npc.wizard"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Wizard Drops")
        8 weight "obj.plainstaff" count 1
        7 weight "obj.wizards_robe" count 1
        3 weight "obj.bluewizhat" count 1
        8 weight "obj.chaosrune" count 2
        8 weight "obj.naturerune" count 2
        3 weight "obj.airrune" count 5
        3 weight "obj.bodyrune" count 5
        3 weight "obj.earthrune" count 5
        3 weight "obj.firerune" count 5
        3 weight "obj.mindrune" count 5
        3 weight "obj.waterrune" count 5
        2 weight "obj.airrune" count 12
        2 weight "obj.bodyrune" count 12
        2 weight "obj.earthrune" count 12
        2 weight "obj.firerune" count 12
        2 weight "obj.mindrune" count 12
        2 weight "obj.waterrune" count 12
        1 weight "obj.bloodrune" count 2
        1 weight "obj.lawrune" count 2
        3 weight "obj.water_talisman" count 1
        4 weight "obj.mind_talisman" count 1
        23 weight "obj.coins" count 1
        9 weight "obj.coins" count 2
        7 weight "obj.coins" count 18
        1 weight "obj.coins" count 30
        16 weight ringNothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing a medium clue scroll asking the player to kill a Wizard.
             true
        }
    },
)
