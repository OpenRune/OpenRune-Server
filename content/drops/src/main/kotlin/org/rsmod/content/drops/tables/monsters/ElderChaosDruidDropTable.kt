package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import dtx.rs.rsWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.content.drops.tables.shared.doubleRollHerbDropTable
import org.rsmod.content.drops.tables.shared.herbDropTable
import org.rsmod.content.drops.tables.shared.tripleRollHerbDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val elderChaosDruidDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elder Chaos druid Drops",
    npcs = npcs("npc.wilderness_chaos_druid"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Elder Chaos druid Drops")
        6 weight "obj.xbows_crossbow_bolts_mithril" count (8..28) condition {
            player -> !player.hasCompletedQuest("quest_observatoryquest")
        }
        7 weight "obj.lawrune" count 6
        5 weight "obj.airrune" count 56
        5 weight "obj.bodyrune" count 19
        5 weight "obj.chaosrune" count 7
        5 weight "obj.earthrune" count 19
        5 weight "obj.mindrune" count 22
        1 weight "obj.naturerune" count 12
        7 weight "obj.coins" count 80
        6 weight "obj.coins" count 250
        10 weight "obj.vial_water" count 4
        5 weight "obj.steel_longsword" count 1
        2 weight "obj.wilderness_fishing_bait" count 10..24
        1 weight "obj.snape_grass" count 4
        1 weight "obj.unholy_symbol_mould" count 1 condition {
            player -> player.hasCompletedQuest("quest_theobservatoryquest")
        }
        4 outOf 1419 separate rsPlayerWeightedTable {
            4 weight "obj.zamrobetop" count 1
            4 weight "obj.zamrobebottom" count 1
        }
        1 outOf 1419 separate rsPlayerWeightedTable {
            1 weight "obj.elderchaos_hood" count 1
            1 weight "obj.elderchaos_bottom" count 1
            1 weight "obj.elderchaos_top" count 1
        }

        55 weight rsWeightedTable(total = 50) {
            15 weight herbDropTable
            20 weight doubleRollHerbDropTable
            15 weight tripleRollHerbDropTable
        }
        1 weight SharedDropTables.rareDrop
        1 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_chaosdruid" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 64 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
