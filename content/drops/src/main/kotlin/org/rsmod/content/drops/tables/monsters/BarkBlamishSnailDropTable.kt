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
public val barkBlamishSnailDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bark Blamish Snail Drops",
    npcs = npcs("npc.mmsnailround_orange"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.shellround_orange" count 1
        "obj.snail_corpse2" count 1
    },
)
