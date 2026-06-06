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
public val sarachnisDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sarachnis Drops",
    npcs = npcs("npc.sarachnis"),
    preRoll = rsPlayerPrerollTable {
        2 outOf 384 weight "obj.dragon_med_helm" count 1
        1 outOf 384 weight "obj.sarachnis_cudgel" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Sarachnis Drops")
        2 weight "obj.cert_battlestaff" count 8..10
        2 weight "obj.rune_platebody" count 1
        2 weight "obj.rune_med_helm" count 1
        2 weight "obj.rune_2h_sword" count 1
        5 weight "obj.bloodrune" count 80..100
        5 weight "obj.chaosrune" count 175..200
        5 weight "obj.cosmicrune" count 125..150
        5 weight "obj.deathrune" count 80..100
        5 weight "obj.soulrune" count 80..100
        2 weight "obj.mithril_arrow" count 450..600
        2 weight "obj.xbows_crossbow_bolts_mithril" count 175..225
        6 weight "obj.cert_mithril_ore" count 60..90
        5 weight "obj.cert_dragonhide_red" count 15..25
        4 weight "obj.cert_uncut_sapphire" count 20..30
        3 weight "obj.cert_adamantite_ore" count 30..40
        3 weight "obj.cert_uncut_emerald" count 20..30
        2 weight "obj.xbows_bolt_tips_onyx" count 8..10
        2 weight "obj.cert_uncut_ruby" count 20..30
        1 weight "obj.cert_runite_ore" count 4..6
        1 weight "obj.cert_uncut_diamond" count 20..30
        6 weight "obj.coins" count 17000..25000
        5 weight "obj.cert_dragon_bones" count 10..15
        5 weight "obj.potato_egg+tomato" count 5..8
        2 weight "obj.cert_weapon_poison++" count 4..6
        1 weight "obj.crystal_key" count 1
        1 weight "obj.cert_village_spider_carcass" count 10
        15 outOf 800 separate "obj.cert_unidentified_kwuarm" count 10..15
        12 outOf 800 separate rsPlayerWeightedTable {
            12 weight "obj.cert_unidentified_dwarf_weed" count 10..15
            12 weight "obj.cert_unidentified_cadantine" count 10..15
        }
        10 outOf 800 separate "obj.cert_unidentified_avantoe" count 5..10
        9 outOf 800 separate "obj.cert_unidentified_lantadyme" count 10..15
        8 outOf 800 separate rsPlayerWeightedTable {
            8 weight "obj.cert_unidentified_snapdragon" count 5..10
            8 weight "obj.cert_unidentified_ranarr" count 5..10
        }
        6 outOf 800 separate "obj.cert_unidentified_torstol" count 5..10

        1 weight SharedDropTables.gem
        2 weight SharedDropTables.rareSeed
        13 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate to receive any page is 1/5. When rolled, the player will receive the page they have the fewest of.
        1 outOf 5 weight "obj.hosdun_moon_page" count 1
        1 outOf 5 weight "obj.hosdun_sun_page" count 1
        1 outOf 5 weight "obj.hosdun_temple_page" count 1
        1 outOf 20 weight "obj.hosdun_egg_sac_full" count 1
        1 outOf 15 weight "obj.hosdun_grubby_key" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 50 weight "obj.slayer_spider_silk" count 1
        1 outOf 2000 weight "obj.jar_of_eyes" count 1
        1 outOf 3000 weight "obj.sarachnispet" count 1
        1 outOf 38 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 57 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
