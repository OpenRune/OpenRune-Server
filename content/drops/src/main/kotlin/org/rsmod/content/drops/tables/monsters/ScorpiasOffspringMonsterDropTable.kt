package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scorpiasOffspringMonsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scorpia's offspring (monster) Drops",
    npcs = npcs("npc.scorpia_minion"),
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
