package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scavengerBeastDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scavenger beast Drops",
    npcs = npcs("npc.raids_scavenger_beast_a", "npc.raids_scavenger_beast_b"),
    mainTable = rsPlayerWeightedTable(total = 18) {
        name("Scavenger beast Drops")
        1 weight "obj.fishing_rod" count 1
        1 weight "obj.iron_pickaxe" count 1
        1 weight "obj.iron_axe" count 1
        1 weight "obj.hunting_butterfly_net" count 1
        1 weight "obj.hammer" count 1
        1 weight "obj.tinderbox" count 1
        2 weight "obj.lockpick" count 1
        2 weight "obj.raids_fishingbait" count 30..50
        2 weight dropRollable(DropRollItem("obj.raids_endarkened_juice", 5..14, bonusDrops = listOf(
            DropRollItem("obj.raids_stinkhorn_mushroom", 1),
        )))
        2 weight "obj.raids_cicely" count 3..6
        2 weight "obj.raids_plank" count 2
        2 weight nothing()
    },
)
