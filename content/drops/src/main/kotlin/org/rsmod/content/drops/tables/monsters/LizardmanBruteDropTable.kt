package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lizardmanBruteDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lizardman brute Drops",
    npcs = npcs("npc.akd_egg_lizardman", "npc.molch_lizardbrute_1", "npc.zeah_lizardman_3_vn", "npc.zeah_lizardman_3_vp"),
    mainTable = rsPlayerWeightedTable(total = 67) {
        name("Lizardman brute Drops")
        14 weight "obj.lizardman_fang" count 1
        8 weight "obj.xeric_fabric" count 1
        1 outOf 250 separate "obj.xeric_talisman_empty" count 1
        45 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
    },
)
