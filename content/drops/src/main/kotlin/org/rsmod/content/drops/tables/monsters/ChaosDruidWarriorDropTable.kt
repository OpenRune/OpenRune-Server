package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val chaosDruidWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos druid warrior Drops",
    npcs = npcs("npc.chaos_druid_warrior"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Chaos druid warrior Drops")
        1 weight "obj.black_dagger" count 1
        5 weight "obj.firerune" count 12
        4 weight "obj.lawrune" count 2
        2 weight "obj.earthrune" count 9
        1 weight "obj.airrune" count 36
        1 weight "obj.naturerune" count 3
        5 weight "obj.white_berries" count 1
        2 weight "obj.unicorn_horn_dust" count 1
        1 weight "obj.limpwurt_root" count 1
        1 weight "obj.limpwurt_root" count 2
        1 weight "obj.snape_grass" count 1
        1 weight "obj.vial_water" count 1
        15 weight "obj.coins" count 3
        3 weight "obj.coins" count 29
        1 weight "obj.coins" count 10
        37 weight ringNothing()
        1 weight "obj.limpwurt_seed" count 1
        1 weight "obj.1dose2defense" count 1

        44 weight rsWeightedTable(total = 44) {
            34 weight herbDropTable
            10 weight doubleRollHerbDropTable
        }
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 25 weight "obj.arceuus_corpse_chaosdruid" count 1
    },
)
