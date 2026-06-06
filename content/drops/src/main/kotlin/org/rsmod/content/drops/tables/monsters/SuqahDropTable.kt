package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val suqahDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Suqah Drops",
    npcs = npcs("npc.lunar_suqka", "npc.lunar_suqka2", "npc.lunar_suqka3", "npc.lunar_suqka4", "npc.lunar_suqka5", "npc.lunar_suqka6", "npc.lunar_suqka7"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.suqka_hide_untanned" count 1
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 2 weight "obj.lunar_tiara" count 1 condition {
            player -> player.isOnQuest("quest_lunardiplomacyafteraskingmeteoraaboutherheadgear")
        }
    },
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Suqah Drops")
        69 weight "obj.suqka_tooth" count 1
        30 weight "obj.unidentified_guam" count 1
        25 weight "obj.unidentified_marentill" count 1

        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/122 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 129 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
