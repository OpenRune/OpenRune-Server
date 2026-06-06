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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val stickDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Stick Drops",
    npcs = npcs("npc.death_troll_leuitanant"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Stick Drops")
        4 weight "obj.steel_platebody" count 1
        3 weight "obj.black_warhammer" count 1
        3 weight "obj.steel_warhammer" count 1
        2 weight "obj.adamant_axe" count 1
        1 weight "obj.adamant_sq_shield" count 1
        1 weight "obj.granite_shield" count 1
        1 weight "obj.mithril_platebody" count 1
        1 weight "obj.rune_warhammer" count 1
        8 weight "obj.earthrune" count 80
        5 weight "obj.naturerune" count 16
        3 weight "obj.lawrune" count 4
        1 weight "obj.earthrune" count 65
        1 weight "obj.earthrune" count 25
        29 weight "obj.coins" count 40
        25 weight "obj.coins" count 135
        10 weight "obj.coins" count 190
        4 weight "obj.coins" count 20
        1 weight "obj.coins" count 420
        3 weight "obj.cert_coal" count 6
        2 weight "obj.cert_raw_tuna" count 4

        15 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_troll_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 28 weight "obj.arceuus_corpse_troll" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
