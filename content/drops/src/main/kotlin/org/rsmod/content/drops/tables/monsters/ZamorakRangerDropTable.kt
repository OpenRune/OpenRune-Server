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
public val zamorakRangerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zamorak ranger Drops",
    npcs = npcs("npc.rc_zmi_ranger", "npc.rc_zmi_ranger2"),
    mainTable = rsPlayerWeightedTable(total = 50) {
        name("Zamorak ranger Drops")
        2 weight "obj.willow_shortbow" count 1
        1 weight "obj.willow_longbow" count 1
        2 weight "obj.steel_arrow" count 10
        2 weight "obj.mithril_arrow" count 10
        1 weight "obj.mithril_arrow" count 20
        40 weight "obj.coins" count 100
        2 weight "obj.cert_bow_string" count 10
    },
)
