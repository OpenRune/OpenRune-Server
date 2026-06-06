package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lizardmanShamanChambersOfXericDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lizardman shaman (Chambers of Xeric) Drops",
    npcs = npcs("npc.raids_lizardshaman_a", "npc.raids_lizardshaman_b"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raids_seed_buchuleaf" count 5..10
        "obj.raids_seed_golpar" count 5..10
        "obj.raids_seed_noxifer" count 5..10
    },
    mainTable = rsPlayerWeightedTable(total = 400) {
        name("Lizardman shaman (Chambers of Xeric) Drops")
        1 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 separate "obj.dorgesh_construction_bone_curved" count 1
        399 weight nothing()
    },
)
