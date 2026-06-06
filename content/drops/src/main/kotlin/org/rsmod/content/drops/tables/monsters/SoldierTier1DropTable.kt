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
public val soldierTier1DropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Soldier (tier 1) Drops",
    npcs = npcs("npc.shayzien_armour_1_combat", "npc.shayzien_armour_1_noncombat"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.shayzien_helm_1" count 1
        "obj.shayzien_body_1" count 1
        "obj.shayzien_legs_1" count 1
        "obj.shayzien_gloves_1" count 1
        "obj.shayzien_boots_1" count 1
    },
)
