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
public val phantomMuspahDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Phantom Muspah Drops",
    npcs = npcs("npc.muspah", "npc.muspah_final", "npc.muspah_melee", "npc.muspah_soulsplit", "npc.muspah_teleport"),
    mainTable = rsPlayerWeightedTable(total = 235) {
        name("Phantom Muspah Drops")
        10 weight "obj.cert_rune_kiteshield" count 3
        5 weight "obj.dragon_plateskirt" count 1
        5 weight "obj.cert_rune_platelegs" count 3
        5 weight "obj.black_dragonhide_body" count 1
        4 weight "obj.dragon_platelegs" count 2
        1 weight "obj.rune_sword" count 1
        10 weight "obj.lawrune" count 146
        10 weight "obj.soulrune" count 380
        10 weight "obj.deathrune" count 428
        15 weight "obj.smokerune" count 314
        5 weight "obj.chaosrune" count 480
        5 weight "obj.firerune" count 1964
        5 weight "obj.mcannonball" count 670
        3 weight "obj.cert_unidentified_toadflax" count 40
        5 weight "obj.yew_seed" count 2
        5 weight "obj.torstol_seed" count 2
        5 weight "obj.palm_tree_seed" count 2
        5 weight "obj.ranarr_seed" count 3
        4 weight "obj.snapdragon_seed" count 5
        3 weight "obj.ranarr_seed" count 5
        2 weight "obj.spirit_tree_seed" count 1
        10 weight "obj.cert_adamantite_ore" count 22
        10 weight "obj.cert_gold_ore" count 180
        10 weight "obj.cert_plank_teak" count 22
        15 weight "obj.cert_molten_glass" count 89
        5 weight "obj.cert_blankrune_high" count 2314
        5 weight "obj.cert_coal" count 163
        3 weight "obj.cert_runite_ore" count 18
        2 weight "obj.cert_silver_ore" count 101
        15 weight "obj.cert_water_orb" count 21
        10 weight "obj.dragon_bolts_unfeathered" count 89
        3 weight "obj.cert_limpwurt_root" count 21
        4 weight "obj.cert_raw_shark" count 28
        4 weight "obj.shark_lure" count 56
        3 weight "obj.cert_mantaray" count 28
        60 outOf 100 separate "obj.ancient_essence" count 540..599
        23 outOf 100 separate "obj.ancient_essence" count 885..995
        10 outOf 100 separate "obj.ancient_essence" count 1970..2060
        4 outOf 100 separate "obj.frozen_cache" count 1
        2 outOf 100 separate "obj.ancient_icon" count 1
        1 outOf 100 separate "obj.venator_shard" count 1 condition { player ->
            // Drops Need Manual: When a Venator shard is received, no regular loot will be dropped alongside it.
             true
        }
        1 outOf 9 separate rsPlayerWeightedTable {
            1 weight "obj.shark" count 4..6
            1 weight "obj.summer_pie" count 4..6
            1 weight "obj.3doseancientbrew" count 1..2
            1 weight "obj.3doserangerspotion" count 1..3
            1 weight "obj.3dose2restore" count 2..3
            1 weight "obj.3doseprayerrestore" count 2..3
        }
        5 outOf 752 separate "obj.cert_unidentified_kwuarm" count 6
        4 outOf 752 separate rsPlayerWeightedTable {
            4 weight "obj.cert_unidentified_dwarf_weed" count 6
            4 weight "obj.cert_unidentified_cadantine" count 6
        }
        3 outOf 752 separate "obj.cert_unidentified_lantadyme" count 6

        5 weight SharedDropTables.rareDrop
        9 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1 weight "obj.muspah_pet_morph" count 1 condition { player ->
            // Drops Need Manual: Guaranteed reward for defeating the boss in under 3:00, provided that the player does not already have one in their possession, or has not already used one on the Muphin pet.
             true
        }
        1 outOf 2500 weight "obj.muspahpet" count 1
        1 outOf 28 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 42 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
