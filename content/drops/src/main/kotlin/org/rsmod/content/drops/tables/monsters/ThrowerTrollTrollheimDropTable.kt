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
public val throwerTrollTrollheimDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Thrower troll (Trollheim) Drops",
    npcs = npcs("npc.troll_thrower1", "npc.troll_thrower2", "npc.troll_thrower3", "npc.troll_thrower4", "npc.troll_thrower5"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
