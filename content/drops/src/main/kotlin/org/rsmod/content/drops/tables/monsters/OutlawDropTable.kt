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
public val outlawDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Outlaw Drops",
    npcs = npcs("npc.surok_outlaw1", "npc.surok_outlaw10", "npc.surok_outlaw2", "npc.surok_outlaw3", "npc.surok_outlaw4", "npc.surok_outlaw5", "npc.surok_outlaw6", "npc.surok_outlaw7", "npc.surok_outlaw8", "npc.surok_outlaw9"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.surok_paper" count 1 condition {
            player -> player.isOnQuest("quest_whatliesbelow")
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Outlaw Drops")
        2 weight "obj.bronze_med_helm" count 1
        2 weight "obj.mindrune" count 9
        2 weight "obj.waterrune" count 6
        2 weight "obj.earthrune" count 5
        12 weight "obj.coins" count 5
        4 weight "obj.coins" count 15
        1 weight "obj.coins" count 25
        46 weight "obj.rope" count 1
        15 weight "obj.fishing_bait" count 1
        2 weight "obj.copper_ore" count 1
        6 weight "obj.cabbage" count 1
        1 weight "obj.knife" count 1

        32 weight SharedDropTables.herb
        1 weight nothing()
    },
)
