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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val turothDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Turoth Drops",
    npcs = npcs("npc.slayer_turoth_baby", "npc.slayer_turoth_child", "npc.slayer_turoth_dad", "npc.slayer_turoth_mum"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Turoth Drops")
        7 weight "obj.steel_platelegs" count 1
        3 weight "obj.mithril_axe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.rune_dagger" count 1
        6 weight "obj.lawrune" count 3
        5 weight "obj.naturerune" count 15
        1 weight "obj.naturerune" count 37
        7 weight "obj.limpwurt_root" count 1
        29 weight "obj.coins" count 44
        12 weight "obj.coins" count 132
        1 weight "obj.coins" count 440
        1 outOf 500 separate "obj.leafbladed_sword" count 1
        1 outOf 512 separate "obj.mystic_robe_bottom_light" count 1

        31 weight rsWeightedTable(total = 31) {
            15 weight herbDropTable
            10 weight doubleRollHerbDropTable
            6 weight tripleRollHerbDropTable
        }
        5 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): Brimstone key rarities for levels 83, 85, 87, and 89 are 1/125, 1/116, 1/106, and 1/99 respectively.
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
