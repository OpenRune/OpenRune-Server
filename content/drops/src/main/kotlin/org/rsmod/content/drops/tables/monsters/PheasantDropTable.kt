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
public val pheasantDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Pheasant Drops",
    npcs = npcs("npc.macro_pheasant_model_1", "npc.macro_pheasant_model_2", "npc.macro_pheasant_model_3", "npc.macro_pheasant_model_4"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_macro_pheasant" count 1
    },
)
