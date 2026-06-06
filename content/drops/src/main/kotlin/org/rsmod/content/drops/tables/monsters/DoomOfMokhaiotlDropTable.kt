package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val doomOfMokhaiotlDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Doom of Mokhaiotl Drops",
    npcs = npcs("npc.dom_boss"),
    mainTable = rsPlayerWeightedTable(total = 104) {
        name("Doom of Mokhaiotl Drops")
        5 weight "obj.cert_dragon_med_helm" count 1
        1 weight "obj.cert_dragon_platelegs" count 2..4
        5 weight "obj.cert_mystic_earth_staff" count 1
        5 weight "obj.cert_rune_pickaxe" count 1..3
        5 weight "obj.deathrune" count 50..70
        5 weight "obj.chaosrune" count 50..70
        5 weight "obj.earthrune" count 500..1000
        5 weight "obj.firerune" count 500..1000
        5 weight "obj.mcannonball" count 200..600
        5 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx" count 5..15
        5 weight "obj.cert_coal" count 15..50
        5 weight "obj.cert_gold_ore" count 20..60
        5 weight "obj.cert_runite_ore" count 3..6
        3 weight "obj.celastrus_tree_seed" count 1
        2 weight "obj.ranarr_seed" count 1..3
        3 weight "obj.spirit_tree_seed" count 1
        5 weight "obj.cosmic_soul_catalyst" count 151..400
        5 weight "obj.dragon_dart_tip" count 30..90
        5 weight "obj.sun_kissed_bone" count 25..75
        1 weight "obj.varlamore_key_half_1" count 1
        7 weight "obj.demon_tear" count 100..300
        7 weight "obj.dom_teleport_item" count 1..2
        1 outOf 1350 separate "obj.avernic_treads" count 1 condition {
            player -> player.hasCompletedQuest("quest_delvelevel4anddeeper")
        }
        1 outOf 2000 separate "obj.eye_of_ayak_uncharged" count 1 condition {
            player -> player.hasCompletedQuest("quest_delvelevel3anddeeper")
        }
        1 outOf 2500 separate "obj.mokhaiotl_cloth" count 1 condition {
            player -> player.hasCompletedQuest("quest_delvelevel2anddeeper")
        }
        10 outOf 312 separate "obj.cert_raw_shark" count 20..35
        5 outOf 312 separate "obj.shark_lure" count 40..70
        1 outOf 75 separate "obj.trail_elite_emote_exp1" count 1
        1 outOf 1000 separate "obj.dompet" count 1
        5 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.demon_tear" count 50
    },
)
