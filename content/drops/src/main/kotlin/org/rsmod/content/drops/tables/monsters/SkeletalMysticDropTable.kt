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
public val skeletalMysticDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeletal Mystic Drops",
    npcs = npcs("npc.raids_skeletonmystic_a", "npc.raids_skeletonmystic_b", "npc.raids_skeletonmystic_c"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raids_seed_golpar" count 5..10
        "obj.raids_seed_buchuleaf" count 5..10
        "obj.raids_seed_noxifer" count 5..10
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
