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
public val iceTrollGruntDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice troll grunt Drops",
    npcs = npcs("npc.fris_troll_bodyguard", "npc.fris_troll_bodyguard_pc"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ice troll grunt Drops")
        10 weight "obj.adamant_full_helm" count 1
        10 weight "obj.steel_platebody" count 1
        5 weight "obj.mithril_warhammer" count 1
        5 weight "obj.adamant_axe" count 1
        2 weight "obj.rune_kiteshield" count 1
        1 weight "obj.granite_shield" count 1
        1 weight "obj.rune_warhammer" count 1
        10 weight "obj.earthrune" count 8..14
        10 weight "obj.earthrune" count 12..36
        5 weight "obj.naturerune" count 4..12
        5 weight "obj.lawrune" count 4..8
        20 weight "obj.coins" count 200
        10 weight "obj.cert_raw_shark" count 2..8
        10 weight "obj.cert_seaweed" count 3..9
        10 weight "obj.cert_ball_of_wool" count 18..42

        2 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 14 weight "obj.cert_arctic_pine_log" count 2..4
        1 outOf 20 weight "obj.arceuus_corpse_troll" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
