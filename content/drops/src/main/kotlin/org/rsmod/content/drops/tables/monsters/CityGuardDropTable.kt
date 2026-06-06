package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val cityGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "City guard Drops",
    npcs = npcs("npc.city_guard"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("City guard Drops")
        109 weight ringNothing()
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        19 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 30 weight "obj.arceuus_corpse_ogre" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
