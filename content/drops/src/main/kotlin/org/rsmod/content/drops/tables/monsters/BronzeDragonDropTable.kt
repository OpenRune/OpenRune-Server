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
public val bronzeDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bronze dragon Drops",
    npcs = npcs("npc.bronze_dragon", "npc.bronze_dragon_strongholdcave", "npc.kourend_bronze_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.bronze_bar" count 5 condition { player ->
            // Drops Need Manual: Bronze bars are dropped in bank note form if players have completed the elite Karamja Diary and are killing bronze dragons in the Brimhaven Dungeon.
             true
        }
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 2048 weight "obj.dragon_plateskirt" count 1
        1 outOf 2048 weight "obj.dragon_platelegs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bronze dragon Drops")
        7 weight "obj.adamant_dart_p" count 16
        4 weight "obj.mithril_2h_sword" count 1
        3 weight "obj.mithril_axe" count 1
        3 weight "obj.mithril_battleaxe" count 1
        3 weight "obj.rune_knife" count 2
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_longsword" count 1
        20 weight "obj.adamant_javelin" count 30
        8 weight "obj.firerune" count 50
        6 weight "obj.xbows_crossbow_bolts_mithril" count 2..12
        5 weight "obj.lawrune" count 10
        3 weight "obj.bloodrune" count 15
        1 weight "obj.deathrune" count 25
        40 weight "obj.coins" count 196
        10 weight "obj.coins" count 330
        1 weight "obj.coins" count 690
        3 weight "obj.adamantite_bar" count 1
        2 weight "obj.swordfish" count 2
        1 weight "obj.swordfish" count 1

        1 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
