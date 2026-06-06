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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val vorkathDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Vorkath Drops",
    npcs = npcs("npc.vorkath", "npc.vorkath_quest", "npc.vorkath_sleeping", "npc.vorkath_sleeping_noop"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_blue" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 150) {
        name("Vorkath Drops")
        5 weight "obj.rune_longsword" count 2..3
        5 weight "obj.rune_kiteshield" count 2..3
        4 weight "obj.cert_battlestaff" count 5..15
        2 weight "obj.dragon_battleaxe" count 1
        2 weight "obj.dragon_longsword" count 1
        2 weight "obj.dragon_platelegs" count 1
        2 weight "obj.dragon_plateskirt" count 1
        6 weight "obj.chaosrune" count 250..350
        6 weight "obj.deathrune" count 200..300
        3 weight "obj.wrathrune" count 30..60
        8 weight "obj.cert_dragonhide_blue" count 25..30
        7 weight "obj.cert_dragonhide_green" count 25..30
        7 weight "obj.cert_dragonhide_red" count 20..25
        7 weight "obj.cert_dragonhide_black" count 15..25
        8 weight "obj.dragon_bolts_unfeathered" count 50..100
        5 weight "obj.xbows_bolt_tips_dragonstone" count 11..25
        4 weight "obj.xbows_bolt_tips_onyx" count 5..10
        3 weight "obj.rune_dart_tip" count 75..100
        6 weight "obj.dragon_dart_tip" count 10..50
        3 weight "obj.dragon_arrowheads" count 25..50
        7 weight "obj.cert_adamantite_ore" count 10..30
        5 weight "obj.coins" count 20000..80000
        5 weight "obj.cert_grapes" count 250..300
        5 weight "obj.cert_magic_logs" count 50
        4 weight "obj.cert_dragon_bones" count 15..20
        4 weight "obj.cert_diamond" count 10..20
        3 weight "obj.cert_dragonstone" count 2..3
        3 weight "obj.wrath_talisman" count 1
        25 outOf 2730 separate "obj.xbows_bolt_tips_diamond" count 25..30
        20 outOf 2730 separate rsPlayerWeightedTable {
            20 weight "obj.xbows_bolt_tips_emerald" count 25..30
            20 weight "obj.xbows_bolt_tips_ruby" count 25..30
        }
        14 outOf 2730 separate "obj.xbows_bolt_tips_dragonstone" count 25..30
        7 outOf 2730 separate "obj.xbows_bolt_tips_onyx" count 25..30
        5 outOf 2730 separate "obj.xbows_bolt_tips_sapphire" count 25..30
        3 outOf 300 separate rsPlayerWeightedTable {
            3 weight "obj.cert_raw_shark" count 35..55
            3 weight "obj.shark_lure" count 70..110
        }
        2 outOf 300 separate "obj.cert_mantaray" count 35..55

        5 weight SharedDropTables.rareDrop
        14 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.scaly_bluehide" count 1
        1 outOf 50 weight "obj.vorkath_head" count 1 condition { player ->
            // Drops Need Manual: }}}}
             true
        }
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1000 weight "obj.dragonbone_necklace" count 1
        1 outOf 3000 weight "obj.jar_of_decay" count 1
        1 outOf 3000 weight "obj.vorkathpet" count 1
        1 outOf 5000 weight "obj.dragonfire_visage" count 1
        1 outOf 5000 weight "obj.skeletal_visage" count 1
        1 outOf 61 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
