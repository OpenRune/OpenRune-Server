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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vyrewatchDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vyrewatch Drops",
    npcs = npcs("npc.darkm_vyrewatch_female_1", "npc.darkm_vyrewatch_female_2", "npc.darkm_vyrewatch_female_3", "npc.darkm_vyrewatch_female_4", "npc.darkm_vyrewatch_male_1", "npc.darkm_vyrewatch_male_2", "npc.darkm_vyrewatch_male_3", "npc.darkm_vyrewatch_male_4", "npc.myq4_vyrewatch_1", "npc.myq4_vyrewatch_1_crowd", "npc.myq4_vyrewatch_2", "npc.myq4_vyrewatch_2_crowd", "npc.myq4_vyrewatch_3", "npc.myq4_vyrewatch_3_crowd", "npc.myq4_vyrewatch_4", "npc.myq4_vyrewatch_4_crowd", "npc.myreque_pt3_takeoff_vyrewatch_female_1", "npc.myreque_pt3_takeoff_vyrewatch_female_2", "npc.myreque_pt3_takeoff_vyrewatch_female_3", "npc.myreque_pt3_takeoff_vyrewatch_female_4", "npc.myreque_pt3_takeoff_vyrewatch_male_1", "npc.myreque_pt3_takeoff_vyrewatch_male_2", "npc.myreque_pt3_takeoff_vyrewatch_male_3", "npc.myreque_pt3_takeoff_vyrewatch_male_4", "npc.sang_myq3_female_flying_ns_vyrewatch_1", "npc.sang_myq3_female_flying_ns_vyrewatch_2", "npc.sang_myq3_female_flying_ns_vyrewatch_3", "npc.sang_myq3_female_flying_ns_vyrewatch_4", "npc.sang_myq3_female_flying_vyrewatch_1", "npc.sang_myq3_female_flying_vyrewatch_2", "npc.sang_myq3_female_flying_vyrewatch_3", "npc.sang_myq3_female_flying_vyrewatch_4", "npc.sang_myq3_female_walk_ns_vyrewatch_1", "npc.sang_myq3_female_walk_ns_vyrewatch_2", "npc.sang_myq3_female_walk_ns_vyrewatch_3", "npc.sang_myq3_female_walk_ns_vyrewatch_4", "npc.sang_myq3_female_walk_vyrewatch_1", "npc.sang_myq3_female_walk_vyrewatch_2", "npc.sang_myq3_female_walk_vyrewatch_3", "npc.sang_myq3_female_walk_vyrewatch_4", "npc.sang_myq3_male_flying_ns_vyrewatch_1", "npc.sang_myq3_male_flying_ns_vyrewatch_2", "npc.sang_myq3_male_flying_ns_vyrewatch_3", "npc.sang_myq3_male_flying_ns_vyrewatch_4", "npc.sang_myq3_male_flying_vyrewatch_1", "npc.sang_myq3_male_flying_vyrewatch_2", "npc.sang_myq3_male_flying_vyrewatch_3", "npc.sang_myq3_male_flying_vyrewatch_4", "npc.sang_myq3_male_walk_ns_vyrewatch_1", "npc.sang_myq3_male_walk_ns_vyrewatch_2", "npc.sang_myq3_male_walk_ns_vyrewatch_3", "npc.sang_myq3_male_walk_ns_vyrewatch_4", "npc.sang_myq3_male_walk_vyrewatch_1", "npc.sang_myq3_male_walk_vyrewatch_2", "npc.sang_myq3_male_walk_vyrewatch_3", "npc.sang_myq3_male_walk_vyrewatch_4", "npc.slepe_vyrewatch_female_1", "npc.slepe_vyrewatch_female_2", "npc.slepe_vyrewatch_female_3", "npc.slepe_vyrewatch_female_4", "npc.slepe_vyrewatch_male_1", "npc.slepe_vyrewatch_male_2", "npc.slepe_vyrewatch_male_3", "npc.slepe_vyrewatch_male_4"),
    mainTable = rsPlayerWeightedTable(total = 96) {
        name("Vyrewatch Drops")
        6 weight "obj.rune_dagger" count 1
        2 weight "obj.adamant_platelegs" count 1
        6 weight "obj.adamant_platebody" count 1
        2 weight "obj.rune_platelegs" count 1
        6 weight "obj.mithril_axe" count 1
        1 weight "obj.rune_full_helm" count 1
        10 weight "obj.deathrune" count 4..12
        10 weight "obj.bloodrune" count 4..12
        10 weight "obj.naturerune" count 6..14
        4 weight "obj.adamant_arrow" count 8..16
        2 weight "obj.rune_javelin" count 5..15
        2 weight "obj.adamantite_ore" count 1
        4 weight "obj.coal" count 6
        2 weight "obj.runite_bar" count 1
        2 weight "obj.mortmyremushroom" count 2
        2 weight "obj.yew_logs" count 4
        4 weight "obj.hollow_bark" count 4..8
        15 weight "obj.coins" count 100..1000
        9 outOf 5376 separate rsPlayerWeightedTable {
            9 weight "obj.opal_bolttips" count 4..10
            9 weight "obj.pearl_bolttips" count 4..10
            9 weight "obj.xbows_bolt_tips_diamond" count 4..10
        }
        3 outOf 5376 separate rsPlayerWeightedTable {
            3 weight "obj.xbows_bolt_tips_jade" count 4..10
            3 weight "obj.xbows_bolt_tips_redtopaz" count 4..10
            3 weight "obj.xbows_bolt_tips_sapphire" count 4..10
            3 weight "obj.xbows_bolt_tips_onyx" count 4..10
        }
        5 outOf 5376 separate "obj.xbows_bolt_tips_emerald" count 4..10
        6 outOf 5376 separate rsPlayerWeightedTable {
            6 weight "obj.xbows_bolt_tips_ruby" count 4..10
            6 weight "obj.xbows_bolt_tips_dragonstone" count 4..10
        }

        1 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        1 weight SharedDropTables.gem
        1 weight SharedDropTables.seed
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
