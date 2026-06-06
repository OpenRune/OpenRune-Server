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
public val juvenileCustodianStalkerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Juvenile custodian stalker Drops",
    npcs = npcs("npc.juvenile_custodian_stalker"),
    mainTable = rsPlayerWeightedTable(total = 160) {
        name("Juvenile custodian stalker Drops")
        20 weight "obj.mcannonball" count 10..15
        20 weight "obj.airrune" count 100
        20 weight "obj.firerune" count 50
        12 weight "obj.rune_arrow" count 5..20
        10 weight "obj.deathrune" count 10..26
        12 weight "obj.cert_blankrune_high" count 15
        8 weight "obj.swordfish" count 2..3
        4 weight "obj.raw_beef" count 1
        20 weight "obj.coins" count 400..750
        16 weight "obj.adamant_scimitar" count 1
        1 outOf 15 separate "obj.custodian_broken_antler" count 1

        1 weight SharedDropTables.rareDrop
        6 weight SharedDropTables.gem
        11 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
