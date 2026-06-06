package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val maniacalMonkeyArcherDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Maniacal Monkey Archer Drops",
    npcs = npcs("npc.mm2_monkey_archer_maze"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Maniacal Monkey Archer Drops")
        2 weight "obj.steel_scimitar" count 1
        1 weight "obj.maple_shortbow" count 1
        3 weight "obj.plank_oak" count 1
        2 weight "obj.unidentified_guam" count 1
        2 weight "obj.banana" count 1
        1 weight "obj.adamant_arrow_p++" count 5
        2 weight "obj.2doseantipoison" count 1
        4 weight "obj.bass" count 2
        2 weight "obj.1doseprayerrestore" count 1
        1 weight "obj.rope" count 1
    },
)
