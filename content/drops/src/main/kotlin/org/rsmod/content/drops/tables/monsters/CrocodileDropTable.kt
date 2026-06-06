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
public val crocodileDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Crocodile Drops",
    npcs = npcs("npc.ics_little_croc", "npc.ics_little_croc_blocked", "npc.ics_little_croc_normal"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Crocodile Drops")

        1 weight SharedDropTables.herb
        19 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls and reward caskets are only dropped when completing an elite clue scroll asking you to kill a crocodile.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
    },
)
