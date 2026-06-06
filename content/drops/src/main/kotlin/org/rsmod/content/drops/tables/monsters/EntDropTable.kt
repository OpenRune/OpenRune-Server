package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val entDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ent Drops",
    npcs = npcs("npc.wcguild_ent"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 236 weight "obj.slayer_enchantment" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Logs [main/Varies]
//   - Oak logs [main/Varies]
//   - Willow logs [main/Varies]
//   - Maple logs [main/Varies]
//   - Yew logs [main/Varies]
//   - Magic logs [main/Varies]
