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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val throwerTrollDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Thrower Troll Drops",
    npcs = npcs("npc.death_troll_thrower1", "npc.death_troll_thrower2", "npc.death_troll_thrower3", "npc.death_troll_thrower4", "npc.death_troll_thrower5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Thrower Troll Drops")
        4 weight "obj.steel_med_helm" count 1
        3 weight "obj.black_warhammer" count 1
        3 weight "obj.steel_warhammer" count 1
        1 weight "obj.adamant_med_helm" count 1
        1 weight "obj.adamnt_warhammer" count 1
        1 weight "obj.mithril_sq_shield" count 1
        8 weight "obj.earthrune" count 60
        5 weight "obj.naturerune" count 7
        3 weight "obj.lawrune" count 2
        1 weight "obj.earthrune" count 45
        1 weight "obj.earthrune" count 25
        29 weight "obj.coins" count 35
        10 weight "obj.coins" count 100
        7 weight "obj.coins" count 8
        6 weight "obj.coins" count 50
        1 weight "obj.coins" count 250
        3 weight "obj.cert_coal" count 3
        2 weight "obj.cert_raw_mackerel" count 3

        15 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
        19 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_troll_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 45 weight "obj.arceuus_corpse_troll" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
