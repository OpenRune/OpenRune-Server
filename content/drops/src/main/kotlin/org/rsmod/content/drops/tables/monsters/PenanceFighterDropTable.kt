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
public val penanceFighterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Penance Fighter Drops",
    npcs = npcs("npc.barbassault_pen_fighter_lv1", "npc.barbassault_pen_fighter_lv2", "npc.barbassault_pen_fighter_lv3", "npc.barbassault_pen_fighter_lv4", "npc.barbassault_pen_fighter_lv5", "npc.barbassault_pen_fighter_lv6", "npc.barbassault_pen_fighter_lv7", "npc.barbassault_pen_fighter_lv8", "npc.barbassault_pen_fighter_lv9", "npc.barbassault_pen_fighter_tutor"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.barbassault_egg_03" count 2
        "obj.barbassault_egg_01" count 2
        "obj.barbassault_egg_02" count 2
    },
)
