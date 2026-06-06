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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val kalphiteWorkerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kalphite Worker Drops",
    npcs = npcs("npc.kalphite_worker", "npc.kalphite_worker_chamber", "npc.kalphite_worker_strongholdcave"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Kalphite Worker Drops")
        3 weight "obj.iron_sword" count 1
        1 weight "obj.iron_javelin" count 5
        3 weight "obj.steel_dagger" count 1
        1 weight "obj.steel_longsword" count 1
        2 weight "obj.hardleather_body" count 1
        2 weight "obj.bodyrune" count 6
        1 weight "obj.cosmicrune" count 2
        2 weight "obj.chaosrune" count 3
        2 weight "obj.firerune" count 7
        2 weight "obj.waterrune" count 2
        3 weight "obj.lawrune" count 2
        2 weight "obj.naturerune" count 4
        8 weight "obj.coins" count 1
        12 weight "obj.coins" count 5
        34 weight "obj.coins" count 15
        12 weight "obj.coins" count 28
        3 weight "obj.coins" count 42
        4 weight "obj.coins" count 62
        21 weight "obj.water_skin4" count 1

        7 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 250 weight "obj.arceuus_corpse_kalphite" count 1
        onBuilder { brimstoneKeyRoll() }
    },
)
