package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val kalphiteSoldierDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kalphite Soldier Drops",
    npcs = npcs("npc.kalphite_soldier", "npc.kalphite_soldier_strongholdcave"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Kalphite Soldier Drops")
        4 weight "obj.steel_full_helm" count 1
        3 weight "obj.steel_scimitar" count 1
        4 weight "obj.steel_axe" count 1
        1 weight "obj.mithril_chainbody" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.adamant_med_helm" count 1
        1 weight "obj.firerune" count 30
        8 weight "obj.firerune" count 60
        5 weight "obj.chaosrune" count 12
        3 weight "obj.deathrune" count 3
        2 weight "obj.naturerune" count 1..4
        7 weight "obj.coins" count 10
        29 weight "obj.coins" count 40
        40 weight "obj.coins" count 120
        10 weight "obj.coins" count 200
        1 weight "obj.coins" count 450
        3 weight "obj.water_skin4" count 1

        1 weight SharedDropTables.herb
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 90 weight "obj.arceuus_corpse_kalphite" count 1
        onBuilder { brimstoneKeyRoll() }
    },
)
