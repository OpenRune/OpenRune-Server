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
public val zamorakWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zamorak warrior Drops",
    npcs = npcs("npc.rc_zmi_melee", "npc.rc_zmi_melee2"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Zamorak warrior Drops")
        2 weight "obj.rune_dagger" count 1
        1 weight "obj.adamant_longsword" count 1
        2 weight "obj.steel_scimitar" count 1
        2 weight "obj.steel_dagger" count 1
        1 weight "obj.rune_scimitar" count 1
        40 weight "obj.coins" count 100
        2 weight "obj.lobster" count 3
    },
)
