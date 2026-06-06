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
public val penanceRangerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Penance Ranger Drops",
    npcs = npcs("npc.barbassault_pen_ranger_lv1", "npc.barbassault_pen_ranger_lv2", "npc.barbassault_pen_ranger_lv3", "npc.barbassault_pen_ranger_lv4", "npc.barbassault_pen_ranger_lv5", "npc.barbassault_pen_ranger_lv6", "npc.barbassault_pen_ranger_lv7", "npc.barbassault_pen_ranger_lv8", "npc.barbassault_pen_ranger_lv9", "npc.barbassault_pen_ranger_tutor"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.barbassault_egg_03" count 2
        "obj.barbassault_egg_01" count 2
        "obj.barbassault_egg_02" count 2
    },
)
