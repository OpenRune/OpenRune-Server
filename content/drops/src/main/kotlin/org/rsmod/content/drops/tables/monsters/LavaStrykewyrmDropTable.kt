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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lavaStrykewyrmDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lava Strykewyrm Drops",
    npcs = npcs("npc.slayer_lava_strykewyrm"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 115 weight "obj.dragon_sheet" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Lava Strykewyrm Drops")
        5 weight "obj.lava_battlestaff" count 1
        4 weight "obj.adamant_platebody" count 1
        2 weight "obj.rune_longsword" count 1
        2 weight "obj.rune_mace" count 1
        2 weight "obj.rune_med_helm" count 1
        2 weight "obj.rune_pickaxe" count 1
        6 weight "obj.lavarune" count 50..100
        6 weight "obj.deathrune" count 5..10
        6 weight "obj.chaosrune" count 10..20
        6 weight "obj.firerune" count 50..100
        5 weight "obj.lavarune" count 200
        5 weight "obj.rune_dart" count 20..30
        5 weight "obj.rune_knife" count 20..30
        5 weight "obj.rune_arrow" count 20..30
        3 weight "obj.adamant_cannonball" count 12..22
        1 weight "obj.rune_cannonball" count 9..14
        6 weight "obj.coins" count 100..200
        5 weight "obj.potato_chilli+carne" count 1
        5 weight "obj.fire_orb" count 1
        5 weight "obj.uncut_diamond" count 1
        4 weight "obj.cert_ashes" count 2..3
        4 weight "obj.3dose2attack" count 1
        4 weight "obj.3dose2defense" count 1
        4 weight "obj.dragon_arrowheads" count 2..6
        4 weight "obj.cert_coal" count 4..6
        4 weight "obj.cert_adamantite_ore" count 2..4
        4 weight "obj.chaos_talisman" count 1
        4 weight "obj.fire_talisman" count 1
        3 weight "obj.cert_runite_ore" count 1..2

        2 weight SharedDropTables.herb
        5 weight nothing()
        5 outOf 471 separate "obj.xbows_bolt_tips_diamond" count 5..10
        5 outOf 589 separate "obj.xbows_bolt_tips_emerald" count 5..10
        5 outOf 589 separate "obj.xbows_bolt_tips_ruby" count 5..10
        1 outOf 157 separate "obj.xbows_bolt_tips_dragonstone" count 5..10
        2 outOf 673 separate "obj.xbows_bolt_tips_onyx" count 5..10
        1 outOf 471 separate "obj.xbows_bolt_tips_sapphire" count 5..10
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
