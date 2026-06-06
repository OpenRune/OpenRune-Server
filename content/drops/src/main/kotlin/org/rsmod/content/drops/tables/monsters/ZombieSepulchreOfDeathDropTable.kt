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
public val zombieSepulchreOfDeathDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zombie (Sepulchre of Death) Drops",
    npcs = npcs("npc.macro_zombie"),
    mainTable = rsPlayerWeightedTable(total = 268) {
        name("Zombie (Sepulchre of Death) Drops")
        1 weight "obj.bones" count 10
        1 weight "obj.big_bones" count 10
        1 weight "obj.dragon_bones" count 2
        5 weight "obj.big_bones" count 1
        5 weight "obj.dragon_bones" count 1
        1 weight "obj.dragon_bones" count 3
        51 weight "obj.big_bones" count 1
        20 weight "obj.big_bones" count 2
        15 weight "obj.big_bones" count 6
        5 weight "obj.bones" count 3
        20 weight "obj.airrune" count 40
        20 weight "obj.cosmicrune" count 5
        20 weight "obj.chaosrune" count 35
        15 weight "obj.chaosrune" count 50
        24 weight "obj.fishing_bait" count 27
        40 weight "obj.fishing_bait" count 50
        10 weight "obj.coins" count 26

        14 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
