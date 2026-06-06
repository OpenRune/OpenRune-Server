package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val banditBanditCampDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bandit (Bandit Camp) Drops",
    npcs = npcs("npc.fourdiamonds_sword_bandit_1", "npc.fourdiamonds_sword_bandit_free"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bandit (Bandit Camp) Drops")
        21 weight "obj.chaosrune" count 2
        3 weight "obj.deathrune" count 2
        2 weight "obj.naturerune" count 2
        2 weight "obj.cosmicrune" count 2
        2 weight "obj.lawrune" count 2
        2 weight "obj.iron_battleaxe" count 1
        1 weight "obj.steel_axe" count 1
        23 weight "obj.coins" count 12
        12 weight "obj.coins" count 20
        1 weight "obj.coins" count 30
        6 weight "obj.coins" count 45
        4 weight "obj.coal" count 1
        5 weight "obj.steel_bar" count 1

        14 weight SharedDropTables.herb
        30 weight nothing()
    },
)
