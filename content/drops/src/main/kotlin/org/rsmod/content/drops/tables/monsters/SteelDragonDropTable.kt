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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val steelDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Steel dragon Drops",
    npcs = npcs("npc.kourend_steel_dragon", "npc.steel_dragon", "npc.steel_dragon_strongholdcave"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.steel_bar" count 5 condition { player ->
            // Drops Need Manual: Steel bars are dropped in bank note form if players have completed the elite Karamja Diary and are killing steel dragons in the Brimhaven Dungeon.
             true
        }
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 512 weight "obj.dragon_plateskirt" count 1
        1 outOf 512 weight "obj.dragon_platelegs" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 133) {
        name("Steel dragon Drops")
        7 weight "obj.rune_dart_p" count 12
        4 weight "obj.rune_mace" count 1
        2 weight "obj.adamant_kiteshield" count 1
        3 weight "obj.rune_knife" count 7
        2 weight "obj.rune_axe" count 1
        1 weight "obj.rune_full_helm" count 1
        19 weight "obj.bloodrune" count 20
        20 weight "obj.rune_javelin" count 7
        6 weight "obj.xbows_crossbow_bolts_runite" count 2..12
        5 weight "obj.soulrune" count 5
        17 weight "obj.coins" count 470
        5 weight "obj.coins" count 650 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        13 weight "obj.3dose2attack" count 1
        8 weight "obj.xbows_crossbow_limbs_runite" count 1
        5 weight "obj.dragon_javelin_head" count 12 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        3 weight "obj.runite_bar" count 1
        3 weight "obj.2dose2defense" count 1
        1 weight "obj.curry" count 1
        1 weight "obj.curry" count 2

        4 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        1 outOf 60 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 475 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
