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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val iceWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice warrior Drops",
    npcs = npcs("npc.ice_warrior_tapoyauik", "npc.icewarrior", "npc.icewarrior_low_wanderrange", "npc.icewarrior_queen"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ice warrior Drops")
        3 weight "obj.iron_battleaxe" count 1
        1 weight "obj.mithril_mace" count 1
        10 weight "obj.naturerune" count 4
        8 weight "obj.chaosrune" count 3
        7 weight "obj.lawrune" count 2
        5 weight "obj.cosmicrune" count 2
        5 weight "obj.mithril_arrow" count 3
        3 weight "obj.adamant_arrow" count 2
        3 weight "obj.deathrune" count 2
        1 weight "obj.bloodrune" count 2
        39 weight "obj.coins" count 15
        13 weight ringNothing()

        10 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        17 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 994 weight "obj.varlamore_key_half_1" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Frozen tear [tertiary/<ref name=row group=d>Only dropped in Ruins of Tapoyauik</ref>]
