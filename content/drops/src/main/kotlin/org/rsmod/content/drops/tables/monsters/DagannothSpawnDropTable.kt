package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val dagannothSpawnDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth spawn Drops",
    npcs = npcs("npc.dagganoth_critter"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dagannoth spawn Drops")
        10 weight "obj.waterrune" count 3
        10 weight "obj.raw_tuna" count 1
        5 weight "obj.raw_herring" count 1
        10 weight "obj.raw_sardine" count 1
        10 weight "obj.coins" count 16
        10 weight "obj.coins" count 25
        10 weight "obj.seaweed" count 1
        38 weight "obj.feather" count 2
        10 weight "obj.fishing_bait" count 3
        4 weight "obj.smalloysterpearls" count 1
        10 weight "obj.water_talisman" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_dagganoth_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
