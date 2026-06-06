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
public val nylocasVasiliasDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Nylocas Vasilias Drops",
    npcs = npcs("npc.nylocas_boss_magic", "npc.nylocas_boss_magic_hard", "npc.nylocas_boss_magic_story", "npc.nylocas_boss_melee", "npc.nylocas_boss_melee_hard", "npc.nylocas_boss_melee_story", "npc.nylocas_boss_ranged", "npc.nylocas_boss_ranged_hard", "npc.nylocas_boss_ranged_story", "npc.nylocas_boss_spawning", "npc.nylocas_boss_spawning_hard", "npc.nylocas_boss_spawning_story"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.tob_book_nylocas" count 1 condition { player ->
            // Drops Need Manual: No longer dropped after reading the book.
             true
        }
    },
)
