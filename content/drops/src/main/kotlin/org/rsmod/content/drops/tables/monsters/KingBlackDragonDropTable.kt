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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val kingBlackDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "King Black Dragon Drops",
    npcs = npcs("npc.king_dragon", "npc.twocats_kbd_cutscene"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_black" count 2
    },
    preRoll = rsPlayerPrerollTable {
        1 outOf 1000 weight "obj.dragon_pickaxe" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("King Black Dragon Drops")
        10 weight "obj.rune_longsword" count 1
        9 weight "obj.adamant_platebody" count 1
        3 weight "obj.adamant_kiteshield" count 1
        1 weight "obj.dragon_med_helm" count 1
        5 weight "obj.firerune" count 300 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        10 weight "obj.airrune" count 300
        10 weight "obj.iron_arrow" count 690
        10 weight "obj.xbows_crossbow_bolts_runite" count 10..20
        5 weight "obj.lawrune" count 30
        5 weight "obj.bloodrune" count 30
        10 weight "obj.cert_yew_logs" count 150
        5 weight "obj.adamantite_bar" count 3
        3 weight "obj.runite_bar" count 1
        2 weight "obj.cert_gold_ore" count 100
        7 weight "obj.amulet_of_power" count 1
        5 weight "obj.dragon_arrowheads" count 5..14
        5 weight "obj.dragon_dart_tip" count 5..14
        5 weight "obj.dragon_javelin_head" count 15 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        4 weight "obj.xbows_crossbow_limbs_runite" count 1
        4 weight "obj.shark" count 4

        8 weight SharedDropTables.rareDrop
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing an elite clue scroll asking you to kill the King Black Dragon. It will only be dropped if the player does not have a copy of the key in their bank or inventory, however the Drop trick can be used to obtain duplicates.
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 128 weight "obj.poh_trophydrop_kbd" count 1
        1 outOf 3000 weight "obj.kbdpet" count 1
        1 outOf 5000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/427 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 450 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
