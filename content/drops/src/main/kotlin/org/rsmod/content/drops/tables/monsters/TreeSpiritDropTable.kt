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
public val treeSpiritDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tree spirit Drops",
    npcs = npcs("npc.fairy2_dryhadguardian_1", "npc.fairy2_dryhadguardian_2", "npc.fairy2_dryhadguardian_3", "npc.fairy2_dryhadguardian_4", "npc.fairy2_dryhadguardian_5", "npc.fairy2_dryhadguardian_6"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Tree spirit Drops")
        10 weight "obj.steel_axe" count 1
        20 weight "obj.mithril_axe" count 1
        8 weight "obj.adamant_axe" count 1
        4 weight "obj.rune_axe" count 1
        31 weight "obj.naturerune" count 22
        2 weight "obj.naturerune" count 10
        2 weight "obj.naturerune" count 5
        1 weight "obj.naturerune" count 2
        6 weight "obj.unidentified_snapdragon" count 1
        6 weight "obj.unidentified_toadflax" count 1
        1 weight "obj.knife" count 1

        15 weight SharedDropTables.herb
        8 weight SharedDropTables.gem
        14 weight SharedDropTables.rareSeed
    },
)
