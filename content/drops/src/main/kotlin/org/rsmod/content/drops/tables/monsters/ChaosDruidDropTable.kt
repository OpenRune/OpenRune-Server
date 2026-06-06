package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val chaosDruidDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chaos druid Drops",
    npcs = npcs("npc.chaos_druid"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Chaos druid Drops")
        7 weight "obj.lawrune" count 2
        4 weight "obj.xbows_crossbow_bolts_mithril" count (2..12) condition {
            player -> !player.hasCompletedQuest("quest_observatoryquest")
        }
        3 weight "obj.airrune" count 36
        2 weight "obj.bodyrune" count 9
        2 weight "obj.earthrune" count 9
        2 weight "obj.mindrune" count 12
        1 weight "obj.naturerune" count 3
        5 weight "obj.coins" count 3
        5 weight "obj.coins" count 8
        3 weight "obj.coins" count 29
        1 weight "obj.coins" count 35
        33 weight ringNothing()
        10 weight "obj.vial_water" count 1
        1 weight "obj.bronze_longsword" count 1
        1 weight "obj.snape_grass" count 1
        1 weight "obj.unholy_symbol_mould" count 1 condition {
            player -> player.hasCompletedQuest("quest_observatoryquest")
        }

        46 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 11 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 35 weight "obj.arceuus_corpse_chaosdruid" count 1
    },
)
