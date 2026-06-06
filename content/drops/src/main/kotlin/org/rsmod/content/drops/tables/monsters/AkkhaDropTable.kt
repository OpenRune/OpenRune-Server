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
public val akkhaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Akkha Drops",
    npcs = npcs("npc.akkha_enrage", "npc.akkha_enrage_dummy", "npc.akkha_enrage_initial", "npc.akkha_enrage_spawn", "npc.akkha_mage", "npc.akkha_melee", "npc.akkha_range", "npc.akkha_spawn"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.toa_book_akkha" count 1 condition { player ->
            // Drops Need Manual: Only received if players do not have it stored in their bank.
             true
        }
        "obj.toa_akkha_ashes" count 1 condition { player ->
            // Drops Need Manual: Only received if players dealt the most damage to Akkha.
             true
        }
    },
)
