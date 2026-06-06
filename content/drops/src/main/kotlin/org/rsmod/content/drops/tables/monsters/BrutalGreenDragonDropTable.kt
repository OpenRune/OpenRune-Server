package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brutalGreenDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brutal green dragon Drops",
    npcs = npcs("npc.brut_green_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_green" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 138) {
        name("Brutal green dragon Drops")
        5 weight "obj.adamant_dart_p" count 25
        4 weight "obj.adamant_2h_sword" count 1
        3 weight "obj.brut_mithril_spear" count 1
        3 weight "obj.adamant_knife" count 8
        3 weight "obj.adamant_med_helm" count 1
        3 weight "obj.rune_thrownaxe" count 8
        2 weight "obj.adamant_spear" count 1
        1 weight "obj.adamant_chainbody" count 1
        1 weight "obj.adamant_kiteshield" count 1
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.rune_chainbody" count 1
        29 weight "obj.bloodrune" count 20
        8 weight "obj.lavarune" count 35
        6 weight "obj.steamrune" count 37
        5 weight "obj.naturerune" count 17
        3 weight "obj.lawrune" count 15
        3 weight "obj.adamant_arrow" count 8
        10 weight "obj.dragon_javelin_head" count 12 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        3 weight "obj.cert_mithril_ore" count 5
        11 weight "obj.coins" count 242
        10 weight "obj.coins" count 621 condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        2 weight "obj.curry" count 1..2

        15 weight SharedDropTables.herb
        3 weight SharedDropTables.rareDrop
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls and reward caskets are only dropped when completing an elite clue scroll asking you to kill a green dragon.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        1 outOf 28 weight "obj.arceuus_corpse_dragon" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
