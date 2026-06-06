package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val elderCustodianStalkerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elder custodian stalker Drops",
    npcs = npcs("npc.elder_custodian_stalker"),
    mainTable = rsPlayerWeightedTable(total = 124) {
        name("Elder custodian stalker Drops")
        15 weight "obj.mcannonball" count 20..30
        12 weight "obj.airrune" count 200
        12 weight "obj.firerune" count 150
        12 weight "obj.rune_arrow" count 20..35
        6 weight "obj.deathrune" count 40..50
        12 weight "obj.shark" count 2..3
        12 weight "obj.cert_blankrune_high" count 30
        6 weight "obj.cert_mithril_bar" count 10..15
        4 weight "obj.custodian_broken_antler" count 1
        4 weight "obj.raw_beef" count 1
        1 weight "obj.huasca_seed" count 2..3
        19 weight "obj.coins" count 1180..1300
        2 weight "obj.alchemist_ring" count 1

        1 weight SharedDropTables.rareDrop
        6 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 650 weight "obj.custodian_antler_guard" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
