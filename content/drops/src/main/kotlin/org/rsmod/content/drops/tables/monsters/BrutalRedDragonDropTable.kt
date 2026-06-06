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
public val brutalRedDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brutal red dragon Drops",
    npcs = npcs("npc.ds2_brut_red_dragon", "npc.kourend_red_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_red" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 139) {
        name("Brutal red dragon Drops")
        10 weight "obj.brut_rune_spear" count 1 condition { player ->
            // Drops Need Manual: Players who have not completed Barbarian training will receive the spear instead of the hasta.
             true
        }
        10 weight "obj.rune_spear" count 1
        7 weight "obj.adamant_platelegs" count 1
        5 weight "obj.adamant_full_helm" count 1
        5 weight "obj.rune_longsword" count 1
        2 weight "obj.red_dragonhide_body" count 1
        2 weight "obj.rune_full_helm" count 2
        1 weight "obj.red_dragon_vambraces" count 1
        1 weight "obj.dragon_dagger" count 1
        1 weight "obj.dragon_longsword" count 1
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.rune_platebody" count 1
        8 weight "obj.deathrune" count 25
        8 weight "obj.rune_javelin" count 30
        7 weight "obj.airrune" count 105
        7 weight "obj.bloodrune" count 12
        7 weight "obj.lawrune" count 25
        7 weight "obj.rune_arrow" count 25
        5 weight "obj.adamant_dart" count 20
        2 weight "obj.rune_knife" count 10
        2 weight "obj.rune_thrownaxe" count 15
        4 weight "obj.cert_white_berries" count 5
        3 weight "obj.dragon_dart_tip" count 8
        2 weight "obj.dragon_arrowheads" count 8
        2 weight "obj.cert_runite_ore" count 2
        2 weight "obj.dragon_javelin_head" count 25 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        12 weight "obj.coins" count 670
        2 weight "obj.coins" count 621 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        8 weight "obj.curry" count 3

        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_dragon" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 475 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
