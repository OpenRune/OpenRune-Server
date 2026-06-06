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
public val vyrewatchSentinelDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vyrewatch Sentinel Drops",
    npcs = npcs("npc.vyrewatch_elite_1", "npc.vyrewatch_elite_2", "npc.vyrewatch_elite_3", "npc.vyrewatch_elite_4", "npc.vyrewatch_elite_5", "npc.vyrewatch_elite_6", "npc.vyrewatch_elite_7", "npc.vyrewatch_elite_8"),
    mainTable = rsPlayerWeightedTable(total = 93) {
        name("Vyrewatch Sentinel Drops")
        6 weight "obj.rune_dagger" count 1
        6 weight "obj.adamant_platelegs" count 1
        4 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_full_helm" count 1
        1 weight "obj.rune_kiteshield" count 1
        10 weight "obj.deathrune" count 6..10
        10 weight "obj.bloodrune" count 8..16
        10 weight "obj.naturerune" count 6..11
        4 weight "obj.rune_arrow" count 4..10
        2 weight "obj.rune_javelin" count 5..15
        4 weight "obj.cert_hollow_bark" count 4..8
        4 weight "obj.cert_coal" count 8
        2 weight "obj.runite_bar" count 1
        2 weight "obj.cert_yew_logs" count 6
        2 weight "obj.runite_ore" count 1
        21 weight "obj.coins" count 100..1000
        9 outOf 5208 separate rsPlayerWeightedTable {
            9 weight "obj.opal_bolttips" count 6..14
            9 weight "obj.pearl_bolttips" count 6..14
            9 weight "obj.xbows_bolt_tips_diamond" count 6..14
        }
        3 outOf 5208 separate rsPlayerWeightedTable {
            3 weight "obj.xbows_bolt_tips_jade" count 6..14
            3 weight "obj.xbows_bolt_tips_redtopaz" count 6..14
            3 weight "obj.xbows_bolt_tips_sapphire" count 6..14
            3 weight "obj.xbows_bolt_tips_onyx" count 6..14
        }
        5 outOf 5208 separate "obj.xbows_bolt_tips_emerald" count 6..14
        6 outOf 5208 separate rsPlayerWeightedTable {
            6 weight "obj.xbows_bolt_tips_ruby" count 6..14
            6 weight "obj.xbows_bolt_tips_dragonstone" count 6..14
        }

        1 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/95 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 100 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
