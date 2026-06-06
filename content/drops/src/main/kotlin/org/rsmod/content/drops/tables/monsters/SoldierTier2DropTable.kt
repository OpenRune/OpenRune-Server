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
public val soldierTier2DropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Soldier (tier 2) Drops",
    npcs = npcs("npc.shayzien_armour_2_combat", "npc.shayzien_armour_2_noncombat"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.shayzien_helm_2" count 1
        "obj.shayzien_body_2" count 1
        "obj.shayzien_legs_2" count 1
        "obj.shayzien_gloves_2" count 1
        "obj.shayzien_boots_2" count 1
    },
)
