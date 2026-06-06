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
public val dagannothMotherDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth mother Drops",
    npcs = npcs("npc.horror_dagganoth_air", "npc.horror_dagganoth_aira", "npc.horror_dagganoth_airb", "npc.horror_dagganoth_airc", "npc.horror_dagganoth_earth", "npc.horror_dagganoth_fire", "npc.horror_dagganoth_melee", "npc.horror_dagganoth_ranged", "npc.horror_dagganoth_water"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.horror_casket" count 1
    },
)
