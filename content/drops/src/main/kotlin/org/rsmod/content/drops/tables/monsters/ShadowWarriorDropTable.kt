package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val shadowWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Shadow warrior Drops",
    npcs = npcs("npc.shadow_warrior"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Shadow warrior Drops")
        1 weight "obj.adamant_spear" count 1
        1 weight "obj.black_dagger_p" count 1
        1 weight "obj.black_knife" count 1
        1 weight "obj.black_longsword" count 1
        1 weight "obj.black_robe" count 1
        9 weight "obj.cosmicrune" count 3
        6 weight "obj.bloodrune" count 2
        4 weight "obj.airrune" count 45
        4 weight "obj.deathrune" count 2
        47 weight "obj.coins" count 8
        22 weight ringNothing()
        4 weight "obj.mithril_bar" count 1
        1 weight "obj.weapon_poison" count 1

        18 weight SharedDropTables.herb
        8 weight SharedDropTables.gem
    },
)
