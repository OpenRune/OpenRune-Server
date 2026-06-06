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
public val lesserDemonTheScarDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lesser demon (The Scar) Drops",
    npcs = npcs("npc.dt2_scar_lesser_demon_1", "npc.dt2_scar_maze_3_link_chanter", "npc.dt2_scar_maze_mage_demon_normal", "npc.dt2_scar_maze_melee_demon_normal", "npc.dt2_scar_maze_ranged_demon_normal"),
    mainTable = rsPlayerWeightedTable(total = 5000) {
        name("Lesser demon (The Scar) Drops")
        1 weight "obj.champions_challenge_lesserdemon" count 1 condition { player ->
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Lesser Demon Champion hasn't been previously killed in the Champions' Challenge.
            // Drops Need Manual: Only confirmed to drop from the level 82 variant; there is insufficient data for the others.
            // Drops Need Manual: Only dropped if access to Champions' Guild is available, and the Lesser Demon Champion hasn't been previously killed in the Champions' Challenge. Only confirmed to drop from the level 82 variant; there is insufficient data for the others.
             true
        }
        4999 weight nothing()
    },
)
