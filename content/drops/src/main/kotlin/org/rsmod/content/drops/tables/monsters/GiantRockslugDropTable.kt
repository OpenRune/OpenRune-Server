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
public val giantRockslugDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant rockslug Drops",
    npcs = npcs("npc.superior_rockslug"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Giant rockslug Drops")
        30 weight "obj.earthrune" count 5
        4 weight "obj.earthrune" count 42
        4 weight "obj.chaosrune" count 2
        13 weight "obj.coal" count 1
        22 weight "obj.iron_ore" count 1
        2 weight "obj.bronze_bar" count 1
        3 weight "obj.iron_bar" count 1
        3 weight "obj.copper_ore" count 1
        8 weight "obj.tin_ore" count 1
        1 weight "obj.mithril_ore" count 1
        13 weight "obj.dwarven_stout" count 1
        10 weight "obj.hammer" count 1
        1 outOf 512 separate "obj.mystic_gloves_light" count 1

        6 weight SharedDropTables.gem
        9 weight SharedDropTables.seed
    },
)
