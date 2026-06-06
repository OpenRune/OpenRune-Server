package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val infernalMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Infernal Mage Drops",
    npcs = npcs("npc.slayer_infernal_mage_1", "npc.slayer_infernal_mage_2", "npc.slayer_infernal_mage_3", "npc.slayer_infernal_mage_4", "npc.slayer_infernal_mage_5"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 512 weight "obj.mystic_boots_dark" count 1
        1 outOf 512 weight "obj.mystic_hat_dark" count 1
        1 outOf 1000 weight "obj.lava_battlestaff" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Infernal Mage Drops")
        8 weight "obj.plainstaff" count 1
        1 weight "obj.staff_of_fire" count 1
        6 weight "obj.earthrune" count 10
        6 weight "obj.firerune" count 10
        4 weight "obj.earthrune" count 36
        3 weight "obj.airrune" count 10
        3 weight "obj.waterrune" count 10
        2 weight "obj.airrune" count 18
        2 weight "obj.waterrune" count 18
        2 weight "obj.earthrune" count 18
        2 weight "obj.firerune" count 18
        2 weight "obj.mindrune" count 18
        2 weight "obj.bodyrune" count 18
        2 weight "obj.bloodrune" count 4
        18 weight "obj.deathrune" count 7
        21 weight "obj.coins" count 1
        16 weight "obj.coins" count 2
        9 weight "obj.coins" count 4
        3 weight "obj.coins" count 29
        16 weight ringNothing()
    },
)
