package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val iceTrollDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice troll Drops",
    npcs = npcs("npc.trollrescue_icetroll_melee1", "npc.trollrescue_icetroll_melee2", "npc.trollrescue_icetroll_melee3", "npc.trollrescue_icetroll_melee4", "npc.trollrescue_icetroll_melee5", "npc.trollrescue_icetroll_melee6", "npc.trollrescue_icetroll_melee7", "npc.trollromance_icetroll_melee1", "npc.trollromance_icetroll_melee2", "npc.trollromance_icetroll_melee3", "npc.trollromance_icetroll_melee4", "npc.trollromance_icetroll_melee5", "npc.trollromance_icetroll_melee6", "npc.trollromance_icetroll_melee7"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ice troll Drops")
        4 weight "obj.black_platebody" count 1
        2 weight "obj.adamant_axe" count 1
        1 weight "obj.adamant_kiteshield" count 1
        1 weight "obj.black_warhammer" count 1
        1 weight "obj.mithril_platebody" count 1
        1 weight "obj.rune_battleaxe" count 1
        8 weight "obj.bloodrune" count 2
        5 weight "obj.bloodrune" count 5
        3 weight "obj.lawrune" count 4
        1 weight "obj.deathrune" count 15
        1 weight "obj.waterrune" count 65
        29 weight "obj.coins" count 30
        25 weight "obj.coins" count 136
        10 weight "obj.coins" count 200
        4 weight "obj.coins" count 20
        3 weight "obj.cert_lobster" count 6
        2 weight "obj.raw_tuna" count 4
        1 weight ringNothing()

        18 weight SharedDropTables.herb
        8 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_troll" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
