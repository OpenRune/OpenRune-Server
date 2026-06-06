package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val soldierYanilleDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Soldier (Yanille) Drops",
    npcs = npcs("npc.yanille_soldier"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Soldier (Yanille) Drops")
        6 weight "obj.steel_longsword" count 1
        1 weight "obj.steel_med_helm" count 1
        5 weight "obj.bronze_arrow" count 4
        4 weight "obj.steel_arrow" count 2
        2 weight "obj.airrune" count 8
        2 weight "obj.earthrune" count 4
        2 weight "obj.firerune" count 6
        1 weight "obj.chaosrune" count 3
        1 weight "obj.bloodrune" count 2
        1 weight "obj.naturerune" count 2
        1 weight "obj.steel_arrow" count 8
        37 weight "obj.coins" count 9
        16 weight "obj.coins" count 8
        9 weight "obj.coins" count 24
        8 weight "obj.coins" count 6
        4 weight "obj.coins" count 30
        4 weight "obj.coins" count 12
        2 weight "obj.coins" count 35
        18 weight ringNothing()
        3 weight "obj.mind_talisman" count 1
        1 weight "obj.iron_ore" count 1
    },
)
