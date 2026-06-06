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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brutalBlueDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brutal blue dragon Drops",
    npcs = npcs("npc.brutal_blue_dragon_tapoyauik", "npc.kourend_blue_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_blue" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 140) {
        name("Brutal blue dragon Drops")
        10 weight "obj.brut_adamant_spear" count 1 condition { player ->
            // Drops Need Manual: Players who have not completed Barbarian training will receive the spear instead of the hasta.
             true
        }
        10 weight "obj.adamant_spear" count 1
        7 weight "obj.adamant_platelegs" count 1
        5 weight "obj.mithril_full_helm" count 1
        5 weight "obj.rune_longsword" count 1
        2 weight "obj.blue_dragonhide_body" count 1
        1 weight "obj.blue_dragon_vambraces" count 1
        1 weight "obj.dragon_dagger" count 1
        1 weight "obj.dragon_longsword" count 1
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.rune_platebody" count 1
        8 weight "obj.chaosrune" count 18
        8 weight "obj.deathrune" count 11
        8 weight "obj.rune_javelin" count 20
        7 weight "obj.airrune" count 50
        7 weight "obj.lawrune" count 15
        7 weight "obj.rune_arrow" count 15
        5 weight "obj.adamant_dart" count 10
        2 weight "obj.rune_knife" count 5
        2 weight "obj.rune_thrownaxe" count 10
        4 weight "obj.cert_blue_dragon_scale" count 5
        3 weight "obj.dragon_dart_tip" count 5
        2 weight "obj.dragon_arrowheads" count 5
        2 weight "obj.cert_runite_ore" count 1
        1 weight "obj.dragon_javelin_head" count 12 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        11 weight "obj.coins" count 370
        8 weight "obj.curry" count 2
        1 weight "obj.coins" count 621 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }

        5 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_dragon" count 1
        1 outOf 33 weight "obj.scaly_bluehide" count 1
        1 outOf 32 weight "obj.frozen_tear" count 14 condition { player ->
            // Drops Need Manual: Only dropped in Ruins of Tapoyauik
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 250 weight "obj.varlamore_key_half_1" count 1
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 712 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
