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
public val emissaryEnforcerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Emissary Enforcer Drops",
    npcs = npcs("npc.vmq4_temple_guard_boss_fight"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.vmq4_cult_manifest" count 1
    },
)
