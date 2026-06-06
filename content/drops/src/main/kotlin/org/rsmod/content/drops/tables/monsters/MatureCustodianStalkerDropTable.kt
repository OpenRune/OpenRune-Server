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
public val matureCustodianStalkerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mature custodian stalker Drops",
    npcs = npcs("npc.mature_custodian_stalker"),
    mainTable = rsPlayerWeightedTable(total = 107) {
        name("Mature custodian stalker Drops")
        15 weight "obj.mcannonball" count 15..30
        12 weight "obj.airrune" count 150
        12 weight "obj.firerune" count 100
        10 weight "obj.rune_arrow" count 10..25
        6 weight "obj.deathrune" count 25..40
        10 weight "obj.cert_blankrune_high" count 20
        8 weight "obj.monkfish" count 2..3
        7 weight "obj.custodian_broken_antler" count 1
        5 weight "obj.cert_mithril_bar" count 5..10
        4 weight "obj.raw_beef" count 1
        1 weight "obj.huasca_seed" count 1
        10 weight "obj.coins" count 800..1050

        1 weight SharedDropTables.rareDrop
        6 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 800 weight "obj.custodian_antler_guard" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
