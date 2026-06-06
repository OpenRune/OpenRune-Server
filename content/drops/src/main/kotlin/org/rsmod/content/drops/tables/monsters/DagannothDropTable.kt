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
public val dagannothDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth Drops",
    npcs = npcs("npc.horror_dagannoth_medium", "npc.horror_dagannoth_medium_darker", "npc.horror_dagannoth_medium_lighter", "npc.horror_dagganoth_jr", "npc.horror_dagganoth_jr_darker", "npc.horror_dagganoth_jr_lighter", "npc.kourend_dagannoth1", "npc.kourend_dagannoth2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dagannoth Drops")
        6 weight "obj.iron_spear" count 1
        5 weight "obj.bronze_spear" count 1
        1 weight "obj.mithril_spear" count 1
        4 weight "obj.waterrune" count 15
        2 weight "obj.steel_arrow" count 15
        1 weight "obj.mithril_javelin" count 3
        12 weight "obj.lobster_pot" count 1
        4 weight "obj.raw_herring" count 3
        4 weight "obj.raw_sardine" count 5
        3 weight "obj.harpoon" count 1
        2 weight "obj.feather" count 15
        2 weight "obj.fishing_bait" count 50
        2 weight "obj.raw_lobster" count 1
        2 weight "obj.raw_tuna" count 1
        2 weight "obj.seaweed" count 10
        1 weight "obj.bigoysterpearls" count 1
        1 weight "obj.smalloysterpearls" count 2
        29 weight "obj.coins" count 56
        9 weight "obj.coins" count 25
        8 weight "obj.coins" count 44
        6 weight "obj.coins" count 41
        1 weight "obj.casket" count 1
        2 weight "obj.opal_bolttips" count 12

        1 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_dagganoth_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 40 weight "obj.arceuus_corpse_dagannoth" count 1
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
