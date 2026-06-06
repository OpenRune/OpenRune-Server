package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ironDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Iron dragon Drops",
    npcs = npcs("npc.ds2_iron_dragon", "npc.iron_dragon", "npc.iron_dragon_strongholdcave", "npc.kourend_iron_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.iron_bar" count 5 condition { player ->
            // Drops Need Manual: Iron bars are dropped in bank note form if players have completed the Elite Karamja Diary and are killing iron dragons in the Brimhaven Dungeon.
             true
        }
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 1024 weight "obj.dragon_plateskirt" count 1
        1 outOf 1024 weight "obj.dragon_platelegs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Iron dragon Drops")
        7 weight "obj.rune_dart_p" count 9
        4 weight "obj.adamant_2h_sword" count 1
        3 weight "obj.adamant_axe" count 1
        3 weight "obj.adamant_battleaxe" count 1
        3 weight "obj.rune_knife" count 5
        1 weight "obj.adamant_sq_shield" count 1
        1 weight "obj.rune_med_helm" count 1
        1 weight "obj.rune_battleaxe" count 1
        20 weight "obj.rune_javelin" count 4
        19 weight "obj.bloodrune" count 15
        6 weight "obj.xbows_crossbow_bolts_adamantite" count 2..12
        5 weight "obj.soulrune" count 3
        20 weight "obj.coins" count 270
        10 weight "obj.coins" count 550
        1 weight "obj.coins" count 990
        8 weight "obj.1dose2strength" count 1
        5 weight "obj.xbows_crossbow_limbs_runite" count 1
        3 weight "obj.adamantite_bar" count 2
        3 weight "obj.curry" count 1

        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
