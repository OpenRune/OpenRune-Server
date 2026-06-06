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
public val scorpiasGuardianDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scorpia's guardian Drops",
    npcs = npcs("npc.scorpia_guardian"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 25 weight "obj.arceuus_corpse_scorpion" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Clue scroll (beginner) [tertiary/Unknown]
