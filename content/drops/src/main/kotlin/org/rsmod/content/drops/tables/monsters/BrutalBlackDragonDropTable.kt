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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brutalBlackDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brutal black dragon Drops",
    npcs = npcs("npc.ds2_brut_black_dragon", "npc.ds2_brut_black_dragon_cutscene", "npc.kourend_black_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_black" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 139) {
        name("Brutal black dragon Drops")
        10 weight "obj.brut_rune_spear" count 1 condition { player ->
            // Drops Need Manual: Players who have not completed Barbarian training will receive the spear instead of the hasta.
             true
        }
        10 weight "obj.rune_spear" count 1
        7 weight "obj.rune_platelegs" count 1
        6 weight "obj.rune_full_helm" count 2
        5 weight "obj.rune_dart" count 20
        5 weight "obj.rune_longsword" count 1
        2 weight "obj.black_dragonhide_body" count 1
        2 weight "obj.rune_knife" count 25
        2 weight "obj.rune_thrownaxe" count 30
        1 weight "obj.black_dragon_vambraces" count 1
        1 weight "obj.rune_platebody" count 1
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.dragon_longsword" count 1
        1 weight "obj.dragon_dagger" count 1
        8 weight "obj.rune_javelin" count 50
        7 weight "obj.deathrune" count 75
        8 weight "obj.bloodrune" count 50
        8 weight "obj.soulrune" count 50
        7 weight "obj.lawrune" count 75
        7 weight "obj.rune_arrow" count 75
        4 weight "obj.cert_lava_scale" count 5
        3 weight "obj.dragon_dart_tip" count 40
        2 weight "obj.cert_runite_ore" count 3
        2 weight "obj.dragon_arrowheads" count 40
        2 weight "obj.dragon_javelin_head" count 40 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        11 weight "obj.coins" count 370
        1 weight "obj.coins" count 540..929
        2 weight "obj.coins" count 2200 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        8 weight "obj.anglerfish" count 2
        1 outOf 512 separate rsPlayerWeightedTable {
            1 weight "obj.dragon_platelegs" count 1
            1 weight "obj.dragon_plateskirt" count 1
            1 weight "obj.dragon_spear" count 1
            1 weight "obj.uncut_dragonstone" count 1
        }

        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 20 weight "obj.arceuus_corpse_dragon" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/237 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 250 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
