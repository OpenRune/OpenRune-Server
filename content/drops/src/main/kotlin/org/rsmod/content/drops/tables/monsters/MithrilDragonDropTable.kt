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
public val mithrilDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mithril dragon Drops",
    npcs = npcs("npc.brut_mithril_dragon", "npc.ds2_mithril_dragon", "npc.ds2_mithril_dragon_cutscene"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mithril_bar" count 3 condition { player ->
            // Drops Need Manual: Mithril bars are dropped in banknote form if players have purchased the ''Duly noted'' unlock for 200 slayer reward points and are on an active Slayer task of metal dragons.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 139) {
        name("Mithril dragon Drops")
        8 weight "obj.rune_battleaxe" count 1
        7 weight "obj.rune_dart_p" count 14
        4 weight "obj.rune_battleaxe" count 1
        3 weight "obj.rune_knife" count 8
        3 weight "obj.rune_mace" count 1
        2 weight "obj.rune_spear" count 1
        1 weight "obj.rune_full_helm" count 1
        19 weight "obj.bloodrune" count 27
        14 weight "obj.rune_javelin" count 8
        6 weight "obj.xbows_crossbow_bolts_runite" count 10..21
        5 weight "obj.soulrune" count 10
        3 weight "obj.rune_arrow" count 8
        6 weight "obj.shark" count 1
        4 weight "obj.shark" count 1
        2 weight "obj.brutal_2doseprayerrestore" count 1
        2 weight "obj.shark" count 6
        2 weight "obj.brutal_2dose2attack" count 1
        2 weight "obj.brutal_2dose2defense" count 1
        2 weight "obj.brutal_2dose2strength" count 1
        17 weight "obj.coins" count 600
        7 weight "obj.coins" count 876 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        7 weight "obj.dragon_javelin_head" count 15 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        3 weight "obj.brut_barbarian_bones" count 1
        3 weight "obj.runite_bar" count 2
        2 weight "obj.brut_document_0" count 1
        1 outOf 32768 separate "obj.brut_dragon_full_helm" count 1

        1 weight SharedDropTables.rareDrop
        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/332 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 350 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
