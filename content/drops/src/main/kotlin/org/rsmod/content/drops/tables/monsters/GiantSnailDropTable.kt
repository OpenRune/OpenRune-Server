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
public val giantSnailDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant snail Drops",
    npcs = npcs("npc.templetrek_giantsnail_1", "npc.templetrek_giantsnail_2", "npc.templetrek_giantsnail_3"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Giant snail Drops")
        49 weight "obj.templetrek_snail_shell" count 1
        1 weight "obj.dorgesh_snail_shell" count 1
    },
)
