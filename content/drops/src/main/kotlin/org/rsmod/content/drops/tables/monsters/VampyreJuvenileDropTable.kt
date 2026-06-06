package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vampyreJuvenileDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vampyre Juvenile Drops",
    npcs = npcs("npc.burgh_vampire_juvenile_1", "npc.burgh_vampire_juvenile_2", "npc.burgh_vampire_juvenile_3", "npc.burgh_vampire_juvenile_held", "npc.darkm_juvenile_01", "npc.darkm_juvenile_01_held", "npc.darkm_juvenile_02", "npc.darkm_juvenile_02_held", "npc.sang_myq3_female_juvenile", "npc.sang_myq3_female_juvenile_held", "npc.sang_myq3_male_juvenile", "npc.sang_myq3_male_juvenile_held"),
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
    },
)
