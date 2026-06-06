package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val experimentNo2DropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Experiment No.2 Drops",
    npcs = npcs("npc.grim_experiment"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.grim_turnip" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Experiment No.2 Drops")
        10 weight "obj.earthrune" count 1..20
        10 weight "obj.waterrune" count 1..14
        7 weight "obj.chaosrune" count 1..15
        3 weight "obj.naturerune" count 1..5
        27 weight "obj.unidentified_tarromin" count 1..3
        18 weight "obj.coins" count 1..75
        3 weight "obj.coins" count 76..100
        1 weight "obj.mole_claw" count 1
        1 weight "obj.mystic_earth_staff" count 1

        42 weight SharedDropTables.herb
        6 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_experiment_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
