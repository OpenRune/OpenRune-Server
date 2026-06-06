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
public val catableponDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Catablepon Drops",
    npcs = npcs("npc.sos_pest_catablepon", "npc.sos_pest_catablepon2", "npc.sos_pest_catablepon3"),
    mainTable = rsPlayerWeightedTable(total = 101) {
        name("Catablepon Drops")
        1 weight "obj.adamant_med_helm" count 1
        7 weight "obj.firerune" count 15
        6 weight "obj.waterrune" count 7
        1 weight "obj.chaosrune" count 7
        4 weight "obj.lawrune" count 2
        3 weight "obj.cosmicrune" count 2
        4 weight "obj.mithril_arrow" count 5..14
        5 weight "obj.cert_blankrune_high" count 15
        7 weight "obj.eye_of_newt" count 1
        2 weight "obj.cert_coal" count 3..7
        6 weight "obj.coins" count 15
        12 weight "obj.coins" count 44
        10 weight "obj.coins" count 5..104
        2 weight "obj.trout" count 1
        9 weight "obj.torch_unlit" count 1
        3 weight "obj.sos_half_sceptre1" count 1

        3 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        14 weight nothing()
    },
)
