package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val soldierBurthorpeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Soldier (Burthorpe) Drops",
    npcs = npcs("npc.death_ig_solider_drilling", "npc.death_ig_solider_sitting1", "npc.death_ig_solider_sitting2", "npc.death_ig_solider_sitting3", "npc.death_ig_solider_training"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Soldier (Burthorpe) Drops")
        1 weight "obj.steel_full_helm" count 1
        9 weight "obj.black_dart" count 6
        13 weight "obj.black_knife" count 6
        2 weight "obj.steel_claws" count 1
        1 weight "obj.black_claws" count 1
        1 weight "obj.steel_bar" count 1
        1 weight "obj.mithril_bar" count 1
        17 weight "obj.coins" count 3
        10 weight "obj.coins" count 5
        20 weight "obj.coins" count 18
        40 weight "obj.coins" count 50
        2 weight "obj.coins" count 115
        1 weight "obj.bloodrune" count 2

        8 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
)
