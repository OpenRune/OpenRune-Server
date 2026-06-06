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
public val skeletonFremennikDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton fremennik Drops",
    npcs = npcs("npc.olaf2_undead_viking_lvl40", "npc.olaf2_undead_viking_lvl40_b", "npc.olaf2_undead_viking_lvl40_c", "npc.olaf2_undead_viking_lvl50", "npc.olaf2_undead_viking_lvl50_b", "npc.olaf2_undead_viking_lvl50_c", "npc.olaf2_undead_viking_lvl60", "npc.olaf2_undead_viking_lvl60_b", "npc.olaf2_undead_viking_lvl60_c"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.olaf2_gate_key_1" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeleton fremennik Drops")
        2 weight "obj.black_axe" count 1
        6 weight "obj.steel_med_helm" count 1
        4 weight "obj.steel_sword" count 1
        1 weight "obj.mithril_scimitar" count 1
        3 weight "obj.airrune" count 60
        3 weight "obj.chaosrune" count 9
        2 weight "obj.lawrune" count 2
        2 weight "obj.mithril_arrow" count 8
        3 weight "obj.waterrune" count 20
        1 weight "obj.cosmicrune" count 4
        23 weight "obj.coins" count 20
        8 weight "obj.coins" count 50
        25 weight "obj.coins" count 80
        4 weight "obj.coins" count 90
        3 weight "obj.coins" count 185
        2 weight "obj.coins" count 200
        5 weight "obj.mithril_bar" count 1

        2 weight SharedDropTables.gem
        29 weight nothing()
    },
)
