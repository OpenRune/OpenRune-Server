package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scorpionDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scorpion Drops",
    npcs = npcs("npc.mm_jungle_scorpion", "npc.scorpion", "npc.sos_pest_scorpion", "npc.sos_pest_scorpion2", "npc.tinyscorpion", "npc.varlamore_scorpion_savannah"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dwarf_rock_page1" count 1 condition {
            player -> player.isOnQuest("quest_betweenarock")
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 25 weight "obj.arceuus_corpse_scorpion" count 1
        1 outOf 100 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
