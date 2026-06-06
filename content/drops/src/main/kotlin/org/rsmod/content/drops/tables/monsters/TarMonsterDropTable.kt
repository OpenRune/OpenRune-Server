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
public val tarMonsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tar Monster Drops",
    npcs = npcs("npc.fossil_tarmonster"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.swamp_tar" count 10
    },
)
