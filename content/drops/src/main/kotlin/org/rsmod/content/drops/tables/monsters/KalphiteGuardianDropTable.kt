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
public val kalphiteGuardianDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kalphite Guardian Drops",
    npcs = npcs("npc.kalphite_lord", "npc.kalphite_lord_chamber"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Kalphite Guardian Drops")
        4 weight "obj.mithril_sword" count 1
        3 weight "obj.steel_battleaxe" count 1
        2 weight "obj.mithril_axe" count 1
        2 weight "obj.adamant_dagger" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_chainbody" count 1
        8 weight "obj.airrune" count 50
        7 weight "obj.chaosrune" count 10
        4 weight "obj.bloodrune" count 7
        1 weight "obj.firerune" count 37
        1 weight "obj.lawrune" count 3
        40 weight "obj.coins" count 132
        7 weight "obj.coins" count 30
        6 weight "obj.coins" count 44
        6 weight "obj.coins" count 220
        1 weight "obj.coins" count 460
        3 weight "obj.lobster" count 1
        1 weight "obj.3dose1defense" count 1

        23 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 35 weight "obj.arceuus_corpse_kalphite" count 1
        onBuilder { brimstoneKeyRoll() }
    },
)
